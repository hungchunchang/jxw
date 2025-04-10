package com.example.jxw.util;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.util.Size;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.jxw.repository.DataRepository;
import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecordHandler {
    private final Context context;
    private final DataRepository dataRepository;
    private final ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;

    private static final String TAG = "RecordHandler";

    public RecordHandler(Context context, DataRepository dataRepository) {
        Log.d(TAG, "Initializing RecordHandler");
        this.context = context;
        this.dataRepository = dataRepository;
        this.cameraExecutor = Executors.newSingleThreadExecutor();
    }

    public void startRecording(LifecycleOwner lifecycleOwner) {
        Log.d(TAG, "Starting recording process");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                Log.d(TAG, "Getting camera provider");
                cameraProvider = cameraProviderFuture.get();
                setupImageAnalysis(lifecycleOwner);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void setupImageAnalysis(LifecycleOwner lifecycleOwner) {
        Log.d(TAG, "Setting up image analysis");
        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(600, 1024))
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] compressedData = ImageUtils.compressImage(
                    buffer,
                    image.getWidth(),
                    image.getHeight()
            );
            String base64Image = Base64.encodeToString(compressedData, Base64.NO_WRAP);
            dataRepository.sendContinuousImage(base64Image);
            image.close();
        });

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        try {
            Log.d(TAG, "Binding use cases");
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalysis  // 確保這裡有加入 imageAnalysis
            );
            Log.d(TAG, "Use cases bound successfully");
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }


    public void destroy() {
        Log.d(TAG, "Destroying RecordHandler");
        if (cameraProvider != null) {
            Log.d(TAG, "Unbinding all camera uses");
            cameraProvider.unbindAll();
        }
        Log.d(TAG, "Shutting down camera executor");
        cameraExecutor.shutdown();
    }
}

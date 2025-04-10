package com.example.jxw.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.jxw.repository.DataRepository;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraHandler {

    private static final String TAG = "CameraHandler";

    private final Context context;
    private ImageCapture imageCapture;
    private Executor executor = Executors.newSingleThreadExecutor();
    private DataRepository dataRepository;

    public CameraHandler(Context context) {
        this.context = context;
        startCamera();
    }

    public void setDataRepository(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    private void startCamera() {
        // 獲取 CameraProvider
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 創建 Preview 用例，負責顯示相機畫面
                Preview preview = new Preview.Builder().build();

                // 創建 ImageCapture 用例，用來拍攝照片
                imageCapture = new ImageCapture.Builder()
                        .setTargetRotation(context.getResources().getConfiguration().orientation) // 確保圖像方向正確
                        .build();

                // 設定相機選擇器 (選擇前置鏡頭)
                CameraSelector cameraSelector;
                try {
                    // 優先使用前置鏡頭
                    cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                            .build();
                } catch (Exception e) {
                    // 如果前置鏡頭不可用，則切換到後置鏡頭
                    cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build();
                }

                // 綁定 CameraX 用例到生命週期
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, preview, imageCapture);

                Log.d(TAG, "Camera started successfully.");

            } catch (Exception e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    public void takePicture(String resultString) {
        // 創建保存圖片的目標檔案
        File file = new File(context.getExternalFilesDir(null), "photo.jpg");

        // 配置拍照輸出選項
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(file).build();

        // 開始拍照
        imageCapture.takePicture(outputOptions, executor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                // 讀取保存的圖片並轉換成 Bitmap
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (dataRepository != null) {
                    // 將拍攝的圖片傳遞到 DataRepository
                    dataRepository.handleCapturedImage(resultString, bitmap);
                }
                Log.d(TAG, "Image captured and saved successfully.");
            }

            @Override
            public void onError(ImageCaptureException error) {
                Log.e(TAG, "Image capture failed: " + error.getMessage(), error);
            }
        });
    }

    public void destroy() {
        // 獲取 CameraProvider
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                // 解除綁定所有的用例
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                // 關閉執行緒
                if (executor != null) {
                    ((ExecutorService) executor).shutdown();
                    executor = null;
                }

                Log.d(TAG, "Camera resources have been released.");
            } catch (Exception e) {
                Log.e(TAG, "Failed to release camera resources", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }
}

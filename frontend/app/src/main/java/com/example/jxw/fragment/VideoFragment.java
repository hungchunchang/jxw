package com.example.jxw.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.jxw.R;
import com.example.jxw.objects.StatusUpdate;
import com.example.jxw.util.RecordHandler;
import com.example.jxw.viewmodel.RobotViewModel;
import com.example.jxw.viewmodel.SharedViewModel;

import java.util.Map;

public class VideoFragment extends Fragment {
    private static final String TAG = "VideoFragment";
    private static final String ARG_CHANNEL = "channel";
    private RecordHandler recordHandler;
    private RobotViewModel robotViewModel;
    private SharedViewModel sharedViewModel;
    private VideoView videoView;
    private Map<String, String> emotionVideoMap;
    private boolean shouldLoopVideo = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        recordHandler = ((MainActivity) requireActivity()).getRecordHandler();
        Log.d("Recording", "[VideoFragment]RecordHandler obtained: " + (recordHandler != null));
        robotViewModel = new ViewModelProvider(requireActivity()).get(RobotViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        sharedViewModel.getEmotionVideoMap().observe(this, map -> {  // Change getViewLifecycleOwner() to this
            this.emotionVideoMap = map;
        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);
        videoView = view.findViewById(R.id.video_view);
        //開始錄影
        Log.d("Recording", "[VideoFragment]Starting recording in VideoFragment");
//        recordHandler.startRecording(getViewLifecycleOwner());
        // 只要從 RobotViewModel 觀察與
        setupVideoCompletionListener();
        observeEmotionChanges();
        observeTTSState();
        // 開啟第一次對話
        robotViewModel.setStatus("thinking", "hi");
        return view;
    }


    // 透過 LiveData 觀察來處理影片播放
    private void observeEmotionChanges() {
        robotViewModel.getEmotionLiveData().observe(getViewLifecycleOwner(), emotionKey -> {
            if (emotionKey != null) {
                String videoPath = emotionVideoMap.get(emotionKey);
                if (videoPath != null) {
                    Uri videoUri = Uri.parse(videoPath);
                    videoView.setVideoURI(videoUri);
                    videoView.start();
                    Log.d(TAG, "Playing video for emotion: " + emotionKey);
                } else {
                    Log.e(TAG, "No video path found for emotion: " + emotionKey);
                }
            }
        });
    }
    private void observeTTSState() {
        robotViewModel.getTtsPlayingState().observe(getViewLifecycleOwner(), isPlaying -> {
            shouldLoopVideo = isPlaying;
            if (!isPlaying && videoView.isPlaying()) {
                videoView.stopPlayback();
            }
        });
    }
    private void setupVideoCompletionListener() {
        videoView.setOnCompletionListener(mp -> {
            if (shouldLoopVideo) {
                videoView.start();
            }
        });
    }
    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "VideoFragment onPause called");
        // 暫停視頻播放
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "VideoFragment onStop called");
        // 停止視頻播放
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "VideoFragment onDestroyView called - performing cleanup");

        // Stop video playback
        if (videoView != null) {
            videoView.stopPlayback();
            videoView = null; // Allow for garbage collection
        }

        // Remove observers to prevent memory leaks
        if (robotViewModel != null) {
            // Remove any observers that might prevent garbage collection
            robotViewModel.getStatusLiveData().removeObservers(getViewLifecycleOwner());
            robotViewModel.getEmotionLiveData().removeObservers(getViewLifecycleOwner());
            robotViewModel.getTtsPlayingState().removeObservers(getViewLifecycleOwner());
        }

        if (sharedViewModel != null) {
            sharedViewModel.getEmotionVideoMap().removeObservers(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "VideoFragment onDestroy called - finalizing cleanup");

        // Ensure recordHandler is destroyed
        if (recordHandler != null) {
            recordHandler.destroy();
            recordHandler = null;
        }

        // Ensure robot is fully reset
        if (robotViewModel != null) {
            robotViewModel.interruptAndReset();
        }

        // Force garbage collection
        System.gc();
    }
}
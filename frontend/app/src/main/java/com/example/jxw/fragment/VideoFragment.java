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

        setupVideoCompletionListener();
        observeActionResponse();
        observeStatusUpdate();
        observeEmotionChanges();
        observeTTSState();
        // 開啟第一次對話
        robotViewModel.setStatus("thinking", "hi");
        return view;
    }

    // 觀察 ActionResponse 的變化，根據數據變化更新 UI
    private void observeActionResponse() {
        robotViewModel.getActionResponse().observe(getViewLifecycleOwner(), actionResponse -> {
            if (actionResponse != null) {
                // 更新 UI 和播放影片
                Log.d(TAG,"Received response: " + actionResponse.getTalk() +actionResponse.getEmotion());
//                Toast.makeText(getContext(), "Received response: " + actionResponse.getTalk(), Toast.LENGTH_LONG).show();

                // 通知 RobotViewModel 進行講話
                robotViewModel.setStatus("speaking", actionResponse.getTalk(), actionResponse.getEmotion());
            }
        });
    }

    private void observeStatusUpdate(){
        robotViewModel.getStatusLiveData().observe(getViewLifecycleOwner(), statusUpdate -> {
            if (statusUpdate != null) {
                // 處理語音狀態更新
                Log.d(TAG, "Status Update: " + statusUpdate.getStatus());
                robotViewModel.setStatus(statusUpdate.getStatus(), statusUpdate.getResultString());
            }
        });
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
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "VideoFragment onDestroy called");
        // 釋放資源
        if (recordHandler != null) {
            recordHandler.destroy();
            Log.d(TAG, "RecordHandler destroyed");
        }
        robotViewModel.interruptAndReset();
        Log.d(TAG, "Robot reset completed");
    }
}
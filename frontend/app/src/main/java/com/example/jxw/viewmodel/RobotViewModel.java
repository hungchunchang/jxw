package com.example.jxw.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jxw.objects.StatusUpdate;
import com.example.jxw.repository.DataRepository;
import com.example.jxw.util.CameraHandler;
import com.nuwarobotics.service.agent.NuwaRobotAPI;

import java.util.HashMap;
import java.util.Map;

public class RobotViewModel extends ViewModel {
    private final NuwaRobotAPI mRobotAPI;
    private final Map<String, String> motionMap;
    private final LiveData<StatusUpdate> statusLiveData;
    private final MutableLiveData<Boolean> ttsPlayingLiveData = new MutableLiveData<>();


    private final CameraHandler cameraHandler;
    private final DataRepository dataRepository;
    private String personalityType = "e";
    private final String TAG = "RobotViewModel";
    private boolean isEnding = false;

    public RobotViewModel(NuwaRobotAPI robotAPI, DataRepository dataRepository, CameraHandler cameraHandler, HashMap<String, String> emotionVideoMap) {
        this.mRobotAPI = robotAPI;
        this.dataRepository = dataRepository;
        this.cameraHandler = cameraHandler;
        this.motionMap = new HashMap<>();
        initializeMotionMap();
        this.statusLiveData = dataRepository.getStatusLiveData();
        this.statusLiveData.observeForever(statusUpdate -> {
            if (statusUpdate != null) {
                // 调用自己的 setStatus 方法来处理状态更新
                setStatus(statusUpdate.getStatus(), statusUpdate.getResultString());
            }
        });

    }

    public DataRepository getDataRepository() {
        return dataRepository;
    }

    private void initializeMotionMap() {
        // Example mappings
        motionMap.put("idling", "666_SA_Discover");
        motionMap.put("thinking", "666_PE_PushGlasses");
        motionMap.put("listening", "666_SA_Think");
        motionMap.put("speaking", "666_RE_Ask");
        motionMap.put("takePicture", "");
        motionMap.put("error", "");
        motionMap.put("default", "");
        motionMap.put("bye", "666_RE_Bye");
    }

    // 獲取完整的表情鍵值
    private String getFullEmotionKey(String baseStatus) {
        if (personalityType == null) {
            Log.w(TAG, "Personality type not set, defaulting to extrovert");
            personalityType = "e";
        }

        // 確保 baseEmotion 不為 null
        if (baseStatus == null || baseStatus.isEmpty()) {
            baseStatus = "neutral";
        }

        return personalityType + "_" + baseStatus;
    }

    // LiveData

    // 收到訊息
    public LiveData<StatusUpdate> getStatusLiveData() {
        return dataRepository.getStatusLiveData();
    }

    // 提供 LiveData 給 Fragment 觀察
    public LiveData<Boolean> getSwitchToUserFragment() {
        return dataRepository.getSwitchToUserFragment();
    }


    // //Emotion LiveData
    private final MutableLiveData<String> emotionLiveData = new MutableLiveData<>();
    public LiveData<String> getEmotionLiveData() {
        return emotionLiveData;
    }

    public LiveData<Boolean> getTtsPlayingState() {
        return ttsPlayingLiveData;
    }

    // // 在應用程式開始後，送出第一條訊息
    public void setInitialData(String userName, String userId, String personality, String channel) {
        //把使用者的基本資料儲存在 DataRepository 中
        dataRepository.setUserInfo(userName, userId, personality, channel);
    }

    // 機器人行為控制：包涵一、二、三個參數的重載
    // 基本的狀態設置
    // 帶結果字符串的狀態設置
    public void setStatus(String status, String resultString) {
        setStatus(new StatusUpdate(status, resultString));
    }

    // 說話狀態的特殊處理（帶情緒）
    public void setStatus(StatusUpdate statusUpdate) {
        Log.d(TAG, "Status: " + statusUpdate.getStatus()); // 通用的狀態記錄

        // 1. 播放對應動作
        String actualMotion = motionMap.getOrDefault(statusUpdate.getStatus(), "");
        if (actualMotion != null && !actualMotion.isEmpty()) {
            if (!"thinking".equals(statusUpdate.getStatus()) || Math.random() > 0.5) {
                mRobotAPI.motionPlay(actualMotion, true);
            }
        }
        // 2. 播放對應表情影片
        if (statusUpdate.getStatus().equals("speaking") && statusUpdate.getEmotion() != null) {
            // Speaking 狀態且有情緒時使用情緒表情
            String fullEmotionKey = getFullEmotionKey(statusUpdate.getEmotion());
            emotionLiveData.setValue(fullEmotionKey);
        } else {
            // 其他狀態使用狀態對應的表情
            String fullEmotionKey = getFullEmotionKey(statusUpdate.getStatus());
            emotionLiveData.setValue(fullEmotionKey);
        }

        switch (statusUpdate.getStatus()) {
            case "idling":
                Log.d(TAG, "Robot is idling.");
                break;

            case "thinking":
                Log.d(TAG, "Send result to server. Result: " + statusUpdate.getResultString());
                mRobotAPI.stopListen();
                dataRepository.sendDataViaHttp(statusUpdate.getResultString(), "");
                break;

            case "listening":
                Log.d(TAG, "Start Mix Understanding.");
                ttsPlayingLiveData.setValue(false);
                if(isEnding){
                    String byeMotion = motionMap.getOrDefault("bye", "");
                    // 播放动作 - 完成后会触发 onCompleteOfMotionPlay 回调
                    mRobotAPI.motionPlay(byeMotion, true);
                } else {
                    mRobotAPI.startMixUnderstand();
                }
                break;

            case "speaking":
                Log.d(TAG, "Start TTS: " + statusUpdate.getResultString());
                ttsPlayingLiveData.setValue(true);
                mRobotAPI.startTTS(statusUpdate.getResultString());
                break;

            case "takingPicture":
                Log.d(TAG, "Taking picture with description: " + statusUpdate.getResultString());
                cameraHandler.takePicture(statusUpdate.getResultString());
                break;

            case "error":
                Log.d(TAG, "An error occurred.");
                break;

            case "ending":
                Log.d(TAG, "Ending the conversation.");
                isEnding = true;
                setStatus(new StatusUpdate("speaking", statusUpdate.getResultString(),statusUpdate.getEmotion()));
                break;
            case "reset":
                Log.d(TAG, "Resetting robot actions.");
                // 發出切換到 UserFragment 的請求
                isEnding = false;
                dataRepository.requestSwitchToUserFragment();
                interruptAndReset();
                break;

            default:
                Log.w(TAG, "Unknown status: " + statusUpdate.getStatus());
                break;
        }
    }

    public void interruptAndReset() {
        Log.d(TAG, "interruptAndReset: Starting comprehensive cleanup");

        if (mRobotAPI != null) {
            // Stop all actions and sounds
            mRobotAPI.motionStop(true);
            mRobotAPI.stopTTS();
            mRobotAPI.stopListen();

            // Hide robot face screen
            mRobotAPI.UnityFaceManager().hideFace();

            // Reset actions and expressions
            mRobotAPI.motionReset();
        } else {
            Log.e(TAG, "interruptAndReset: mRobotAPI is null, cannot stop actions");
        }

        // Reset status flags
        ttsPlayingLiveData.setValue(false);
        emotionLiveData.setValue(getFullEmotionKey("reset"));

        // Force garbage collection to release any lingering resources
        System.gc();
    }
    public void cleanupObservers() {
        Log.d(TAG, "Cleaning up all observers and listeners");

        // If you have any observers or listeners that were added with observeForever
        // make sure to remove them here to prevent memory leaks

        // Example:
        // if (someForeverObserver != null) {
        //     someLiveData.removeObserver(someForeverObserver);
        //     someForeverObserver = null;
        // }
    }
}

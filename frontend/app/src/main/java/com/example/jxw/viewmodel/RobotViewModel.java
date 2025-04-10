package com.example.jxw.viewmodel;

import android.os.Handler;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.jxw.objects.ActionResponse;
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

    public RobotViewModel(NuwaRobotAPI robotAPI, DataRepository dataRepository, CameraHandler cameraHandler, HashMap<String, String> emotionVideoMap) {
        this.mRobotAPI = robotAPI;
        this.dataRepository = dataRepository;
        this.cameraHandler = cameraHandler;
        this.motionMap = new HashMap<>();
        initializeMotionMap();
        this.statusLiveData = dataRepository.getStatusLiveData();

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
    // // 收到訊息
    public LiveData<ActionResponse> getActionResponse() {
        return dataRepository.getActionResponseLiveData();
    }

    // // 收到訊息
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

    // 用於直接設置情緒表情（用於 ActionResponse 的情況）
    public void setEmotion(String emotion) {
        emotionLiveData.setValue(emotion);
    }

    public LiveData<Boolean> getTtsPlayingState() {
        return ttsPlayingLiveData;
    }

    // // 在應用程式開始後，送出第一條訊息
    public void setInitialData(String userName, String userId, String personality, String channel) {
        // 把人格儲存在 RobotViewModel中：只有內外向
        setMBTIPersonality(personality);

        //把使用者的基本資料儲存在 DataRepository 中
        dataRepository.setUserInfo(userName, userId, personality, channel);
    }

    // 處理 MBTI 性格類型
    private void setMBTIPersonality(String mbtiType) {
        if (mbtiType != null && !mbtiType.isEmpty()) {
            // 取第一個字母 E 或 I 來決定性格類型
            String firstLetter = mbtiType.substring(0, 1).toUpperCase();
            this.personalityType = firstLetter.equals("E") ? "e" : "i";
            Log.d(TAG, "Personality set to: " + this.personalityType + " from MBTI: " + mbtiType);
        } else {
            // 默認為內向
            this.personalityType = "e";
            Log.d(TAG, "Invalid MBTI type, defaulting to introvert");
        }
    }

    // 機器人行為控制：包涵一、二、三個參數的重載
    // 基本的狀態設置
    // 帶結果字符串的狀態設置
    public void setStatus(String status, String resultString) {
        setStatus(status, resultString, null);
    }

    // 說話狀態的特殊處理（帶情緒）
    public void setStatus(String status, String resultString, String emotion) {
        Log.d(TAG, "Status: " + status); // 通用的狀態記錄

        // 1. 播放對應動作
        String actualMotion = motionMap.getOrDefault(status, "");
        if (actualMotion != null && !actualMotion.isEmpty()) {
            if (!"thinking".equals(status) || Math.random() > 0.5) {
                mRobotAPI.motionPlay(actualMotion, true);
            }
        }
        // 2. 播放對應表情影片
        if (status.equals("speaking") && emotion != null) {
            // Speaking 狀態且有情緒時使用情緒表情
            String fullEmotionKey = getFullEmotionKey(emotion);
            emotionLiveData.setValue(fullEmotionKey);
        } else {
            // 其他狀態使用狀態對應的表情
            String fullEmotionKey = getFullEmotionKey(status);
            emotionLiveData.setValue(fullEmotionKey);
        }

        switch (status) {
            case "idling":
                Log.d(TAG, "Robot is idling.");
                break;

            case "thinking":
                Log.d(TAG, "Send result to server. Result: " + resultString);
                mRobotAPI.stopListen();
                dataRepository.sendDataViaHttp(resultString, "");
                break;

            case "listening":
                Log.d(TAG, "Start Mix Understanding.");
                ttsPlayingLiveData.setValue(false);
                mRobotAPI.startMixUnderstand();
                break;

            case "speaking":
                Log.d(TAG, "Start TTS: " + resultString);
                ttsPlayingLiveData.setValue(true);
                mRobotAPI.startTTS(resultString);
                break;

            case "takingPicture":
                Log.d(TAG, "Taking picture with description: " + resultString);
                cameraHandler.takePicture(resultString);
                break;

            case "error":
                Log.d(TAG, "An error occurred.");
                break;

            case "reset":
                Log.d(TAG, "Resetting robot actions.");
                // 發出切換到 UserFragment 的請求
                dataRepository.requestSwitchToUserFragment();

                interruptAndReset();
                break;

            default:
                Log.w(TAG, "Unknown status: " + status);
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
        // 移除所有永久觀察者
        if (dataRepository != null) {
            // 如果你使用了 observeForever，需要在這裡移除
        }
    }
}

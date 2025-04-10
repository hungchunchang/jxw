package com.example.jxw.repository;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.jxw.objects.ActionResponse;
import com.example.jxw.objects.StatusUpdate;
import com.example.jxw.util.HttpHandlerInterface;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;

public class DataRepository {
    private final HttpHandlerInterface httpHandler;
    private final MutableLiveData<ActionResponse> actionResponseLiveData = new MutableLiveData<>();
    private final MutableLiveData<StatusUpdate> statusLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> switchToUserFragment = new MutableLiveData<>();


    private final String TAG = "DataRepository";
    private final ExecutorService executorService;
    // 使用者相關資訊
    private String userName;
    private String userId;
    private String personality;
    private String channel;

    public DataRepository(HttpHandlerInterface httpHandler, ExecutorService executorService) {
        this.httpHandler = httpHandler;
        this.executorService = executorService;
    }
    public void setUserInfo(String userName, String userId, String personality, String channel){
        this.userName = userName;
        this.userId = userId;
        this.personality = personality;
        this.channel = channel;
        Log.d(TAG, "user info set");
    }

    public void handleCapturedImage(String resultString, Bitmap imageBitmap) {
        if (httpHandler != null) {
            executorService.execute(() -> {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                byte[] imageBytes = byteArrayOutputStream.toByteArray();
                String imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                Log.d(TAG, "Sending picture via Http...");
                httpHandler.sendDataAndFetch(resultString, imageBase64, userName, userId, personality, channel);
            });
        }
    }

    public void sendDataViaHttp(String resultString, String imageBitmap) {
        if (resultString != null) {
            executorService.execute(() -> httpHandler.sendDataAndFetch(resultString, imageBitmap, userName, userId, personality, channel));
            Log.d(TAG, "message sent to " + channel);
        } else {
            Log.e(TAG, "result Text is null or empty(http)");
        }
    }

    public void sendContinuousImage(String base64Image) {
//        if (httpHandler != null) {
//            httpHandler.sendContinuousImage(userName, userId, base64Image);
//        }
    }
    public void cleanup() {
        Log.d(TAG, "DataRepository: Performing complete cleanup");

        // Reset all LiveData objects to null or initial values
        actionResponseLiveData.setValue(null);
        statusLiveData.setValue(new StatusUpdate("reset", null));
        switchToUserFragment.setValue(false);

        // If you have any ongoing operations or threads, cancel them here
    }


    // 更新 `ActionResponse` 的方法
    public void updateActionResponse(ActionResponse actionResponse) {
        actionResponseLiveData.postValue(actionResponse); // 更新數據
    }

    // 供 `ViewModel` 獲取 `LiveData`
    public LiveData<ActionResponse> getActionResponseLiveData() {
        return actionResponseLiveData;
    }

    // 提供單一的 LiveData 給外部觀察
    public LiveData<StatusUpdate> getStatusLiveData() {
        return statusLiveData;
    }

    // 更新狀態並選擇性地包含結果字串
    public void updateStatus(String status, String resultString) {
        Log.d(TAG, "status updated to" + status);
        statusLiveData.postValue(new StatusUpdate(status, resultString));
    }

    // 如果不需要 resultString，可以使用此方法
    public void updateStatus(String status) {
        updateStatus(status, null);
    }

    // 提供 LiveData 給外部觀察
    public LiveData<Boolean> getSwitchToUserFragment() {
        return switchToUserFragment;
    }

    // 更新切換請求
    public void requestSwitchToUserFragment() {
        switchToUserFragment.postValue(true);
    }

    public void resetSwitchToUserFragment() {
        switchToUserFragment.postValue(false);
    }
}
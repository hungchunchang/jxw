package com.example.jxw.util;

import static android.content.ContentValues.TAG;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.jxw.objects.StatusUpdate;
import com.example.jxw.repository.DataRepository;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

public class HttpHandler implements HttpHandlerInterface {

    private static final String BASE_URL = "http://140.112.92.133:8080/api/";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executorService;
    private DataRepository dataRepository;
    private volatile boolean cancelRequests = false;

    public HttpHandler(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setDataRepository(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    public void sendDataAndFetch(String message, String imgBase64, String userName,
                                 String userId, String personality, String channel) {
        sendRequest(message, imgBase64, userName, userId, personality, channel);
    }

    @Override
    public void sendContinuousImage(String userName, String userId, String imgBase64) {
        sendRequest(null, imgBase64, userName, userId, null, "save_pic");
    }

    public void sendRequest(String message, String imgBase64, String userName,
                            String userId, String personality, String channel) {
        resetCancelFlag();
        JSONObject data = new JSONObject();
        try {
            data.put("user_id", userId);
            data.put("user_name", userName);
            data.put("img_base64", imgBase64);

            if (!"save_pic".equals(channel)) {
                data.put("robot_mbti", personality);
                data.put("message", message);
            }

            sendHttpRequest(BASE_URL + channel, data);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON", e);
        }
    }

    private void sendHttpRequest(String url, JSONObject data) {
        executorService.execute(() -> {
            if (cancelRequests) {
                Log.d(TAG, "Request cancelled before execution: " + url);
                return;
            }

            HttpURLConnection connection = null;
            try {
                // Check again before setting up connection
                if (cancelRequests) {
                    Log.d(TAG, "Request cancelled before connection setup: " + url);
                    return;
                }
                connection = setupConnection(url);
                sendData(connection, data);

                // Check again before processing response
                if (cancelRequests) {
                    Log.d(TAG, "Request cancelled before processing response: " + url);
                    connection.disconnect();
                    return;
                }

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "HTTP Response Code: " + responseCode);

                if (dataRepository != null && !url.endsWith("save_pic")) {
                    StatusUpdate statusUpdate = getStatusHttp(connection);

                    if (!cancelRequests) {
                        mainHandler.post(() -> dataRepository.updateStatus(statusUpdate));
                    } else {
                        Log.d(TAG, "Response handling cancelled: " + url);
                    }
                    mainHandler.post(() -> dataRepository.updateStatus(statusUpdate));
                }
            } catch (Exception e) {
                Log.e(TAG, "Request failed", e);
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private HttpURLConnection setupConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setDoOutput(true);
        return connection;
    }

    private void sendData(HttpURLConnection connection, JSONObject data) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())) {
            writer.write(data.toString());
            writer.flush();
        }
    }
    public void cancelAllRequests() {
        Log.d(TAG, "Cancelling all pending HTTP requests");
        cancelRequests = true;
    }
    public void resetCancelFlag() {
        cancelRequests = false;
    }

    private static @NonNull StatusUpdate getStatusHttp(HttpURLConnection connection) throws IOException, JSONException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        String jsonString = response.toString();
        JSONObject jsonResponse = new JSONObject(jsonString);

        String status = "speaking";
        //這邊用reset的話，會直接跳走，所以要把 onTTsEnded 再回到主頁
        if(jsonResponse.optBoolean("is_ended")){
            status = "ending";
        }
        Log.d(TAG,"Status is"+status);

        String question = jsonResponse.optString("question", "");
        String emotion = jsonResponse.optString("emotion", "");
        Log.d(TAG, "message updating with message: "+ question);

        return new StatusUpdate(status, question, emotion);
    }
}
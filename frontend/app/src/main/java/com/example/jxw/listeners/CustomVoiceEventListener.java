package com.example.jxw.listeners;

import android.util.Log;

import com.example.jxw.repository.DataRepository;
import com.nuwarobotics.service.agent.VoiceEventListener;
import com.nuwarobotics.service.agent.VoiceResultJsonParser;

import org.json.JSONException;
import org.json.JSONObject;

public class CustomVoiceEventListener implements VoiceEventListener {

    private static final String TAG = "CustomVoiceEventListener";
    private final DataRepository dataRepository;



    public CustomVoiceEventListener(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    public void onWakeup(boolean isError, String score, float direction) {}

    @Override
    public void onTTSComplete(boolean isError) {
        Log.d(TAG, "TTS Complete, starting to listen again");
        if (isError) {
            dataRepository.updateStatus("error");
        } else {
            dataRepository.updateStatus("listening");
        }
    }

    @Override
    public void onSpeechRecognizeComplete(boolean isError, ResultType iFlyResult, String json) {}

    @Override
    public void onSpeech2TextComplete(boolean isError, String json) {
        Log.d(TAG, "onSpeech2TextComplete:" + !isError + ", json:" + json);

        String result_string = null;

        if (isValidJson(json)) {
            result_string = VoiceResultJsonParser.parseVoiceResult(json);
        } else {
            Log.e(TAG, "Invalid JSON format in onSpeech2TextComplete");
        }

        if (isError || result_string == null || result_string.trim().isEmpty()) {
            Log.e(TAG, "Speech to text result is empty or error occurred, restarting listening");
            new Thread(() -> dataRepository.updateStatus("listening")).start();
            return;
        }

        // 語音轉文字成功後，設定機器人進入思考狀態
        dataRepository.updateStatus("thinking", result_string);
    }

    @Override
    public void onMixUnderstandComplete(boolean isError, ResultType resultType, String s) {
        Log.d(TAG, "onMixUnderstandComplete isError:" + isError + ", json:" + s);
        String result_string = null;

        if (isValidJson(s)) {
            try {
                JSONObject jsonObject = new JSONObject(s);
                result_string = jsonObject.getString("result");
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse JSON", e);
            }
        } else {
            result_string = s;  // 若不是有效的 JSON 格式，直接使用原始字串
        }

        if (isError) {
            Log.e(TAG, "MixUnderstand error occurred, restarting listening");
            new Thread(() -> dataRepository.updateStatus("listening")).start();
            return;
        }

        if (result_string == null || result_string.trim().isEmpty()) {
            Log.e(TAG, "MixUnderstand result is empty, restarting listening");
            new Thread(() -> {
                if (dataRepository != null) {
                    dataRepository.updateStatus("listening");
                } else {
                    Log.e(TAG, "robotViewModel is null, cannot set status to Listening");
                }
            }).start();
            return;
        }

        // 檢查是否包含拍照指令
        if (result_string.contains("你看") || result_string.contains("這是什麼")) {
            Log.d(TAG, "Taking picture based on the command");
            dataRepository.updateStatus("takePicture", result_string);

        } else {
            Log.d(TAG, "Sending message to server");
            dataRepository.updateStatus("thinking",result_string);
        }
    }


    @Override
    public void onSpeechState(ListenType listenType, SpeechState speechState) {}

    @Override
    public void onSpeakState(SpeakType speakType, SpeakState speakState) {}

    @Override
    public void onGrammarState(boolean isError, String s) {
        Log.d(TAG, "onGrammarState error, " + s);
    }

    @Override
    public void onListenVolumeChanged(ListenType listenType, int i) {}

    @Override
    public void onHotwordChange(HotwordState hotwordState, HotwordType hotwordType, String s) {}

//    }

    private boolean isValidJson(String json) {
        try {
            new JSONObject(json);
            return true;
        } catch (JSONException ex) {
            return false;
        }
    }
}

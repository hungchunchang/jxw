package com.example.jxw.viewmodel;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Map;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<Map<String, String>> emotionVideoMap = new MutableLiveData<>();

    public void setEmotionVideoMap(Map<String, String> map) {
        emotionVideoMap.setValue(map);
    }

    public LiveData<Map<String, String>> getEmotionVideoMap() {
        return emotionVideoMap;
    }
}

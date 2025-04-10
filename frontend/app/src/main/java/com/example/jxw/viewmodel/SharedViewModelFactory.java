package com.example.jxw.viewmodel;


import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.Map;

public class SharedViewModelFactory implements ViewModelProvider.Factory {

    public SharedViewModelFactory(Map<String, String> emotionVideoMap) {
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SharedViewModel.class)) {
            return (T) new SharedViewModel();
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
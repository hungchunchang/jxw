package com.example.jxw.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.jxw.repository.DataRepository;
import com.example.jxw.util.CameraHandler;
import com.example.jxw.util.HttpHandlerInterface;
import com.nuwarobotics.service.agent.NuwaRobotAPI;

import java.util.HashMap;

public class RobotViewModelFactory implements ViewModelProvider.Factory {
    private final NuwaRobotAPI mRobotAPI;
    private final DataRepository dataRepository;
    private final CameraHandler cameraHandler;

    public RobotViewModelFactory(NuwaRobotAPI mRobotAPI, HttpHandlerInterface httpHandler, DataRepository dataRepository, CameraHandler cameraHandler, HashMap emotionVideoMap) {
        this.mRobotAPI = mRobotAPI;
        this.dataRepository = dataRepository;
        this.cameraHandler = cameraHandler;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(RobotViewModel.class))
            return (T) new RobotViewModel(mRobotAPI, dataRepository, cameraHandler);
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
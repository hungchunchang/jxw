package com.example.jxw.util;

public interface RobotEventCallback {
    void postToUiThread(Runnable runnable);
//    void resetUIToInitialState();
}
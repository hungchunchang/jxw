package com.example.jxw.objects;

public class StatusUpdate {
    private final String status;
    private final String resultString;

    private final String emotion;

    public StatusUpdate(String status, String resultString, String emotion) {
        this.status = status;
        this.resultString = resultString;
        this.emotion = emotion;
    }
    public StatusUpdate(String status, String resultString) {
        this.status = status;
        this.resultString = resultString;
        this.emotion = "";
        new StatusUpdate(status, resultString, emotion);
    }
    public StatusUpdate(String status) {
        this.status = status;
        this.resultString = "";
        this.emotion = "";
        new StatusUpdate(status, resultString, emotion);
    }



    public String getStatus() {
        return status;
    }

    public String getResultString() {
        return resultString;
    }
    public String getEmotion() {
        return emotion;
    }
}


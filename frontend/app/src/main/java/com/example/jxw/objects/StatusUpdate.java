package com.example.jxw.objects;

public class StatusUpdate {
    private final String status;
    private final String resultString; // 可選值

    public StatusUpdate(String status, String resultString) {
        this.status = status;
        this.resultString = resultString;
    }

    public String getStatus() {
        return status;
    }

    public String getResultString() {
        return resultString;
    }
}


package com.example.jxw.objects;

public class ActionResponse {
    private String action;
    private String emotion;
    private String talk;

    public ActionResponse(String action, String emotion, String talk) {
        this.action = action;
        this.emotion = emotion;
        this.talk = talk;
    }

    public String getAction() {
        return action;
    }

    public String getEmotion() {
        return emotion;
    }

    public String getTalk() {
        return talk;
    }
}

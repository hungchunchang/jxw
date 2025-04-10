package com.example.jxw.util;

import com.example.jxw.repository.DataRepository;

public interface HttpHandlerInterface {

    // Method for sending HTTP POST requests with JSON data
    void sendDataAndFetch(String resultString, String imgBase64, String userName, String userId, String personality, String channel);

    void setDataRepository(DataRepository dataRepository);

    void sendRequest(String message, String imgBase64, String userName,
                     String userId, String personality, String channel);


    void sendContinuousImage(String userName, String userId,String base64Image);

    void cancelAllRequests();
}
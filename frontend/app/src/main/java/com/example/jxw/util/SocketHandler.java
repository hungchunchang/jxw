//package com.example.xiao2.util;
//
//import android.content.Context;
//import android.os.Handler;
//import android.util.Log;
//
//import com.example.xiao2.repository.DataRepository;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.io.DataInputStream;
//import java.io.DataOutputStream;
//import java.io.IOException;
//import java.io.Serializable;
//import java.net.Socket;
//import java.nio.charset.StandardCharsets;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.function.Function;
//
//public class SocketHandler implements SocketHandlerInterface, Serializable {
//    private final String serverName;
//    private final int serverPort;
//    private final transient Handler mainHandler;
//    private transient Socket socket;
//    private transient DataOutputStream out;
//    private transient DataInputStream in;
//    private final transient Context context;
//    private boolean isConnected = false;
//    private OnMessageReceivedListener messageListener;
//    private ConnectionListener connectionListener;
//    private final Map<String, Function<String, String>> messageHandlers = new HashMap<>();
//
//    private final String TAG = SocketHandler.class.getSimpleName();
//    private DataRepository dataRepository;
//
//
//    public SocketHandler(Context context, String serverName, int serverPort) {
//        this.context = context;
//        this.serverName = serverName;
//        this.serverPort = serverPort;
//        this.mainHandler = new Handler(context.getMainLooper());
//    }
//
//    public void setDataRepository(DataRepository dataRepository) {
//        this.dataRepository = dataRepository;
//    }
//
//
//
//    public void connect(ConnectionListener listener) {
//        this.connectionListener = listener;
//        new Thread(() -> {
//            try {
//                socket = new Socket(serverName, serverPort);
//                out = new DataOutputStream(socket.getOutputStream());
//                in = new DataInputStream(socket.getInputStream());
//                isConnected = true; // 連接成功標誌
//                Log.d(TAG, "Connected to server");
//
//                if (connectionListener != null) {
//                    mainHandler.post(() -> connectionListener.onConnected());
//                }
//
//                // 開始接收訊息
//                startListening();
//
//            } catch (Exception e) {
//                Log.e(TAG, "Error connecting to server", e);
//                if (connectionListener != null) {
//                    mainHandler.post(() -> connectionListener.onDisconnected());
//                }
//            }
//        }).start();
//    }
//
//    private void startListening() {
//        new Thread(() -> {
//            try {
//                while (isConnected) {
//                    int messageLength = in.readInt();
//                    Log.d(TAG, "Expected message length: " + messageLength);
//                    if (messageLength > 0) {
//                        byte[] messageBytes = new byte[messageLength];
//                        in.readFully(messageBytes, 0, messageLength);
//                        String message = new String(messageBytes, StandardCharsets.UTF_8);
//                        Log.d(TAG, "Message received: " + message);
//                        handleMessage(message);
//                    }
//                }
//            } catch (IOException e) {
//                Log.e(TAG, "Error receiving data from the server", e);
//                if (connectionListener != null) {
//                    mainHandler.post(() -> connectionListener.onDisconnected());
//                }
//            } finally {
//                // 釋放資源
//                try {
//                    if (in != null) {
//                        in.close();
//                    }
//                } catch (IOException e) {
//                    Log.e(TAG, "Error closing DataInputStream", e);
//                }
//            }
//        }).start();
//    }
//
//    private void handleMessage(String message) {
//        try {
//            JSONObject json = new JSONObject(message);
//            String dataType = json.getString("data_type");
//            String output = json.getString("message");
//
//            switch (dataType) {
//                case "heartbeat":
//                    Log.d(TAG, "Heartbeat received");
//                    break;
//
//                case "text":
//                    Log.d(TAG, "Received message: " + output);
//                    dataRepository.updateMessage(output);
//                    break;
//
//                default:
//                    Log.e(TAG, "Unknown data type: " + dataType);
//                    break;
//            }
//        } catch (JSONException e) {
//            Log.e(TAG, "Error parsing JSON message", e);
//        }
//    }
//
//    public void sendData(String dataType, String data) {
//        if (data == null || data.isEmpty()) {
//            Log.e(TAG, "Data to send is null or empty");
//            return;
//        }
//
//        new Thread(() -> {
//            try {
//                JSONObject json = new JSONObject();
////                uuid
//                json.put("id", "id");
//                json.put("user_name","user_test");
//                json.put("data_type", dataType);
//                json.put("message", data);
//
//                byte[] messageBytes = json.toString().getBytes(StandardCharsets.UTF_8);
//                int messageLength = messageBytes.length;
//
//                synchronized (this) {
//                    if (!isConnected() || out == null) {
//                        Log.e(TAG, "Not connected to server, cannot send data");
//                        return;
//                    }
//                    Log.d(TAG, "Sending data: " + json.toString());
//                    out.writeInt(messageLength);
//                    out.write(messageBytes);
//                    out.flush();
//                }
//                Log.d(TAG, "Data sent successfully");
//            } catch (Exception e) {
//                Log.e(TAG, "Error sending data to the server", e);
//            }
//        }).start();
//    }
//
//    public void disconnect() {
//        synchronized (this) {
//            try {
//                if (socket != null) {
//                    socket.close();
//                    socket = null;
//                }
//                if (out != null) {
//                    out.close();
//                    out = null;
//                }
//                if (in != null) {
//                    in.close();
//                    in = null;
//                }
//                isConnected = false;
//                if (connectionListener != null) {
//                    mainHandler.post(() -> connectionListener.onDisconnected());
//                }
//            } catch (IOException e) {
//                Log.e("SocketHandler", "Error closing socket", e);
//            }
//        }
//    }
//
//
//    public boolean isConnected() {
//        return isConnected;
//    }
//
//    public void setMessageListener(OnMessageReceivedListener listener) {
//        this.messageListener = listener;
//    }
//
//    public Context getContext() {
//        return context;
//    }
//
//    public interface OnMessageReceivedListener {
//        void onMessageReceived(String message);
//    }
//
//    public interface ConnectionListener {
//        void onConnected();
//        void onDisconnected();
//    }
//}

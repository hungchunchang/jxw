package com.example.jxw.fragment;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.jxw.R;
import com.example.jxw.viewmodel.RobotViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class UserFragment extends Fragment {
    private RobotViewModel robotViewModel;
    private Button startButton;
    private View view;
    private FragmentTransaction transaction;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 獲取 ViewModel
        robotViewModel = new ViewModelProvider(requireActivity()).get(RobotViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 加載佈局
        view = inflater.inflate(R.layout.fragment_user, container, false);

//        view.setVisibility(View.VISIBLE);
        // 設置設定按鈕，切換到 SettingFragment
        ImageView settingsButton = view.findViewById(R.id.settings_button);
        if (settingsButton == null) {
            Log.e("SettingFragment", "settingsButton is null, check your XML layout.");
        } else {
            Log.d("SettingFragment", "settingsButton is found.");
        }
        assert settingsButton != null;
        settingsButton.setOnClickListener(v -> {
            Log.d("SettingFragment", "Trying to switch fragment.");
            v.post(() -> {
                try {
                    transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.fragment_container, new SettingFragment());
                    transaction.addToBackStack(null);
                    transaction.commit();
                    Log.d("SettingFragment", "Successfully switched to SettingFragment.");
                } catch (Exception e) {
                    Log.e("SettingFragment", "Error switching to SettingFragment", e);
                }
            });
        });

        // 設置開始按鈕
        startButton = view.findViewById(R.id.start_button);
        startButton.setVisibility(View.VISIBLE);  // 初始化設置為 GONE，稍後按需要顯示
        setupBreathingAnimation(startButton);
        startButton.setOnClickListener(v -> handleStartButtonClick());

        return view;
    }

    // 設置呼吸動畫
    private void setupBreathingAnimation(Button button) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.1f);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setRepeatMode(ObjectAnimator.REVERSE);
        scaleY.setRepeatMode(ObjectAnimator.REVERSE);
        scaleX.setDuration(1000);
        scaleY.setDuration(1000);
        scaleX.start();
        scaleY.start();
    }

    // 開始按鈕的點擊邏輯
    private void handleStartButtonClick() {
        // 清除動畫並隱藏按鈕
        startButton.clearAnimation();
        startButton.setVisibility(View.GONE);

        // 獲取 SharedPreferences
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE);

        // 檢查是否已有用戶ID，沒有則生成新的
        String userId = sharedPreferences.getString("user_id", null);
        if (userId == null) {
            // 為新使用者創建唯一 ID
            userId = UUID.randomUUID().toString();

            // 創建日期格式
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            // 獲取當前日期時間
            String createdDate = sdf.format(new Date());

            // 儲存使用者資料到 SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("user_id", userId);
            editor.putString("created_date", createdDate);
            editor.apply();

            Log.d("UserFragment", "新用戶ID已生成: " + userId);
        }

        // 使用訪客用戶名代替需要輸入的用戶名
        String username = "visitor_" + userId.substring(0, 8);

        // 將這些資訊發送到後端 - 移除 personality 和 channel 參數
        robotViewModel.setInitialData(username, userId);

        // 顯示提示
        Toast.makeText(requireContext(), "已連接到磯永吉小屋互動系統", Toast.LENGTH_SHORT).show();
        transactionToVideo();
    }
    private void transactionToVideo(){
        // 使用 FragmentTransaction 進行跳轉到 VideoFragment
        transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new VideoFragment());  // 跳轉到 VideoFragment
        transaction.addToBackStack(null);  // 可選：將該 Fragment 添加到返回棧
        transaction.commit();
    }


    @Override
    public void onResume() {
        super.onResume();
        // 當 Fragment 可見時，顯示整個視圖
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }

        // 確保機器人已重置
        if (robotViewModel != null) {
            robotViewModel.interruptAndReset();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // 當 Fragment 不可見時，隱藏整個視圖
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

}

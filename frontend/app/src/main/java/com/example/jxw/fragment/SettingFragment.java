package com.example.jxw.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.jxw.R;

public class SettingFragment extends Fragment {

    private String selectedPersonality = "INFP";  // 默認人格
    private String selectedChatType = "biography";     // 默認聊天模式
    private Button selectedPersonalityButton;     // 已選擇的人格按鈕
    private Button selectedChatTypeButton;        // 已選擇的聊天模式按鈕
    private SharedPreferences sharedPreferences;
    private View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_settings, container, false);

        // 初始化 SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE);

        // 綁定使用者資訊
        TextView userIdText = view.findViewById(R.id.user_id);
        TextView usernameText = view.findViewById(R.id.username);
        TextView passwordText = view.findViewById(R.id.password);
        TextView createdDateText = view.findViewById(R.id.created_date);

        // 從 SharedPreferences 中獲取數據
        String userId = sharedPreferences.getString("user_id", "N/A");
        String username = sharedPreferences.getString("username", "N/A");
        String password = sharedPreferences.getString("password", "N/A");  // 密碼以星號顯示
        String createdDate = sharedPreferences.getString("created_date", "N/A");

        // 設置數據到 TextView
        userIdText.setText(userId);
        usernameText.setText(username);
        passwordText.setText(password.replaceAll("\\.", "*"));  // 使用星號隱藏密碼
        createdDateText.setText(createdDate);

        // 人格選擇按鈕
        Button enfpButton = view.findViewById(R.id.enfp_button);
        Button entpButton = view.findViewById(R.id.entp_button);
        Button infjButton = view.findViewById(R.id.infj_button);
        Button infpButton = view.findViewById(R.id.infp_button);

        // 設置人格選擇按鈕組的單選邏輯
        setupSingleSelection(true, enfpButton, entpButton, infjButton, infpButton);

        // 聊天模式選擇按鈕
        Button chatButton = view.findViewById(R.id.chat_button);
        Button interviewButton = view.findViewById(R.id.interview_button);

        // 設置聊天模式選擇按鈕組的單選邏輯
        setupSingleSelection(false, chatButton, interviewButton);

        Button logoutButton = view.findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(v -> {
            // 顯示登出成功的訊息
            Toast.makeText(requireContext(), "已登出", Toast.LENGTH_SHORT).show();

            // 回到 LoginFragment
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new LoginFragment());  // 跳轉到 LoginFragment
            transaction.addToBackStack(null);  // 可選：將該 Fragment 添加到返回棧
            transaction.commit();
        });

        Button saveButton = view.findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> {
            // 保存選擇的數據到 SharedPreferences
            saveSettingsToSharedPreferences();

            // 顯示保存成功的訊息
            Toast.makeText(requireContext(), "設定已儲存", Toast.LENGTH_SHORT).show();

            // 回到 UserFragment
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new UserFragment());  // 跳轉到 UserFragment
            transaction.addToBackStack(null);  // 可選：將該 Fragment 添加到返回棧
            transaction.commit();
        });

        return view;
    }

    // 保存選擇的數據到 SharedPreferences
    private void saveSettingsToSharedPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("selected_personality", selectedPersonality);
        editor.putString("selected_chat_type", selectedChatType);
        editor.apply();  // 保存變更
    }

    private void setupSingleSelection(boolean isPersonality, Button... buttons) {
        for (Button button : buttons) {
            button.setOnClickListener(v -> {
                // 重置所有按鈕的樣式
                for (Button btn : buttons) {
                    resetButtonStyle(btn);
                }

                if (isPersonality) {
                    selectedPersonalityButton = button;
                    selectedPersonality = button.getText().toString();  // 更新選擇的人格
                } else {
                    selectedChatTypeButton = button;
                    selectedChatType = button.getText().toString();  // 更新選擇的聊天模式
                }

                // 設置當前選擇的按鈕樣式
                setSelectedButtonStyle(button);
            });
        }
    }

    // 重置按鈕樣式
    private void resetButtonStyle(Button button) {
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.button_normal));  // 使用自定義的默認按鈕背景色
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));  // 恢復默認文字顏色（深灰色）
    }

    // 設置選中的按鈕樣式
    private void setSelectedButtonStyle(Button button) {
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));  // 設置選中背景顏色（暖橘色）
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_primary));  // 設置選中時文字顏色（白色）
    }

    @Override
    public void onResume() {
        super.onResume();
        // 當 Fragment 可見時，顯示登出按鈕
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // 當 Fragment 不可見時，隱藏登出按鈕
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }
}

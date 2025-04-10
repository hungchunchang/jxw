package com.example.jxw.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.jxw.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class LoginFragment extends Fragment {

    private EditText usernameEditText, passwordEditText;

    // SharedPreferences 用於存儲用戶資料
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "user_data";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_USER_ID = "user_id"; // 用於存儲使用者 ID
    private static final String KEY_CREATED_DATE = "created_date"; // 用於存儲使用者 ID


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        usernameEditText = view.findViewById(R.id.usernameEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        Button loginButton = view.findViewById(R.id.loginButton);
        Button registerButton = view.findViewById(R.id.registerButton);

        // 初始化 SharedPreferences
        sharedPreferences = Objects.requireNonNull(requireActivity()).getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // 設置登入按鈕點擊事件
        loginButton.setOnClickListener(v -> attemptLogin());

        // 設置註冊按鈕點擊事件
        registerButton.setOnClickListener(v -> registerUser());

        // 設置點擊監聽器來隱藏鍵盤
        view.setOnTouchListener(this::onTouch);

        return view;
    }

    // 隱藏鍵盤的輔助方法
    private void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // 驗證使用者是否存在並處理登入邏輯
    private void attemptLogin() {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(getActivity(), "請輸入使用者名稱和密碼", Toast.LENGTH_SHORT).show();
            return;
        }

        // 驗證使用者是否存在
        String registeredUsername = sharedPreferences.getString(KEY_USERNAME, null);
        String registeredPassword = sharedPreferences.getString(KEY_PASSWORD, null);
        String userId = sharedPreferences.getString(KEY_USER_ID, null);

        if (username.equals(registeredUsername)) {
            if (password.equals(registeredPassword)) {
                Toast.makeText(getActivity(), "登入成功，使用者 ID：" + userId, Toast.LENGTH_SHORT).show();
                Log.d("LoginFragment", "使用者登入成功，使用者 ID：" + userId);
                // 成功登入後跳轉到主畫面
                proceedToNextScreen();
            } else {
                Toast.makeText(getActivity(), "密碼錯誤", Toast.LENGTH_SHORT).show();
                Log.d("LoginFragment", "密碼錯誤");
            }
        } else {
            Toast.makeText(getActivity(), "使用者尚未註冊", Toast.LENGTH_SHORT).show();
            Log.d("LoginFragment", "使用者尚未註冊");
        }
    }

    // 註冊新使用者
    private void registerUser() {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(getActivity(), "請輸入使用者名稱和密碼進行註冊", Toast.LENGTH_SHORT).show();
            return;
        }

        // 為新使用者創建唯一 ID
        String userId = UUID.randomUUID().toString();
        // 創建日期格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        // 獲取當前日期時間
        String createdDate = sdf.format(new Date());

        // 儲存使用者資料到 SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_PASSWORD, password);
        editor.putString(KEY_USER_ID, userId);  // 存儲 user_id
        editor.putString(KEY_CREATED_DATE, createdDate);
        editor.apply();

        Toast.makeText(getActivity(), "註冊成功，使用者 ID：" + userId, Toast.LENGTH_SHORT).show();
        Log.d("LoginFragment", "使用者註冊成功，使用者 ID：" + userId);

        // 註冊成功後進入下一個畫面
        proceedToNextScreen();
    }

    // 跳轉到 UserFragment
    private void proceedToNextScreen() {
        View loginLayout = requireActivity().findViewById(R.id.login_layout);
        if (loginLayout != null) {
            loginLayout.setVisibility(View.GONE);  // 隱藏整個主佈局，所有子元素都會隱藏
        }
        // 使用 FragmentTransaction 進行跳轉
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new UserFragment());  // 跳轉到 UserFragment
        transaction.addToBackStack(null);  // 可選：將該 Fragment 添加到返回棧
        transaction.commit();
    }

    private boolean onTouch(View v, MotionEvent event) {
        hideKeyboard();
        return false;
    }

}

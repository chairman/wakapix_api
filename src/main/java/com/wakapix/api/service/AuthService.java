package com.wakapix.api.service;

import com.wakapix.api.dto.request.LoginRequest;
import com.wakapix.api.dto.request.RegisterRequest;
import com.wakapix.api.dto.request.SmsLoginRequest;
import com.wakapix.api.dto.response.LoginResponse;

public interface AuthService {

    void register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    LoginResponse smsLogin(SmsLoginRequest request);

    void logout(Long userId);

    void sendSmsCode(String phone);

    LoginResponse wechatLogin(String code);

    String getWechatQrCodeUrl();
}

package com.wakapix.api.controller;

import com.wakapix.api.dto.request.LoginRequest;
import com.wakapix.api.dto.request.RegisterRequest;
import com.wakapix.api.dto.request.SmsLoginRequest;
import com.wakapix.api.dto.response.ApiResponse;
import com.wakapix.api.dto.response.LoginResponse;
import com.wakapix.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户注册、登录、退出等认证相关接口")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "创建新用户账号，支持用户名、密码、邮箱、手机号")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("注册成功", null));
    }

    @PostMapping("/login")
    @Operation(summary = "账号密码登录", description = "使用用户名和密码进行登录")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }

    @PostMapping("/sms/send")
    @Operation(summary = "发送短信验证码", description = "向指定手机号发送验证码，用于短信登录")
    public ResponseEntity<ApiResponse<Void>> sendSmsCode(@Parameter(description = "手机号", required = true) @RequestParam String phone) {
        authService.sendSmsCode(phone);
        return ResponseEntity.ok(ApiResponse.success("验证码已发送", null));
    }

    @PostMapping("/sms/login")
    @Operation(summary = "短信验证码登录", description = "使用手机号和验证码登录，未注册用户将自动注册")
    public ResponseEntity<ApiResponse<LoginResponse>> smsLogin(@Valid @RequestBody SmsLoginRequest request) {
        LoginResponse response = authService.smsLogin(request);
        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "清除用户登录状态和token")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.success("退出成功", null));
    }

    @GetMapping("/wechat/qrcode")
    @Operation(summary = "获取微信扫码登录二维码", description = "获取微信扫码登录的二维码URL")
    public ResponseEntity<ApiResponse<String>> getWechatQrCodeUrl() {
        String url = authService.getWechatQrCodeUrl();
        return ResponseEntity.ok(ApiResponse.success(url));
    }

    @GetMapping("/wechat/callback")
    @Operation(summary = "微信登录回调", description = "微信扫码登录成功后的回调接口，携带code参数")
    public ResponseEntity<ApiResponse<LoginResponse>> wechatCallback(@Parameter(description = "微信授权code", required = true) @RequestParam String code) {
        LoginResponse response = authService.wechatLogin(code);
        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }
}
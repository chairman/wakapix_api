package com.wakapix.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wakapix.api.config.JwtAuthenticationFilter;
import com.wakapix.api.config.SecurityConfig;
import com.wakapix.api.dto.request.LoginRequest;
import com.wakapix.api.dto.request.RegisterRequest;
import com.wakapix.api.dto.request.SmsLoginRequest;
import com.wakapix.api.dto.response.LoginResponse;
import com.wakapix.api.service.AuthService;
import com.wakapix.api.util.JwtTokenUtil;
import com.wakapix.api.util.TokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private TokenManager tokenManager;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private SmsLoginRequest smsLoginRequest;
    private LoginResponse loginResponse;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPhone("13800138000");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        smsLoginRequest = new SmsLoginRequest();
        smsLoginRequest.setPhone("13800138000");
        smsLoginRequest.setCode("123456");

        loginResponse = LoginResponse.builder()
                .token("test-token")
                .user(LoginResponse.UserInfo.builder()
                        .id(1L)
                        .username("testuser")
                        .nickname("测试用户")
                        .phone("13800138000")
                        .build())
                .build();
    }

    @Test
    @DisplayName("用户注册 - 成功")
    void register_Success() throws Exception {
        doNothing().when(authService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("注册成功"));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("用户注册 - 用户名为空")
    void register_EmptyUsername() throws Exception {
        registerRequest.setUsername("");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("用户登录 - 成功")
    void login_Success() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.token").value("test-token"))
                .andExpect(jsonPath("$.data.user.username").value("testuser"));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("发送短信验证码 - 成功")
    void sendSmsCode_Success() throws Exception {
        doNothing().when(authService).sendSmsCode(anyString());

        mockMvc.perform(post("/api/auth/sms/send")
                        .with(csrf())
                        .param("phone", "13800138000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("验证码已发送"));

        verify(authService, times(1)).sendSmsCode("13800138000");
    }

    @Test
    @DisplayName("短信验证码登录 - 成功")
    void smsLogin_Success() throws Exception {
        when(authService.smsLogin(any(SmsLoginRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/sms/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(smsLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.token").value("test-token"));

        verify(authService, times(1)).smsLogin(any(SmsLoginRequest.class));
    }

    @Test
    @DisplayName("获取微信二维码URL - 成功")
    void getWechatQrCodeUrl_Success() throws Exception {
        String qrUrl = "https://open.weixin.qq.com/connect/qrconnect?appid=test";
        when(authService.getWechatQrCodeUrl()).thenReturn(qrUrl);

        mockMvc.perform(get("/api/auth/wechat/qrcode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(qrUrl));

        verify(authService, times(1)).getWechatQrCodeUrl();
    }

    @Test
    @DisplayName("微信回调登录 - 成功")
    void wechatCallback_Success() throws Exception {
        when(authService.wechatLogin(anyString())).thenReturn(loginResponse);

        mockMvc.perform(get("/api/auth/wechat/callback")
                        .param("code", "test-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.token").value("test-token"));

        verify(authService, times(1)).wechatLogin("test-code");
    }
}
package com.wakapix.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wakapix.api.dto.request.LoginRequest;
import com.wakapix.api.dto.request.RegisterRequest;
import com.wakapix.api.dto.request.SmsLoginRequest;
import com.wakapix.api.dto.response.LoginResponse;
import com.wakapix.api.entity.User;
import com.wakapix.api.exception.BusinessException;
import com.wakapix.api.mapper.LoginLogMapper;
import com.wakapix.api.mapper.UserMapper;
import com.wakapix.api.util.JwtTokenUtil;
import com.wakapix.api.util.PasswordUtil;
import com.wakapix.api.util.SmsCodeUtil;
import com.wakapix.api.util.TokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private LoginLogMapper loginLogMapper;

    @Mock
    private PasswordUtil passwordUtil;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private TokenManager tokenManager;

    @Mock
    private SmsCodeUtil smsCodeUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private SmsLoginRequest smsLoginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "wechatAppId", "wx_test_appid");
        ReflectionTestUtils.setField(authService, "wechatRedirectUri", "http://localhost:8080/api/auth/wechat/callback");

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

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encryptedPassword")
                .email("test@example.com")
                .phone("13800138000")
                .nickname("测试用户")
                .status(1)
                .build();
    }

    @Test
    @DisplayName("用户注册 - 成功")
    void register_Success() {
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordUtil.encrypt(anyString())).thenReturn("encryptedPassword");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        authService.register(registerRequest);

        verify(userMapper, times(1)).insert(any(User.class));
    }

    @Test
    @DisplayName("用户注册 - 用户名已存在")
    void register_UsernameExists() {
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals(400, exception.getCode());
        assertEquals("用户名已存在", exception.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("用户注册 - 邮箱已存在")
    void register_EmailExists() {
        when(userMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L)  // 第一次：检查用户名
                .thenReturn(1L); // 第二次：检查邮箱

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals(400, exception.getCode());
        assertEquals("邮箱已被注册", exception.getMessage());
    }

    @Test
    @DisplayName("用户登录 - 成功")
    void login_Success() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
        when(passwordUtil.verify(anyString(), anyString())).thenReturn(true);
        when(jwtTokenUtil.generateToken(anyLong(), anyString())).thenReturn("test-token");

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("test-token", response.getToken());
        assertEquals("testuser", response.getUser().getUsername());
        verify(tokenManager, times(1)).saveToken(1L, "test-token");
        verify(loginLogMapper, times(1)).insert(any());
    }

    @Test
    @DisplayName("用户登录 - 用户不存在")
    void login_UserNotFound() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals(400, exception.getCode());
        assertEquals("账号或密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("用户登录 - 密码错误")
    void login_WrongPassword() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
        when(passwordUtil.verify(anyString(), anyString())).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals(400, exception.getCode());
        assertEquals("账号或密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("用户登录 - 账号已禁用")
    void login_AccountDisabled() {
        testUser.setStatus(0);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
        when(passwordUtil.verify(anyString(), anyString())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals(400, exception.getCode());
        assertEquals("账号已被禁用", exception.getMessage());
    }

    @Test
    @DisplayName("短信验证码登录 - 已注册用户")
    void smsLogin_ExistingUser() {
        when(smsCodeUtil.verifyCode(anyString(), anyString())).thenReturn(true);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
        when(jwtTokenUtil.generateToken(anyLong(), anyString())).thenReturn("test-token");

        LoginResponse response = authService.smsLogin(smsLoginRequest);

        assertNotNull(response);
        assertEquals("test-token", response.getToken());
    }

    @Test
    @DisplayName("短信验证码登录 - 新用户自动注册")
    void smsLogin_NewUserAutoRegister() {
        when(smsCodeUtil.verifyCode(anyString(), anyString())).thenReturn(true);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return 1;
        });
        when(jwtTokenUtil.generateToken(anyLong(), anyString())).thenReturn("test-token");

        LoginResponse response = authService.smsLogin(smsLoginRequest);

        assertNotNull(response);
        assertEquals("test-token", response.getToken());
        verify(userMapper, times(1)).insert(any(User.class));
    }

    @Test
    @DisplayName("短信验证码登录 - 验证码错误")
    void smsLogin_InvalidCode() {
        when(smsCodeUtil.verifyCode(anyString(), anyString())).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.smsLogin(smsLoginRequest);
        });

        assertEquals(400, exception.getCode());
        assertEquals("验证码错误或已过期", exception.getMessage());
    }

    @Test
    @DisplayName("退出登录")
    void logout_Success() {
        doNothing().when(tokenManager).removeToken(anyLong());

        authService.logout(1L);

        verify(tokenManager, times(1)).removeToken(1L);
    }

    @Test
    @DisplayName("发送短信验证码")
    void sendSmsCode_Success() {
        doNothing().when(smsCodeUtil).sendCode(anyString());

        authService.sendSmsCode("13800138000");

        verify(smsCodeUtil, times(1)).sendCode("13800138000");
    }

    @Test
    @DisplayName("获取微信二维码URL")
    void getWechatQrCodeUrl_Success() {
        String url = authService.getWechatQrCodeUrl();

        assertNotNull(url);
        assertTrue(url.contains("wx_test_appid"));
        assertTrue(url.contains("open.weixin.qq.com"));
    }

    @Test
    @DisplayName("微信登录 - 已绑定用户")
    void wechatLogin_ExistingUser() {
        User wechatUser = User.builder()
                .id(1L)
                .username("wx_user")
                .wechatOpenid("mock_openid_test-code")
                .nickname("微信用户")
                .status(1)
                .build();

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(wechatUser);
        when(jwtTokenUtil.generateToken(anyLong(), anyString())).thenReturn("test-token");

        LoginResponse response = authService.wechatLogin("test-code");

        assertNotNull(response);
        assertEquals("test-token", response.getToken());
    }

    @Test
    @DisplayName("微信登录 - 新用户自动注册")
    void wechatLogin_NewUser() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return 1;
        });
        when(jwtTokenUtil.generateToken(anyLong(), anyString())).thenReturn("test-token");

        LoginResponse response = authService.wechatLogin("test-code");

        assertNotNull(response);
        assertEquals("test-token", response.getToken());
        verify(userMapper, times(1)).insert(any(User.class));
    }

    @Test
    @DisplayName("微信登录 - 账号已禁用")
    void wechatLogin_AccountDisabled() {
        testUser.setWechatOpenid("mock_openid_test-code");
        testUser.setStatus(0);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.wechatLogin("test-code");
        });

        assertEquals(400, exception.getCode());
        assertEquals("账号已被禁用", exception.getMessage());
    }
}

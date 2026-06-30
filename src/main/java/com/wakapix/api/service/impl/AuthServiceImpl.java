package com.wakapix.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wakapix.api.dto.request.LoginRequest;
import com.wakapix.api.dto.request.RegisterRequest;
import com.wakapix.api.dto.request.SmsLoginRequest;
import com.wakapix.api.dto.response.LoginResponse;
import com.wakapix.api.entity.LoginLog;
import com.wakapix.api.entity.User;
import com.wakapix.api.exception.BusinessException;
import com.wakapix.api.mapper.LoginLogMapper;
import com.wakapix.api.mapper.UserMapper;
import com.wakapix.api.service.AuthService;
import com.wakapix.api.util.JwtTokenUtil;
import com.wakapix.api.util.PasswordUtil;
import com.wakapix.api.util.SmsCodeUtil;
import com.wakapix.api.util.TokenManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;
    private final PasswordUtil passwordUtil;
    private final JwtTokenUtil jwtTokenUtil;
    private final TokenManager tokenManager;
    private final SmsCodeUtil smsCodeUtil;

    @Value("${wechat.app-id}")
    private String wechatAppId;

    @Value("${wechat.redirect-uri}")
    private String wechatRedirectUri;

    public AuthServiceImpl(UserMapper userMapper,
                           LoginLogMapper loginLogMapper,
                           PasswordUtil passwordUtil,
                           JwtTokenUtil jwtTokenUtil,
                           TokenManager tokenManager,
                           SmsCodeUtil smsCodeUtil) {
        this.userMapper = userMapper;
        this.loginLogMapper = loginLogMapper;
        this.passwordUtil = passwordUtil;
        this.jwtTokenUtil = jwtTokenUtil;
        this.tokenManager = tokenManager;
        this.smsCodeUtil = smsCodeUtil;
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())) > 0) {
            throw new BusinessException(400, "用户名已存在");
        }
        if (request.getEmail() != null && userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())) > 0) {
            throw new BusinessException(400, "邮箱已被注册");
        }
        if (request.getPhone() != null && userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone())) > 0) {
            throw new BusinessException(400, "手机号已被注册");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordUtil.encrypt(request.getPassword()))
                .email(request.getEmail())
                .phone(request.getPhone())
                .nickname(request.getUsername())
                .availablePower(300)
                .status(1)
                .build();

        userMapper.insert(user);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException(400, "账号或密码错误");
        }

        if (!passwordUtil.verify(request.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "账号或密码错误");
        }

        if (user.getStatus() != 1) {
            throw new BusinessException(400, "账号已被禁用");
        }

        return createLoginResponse(user, "ACCOUNT");
    }

    @Override
    @Transactional
    public LoginResponse smsLogin(SmsLoginRequest request) {
        if (!smsCodeUtil.verifyCode(request.getPhone(), request.getCode())) {
            throw new BusinessException(400, "验证码错误或已过期");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));

        if (user == null) {
            user = User.builder()
                    .username("user_" + System.currentTimeMillis())
                    .phone(request.getPhone())
                    .nickname("用户" + request.getPhone().substring(7))
                    .availablePower(300)
                    .status(1)
                    .build();
            userMapper.insert(user);
        }

        if (user.getStatus() != 1) {
            throw new BusinessException(400, "账号已被禁用");
        }

        return createLoginResponse(user, "SMS");
    }

    @Override
    @Transactional
    public void logout(Long userId) {
        tokenManager.removeToken(userId);
    }

    @Override
    public void sendSmsCode(String phone) {
        smsCodeUtil.sendCode(phone);
    }

    @Override
    @Transactional
    public LoginResponse wechatLogin(String code) {
        String openid = mockWechatAuth(code);

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getWechatOpenid, openid));

        if (user == null) {
            user = User.builder()
                    .username("wx_" + UUID.randomUUID().toString().substring(0, 8))
                    .wechatOpenid(openid)
                    .nickname("微信用户")
                    .availablePower(300)
                    .status(1)
                    .build();
            userMapper.insert(user);
        }

        if (user.getStatus() != 1) {
            throw new BusinessException(400, "账号已被禁用");
        }

        return createLoginResponse(user, "WECHAT");
    }

    @Override
    public String getWechatQrCodeUrl() {
        return "https://open.weixin.qq.com/connect/qrconnect?appid=" + wechatAppId +
                "&redirect_uri=" + wechatRedirectUri +
                "&response_type=code&scope=snsapi_login&state=STATE#wechat_redirect";
    }

    private LoginResponse createLoginResponse(User user, String loginType) {
        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername());
        tokenManager.saveToken(user.getId(), token);

        LoginLog loginLog = LoginLog.builder()
                .userId(user.getId())
                .loginType(loginType)
                .build();
        loginLogMapper.insert(loginLog);

        return LoginResponse.builder()
                .token(token)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .avatar(user.getAvatar())
                        .phone(user.getPhone())
                        .email(user.getEmail())
                        .build())
                .build();
    }

    private String mockWechatAuth(String code) {
        return "mock_openid_" + code;
    }
}

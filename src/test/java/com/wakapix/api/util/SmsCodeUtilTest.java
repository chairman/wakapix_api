package com.wakapix.api.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SmsCodeUtilTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private SmsCodeUtil smsCodeUtil;

    @BeforeEach
    void setUp() {
        smsCodeUtil = new SmsCodeUtil(redisTemplate);
        ReflectionTestUtils.setField(smsCodeUtil, "expireTime", 300000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("生成验证码 - 6位数字")
    void generateCode_Success() {
        String code = smsCodeUtil.generateCode();

        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    @DisplayName("发送验证码 - 成功")
    void sendCode_Success() {
        doNothing().when(valueOperations).set(anyString(), any(), anyLong(), any());

        smsCodeUtil.sendCode("13800138000");

        verify(valueOperations, times(1)).set(anyString(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("验证验证码 - 正确")
    void verifyCode_Correct() {
        String phone = "13800138000";
        String code = "123456";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(code);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        boolean result = smsCodeUtil.verifyCode(phone, code);

        assertTrue(result);
        verify(redisTemplate, times(1)).delete(anyString());
    }

    @Test
    @DisplayName("验证验证码 - 错误")
    void verifyCode_Wrong() {
        String phone = "13800138000";
        String code = "123456";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("654321");

        boolean result = smsCodeUtil.verifyCode(phone, code);

        assertFalse(result);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("验证验证码 - 已过期")
    void verifyCode_Expired() {
        String phone = "13800138000";
        String code = "123456";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        boolean result = smsCodeUtil.verifyCode(phone, code);

        assertFalse(result);
    }

    @Test
    @DisplayName("删除验证码 - 成功")
    void deleteCode_Success() {
        when(redisTemplate.delete(anyString())).thenReturn(true);

        smsCodeUtil.deleteCode("13800138000");

        verify(redisTemplate, times(1)).delete(anyString());
    }
}
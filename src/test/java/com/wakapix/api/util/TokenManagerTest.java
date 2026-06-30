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
class TokenManagerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private TokenManager tokenManager;

    @BeforeEach
    void setUp() {
        tokenManager = new TokenManager(redisTemplate);
        ReflectionTestUtils.setField(tokenManager, "expiration", 86400000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("保存Token - 成功")
    void saveToken_Success() {
        doNothing().when(valueOperations).set(anyString(), any(), anyLong(), any());

        tokenManager.saveToken(1L, "test-token");

        verify(valueOperations, times(1)).set(anyString(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("获取Token - 存在")
    void getToken_Exists() {
        when(valueOperations.get(anyString())).thenReturn("test-token");

        String token = tokenManager.getToken(1L);

        assertEquals("test-token", token);
    }

    @Test
    @DisplayName("获取Token - 不存在")
    void getToken_NotExists() {
        when(valueOperations.get(anyString())).thenReturn(null);

        String token = tokenManager.getToken(1L);

        assertNull(token);
    }

    @Test
    @DisplayName("删除Token - 成功")
    void removeToken_Success() {
        when(redisTemplate.delete(anyString())).thenReturn(true);

        tokenManager.removeToken(1L);

        verify(redisTemplate, times(1)).delete(anyString());
    }

    @Test
    @DisplayName("验证Token - 有效")
    void isTokenValid_Valid() {
        when(valueOperations.get(anyString())).thenReturn("test-token");

        boolean result = tokenManager.isTokenValid(1L, "test-token");

        assertTrue(result);
    }

    @Test
    @DisplayName("验证Token - 无效")
    void isTokenValid_Invalid() {
        when(valueOperations.get(anyString())).thenReturn("different-token");

        boolean result = tokenManager.isTokenValid(1L, "test-token");

        assertFalse(result);
    }

    @Test
    @DisplayName("验证Token - Token不存在")
    void isTokenValid_NotExists() {
        when(valueOperations.get(anyString())).thenReturn(null);

        boolean result = tokenManager.isTokenValid(1L, "test-token");

        assertFalse(result);
    }
}
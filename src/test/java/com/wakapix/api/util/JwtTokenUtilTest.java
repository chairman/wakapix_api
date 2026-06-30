package com.wakapix.api.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenUtilTest {

    private JwtTokenUtil jwtTokenUtil;

    @BeforeEach
    void setUp() {
        jwtTokenUtil = new JwtTokenUtil();
        ReflectionTestUtils.setField(jwtTokenUtil, "secret", "wakapix_secret_key_2024_padding_to_32bytes_minimum");
        ReflectionTestUtils.setField(jwtTokenUtil, "expiration", 86400000L);
    }

    @Test
    @DisplayName("生成Token - 成功")
    void generateToken_Success() {
        String token = jwtTokenUtil.generateToken(1L, "testuser");

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    @DisplayName("生成Token - 自定义过期时间")
    void generateToken_CustomExpiration() {
        String token = jwtTokenUtil.generateToken(1L, "testuser", 3600000L);

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    @DisplayName("从Token获取用户ID")
    void getUserIdFromToken_Success() {
        String token = jwtTokenUtil.generateToken(1L, "testuser");

        Long userId = jwtTokenUtil.getUserIdFromToken(token);

        assertEquals(1L, userId);
    }

    @Test
    @DisplayName("从Token获取用户名")
    void getUsernameFromToken_Success() {
        String token = jwtTokenUtil.generateToken(1L, "testuser");

        String username = jwtTokenUtil.getUsernameFromToken(token);

        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("验证Token - 有效")
    void validateToken_Valid() {
        String token = jwtTokenUtil.generateToken(1L, "testuser");

        boolean isValid = jwtTokenUtil.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("验证Token - 无效Token")
    void validateToken_Invalid() {
        boolean isValid = jwtTokenUtil.validateToken("invalid-token");

        assertFalse(isValid);
    }

    @Test
    @DisplayName("验证Token - 空Token")
    void validateToken_Empty() {
        boolean isValid = jwtTokenUtil.validateToken("");

        assertFalse(isValid);
    }

    @Test
    @DisplayName("获取过期时间")
    void getExpirationDateFromToken_Success() {
        String token = jwtTokenUtil.generateToken(1L, "testuser");

        assertNotNull(jwtTokenUtil.getExpirationDateFromToken(token));
    }
}
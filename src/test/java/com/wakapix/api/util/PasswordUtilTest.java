package com.wakapix.api.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    private final PasswordUtil passwordUtil = new PasswordUtil();

    @Test
    @DisplayName("密码加密 - 成功")
    void encrypt_Success() {
        String password = "password123";
        String encrypted = passwordUtil.encrypt(password);

        assertNotNull(encrypted);
        assertTrue(encrypted.length() > 0);
        assertNotEquals(password, encrypted);
    }

    @Test
    @DisplayName("密码验证 - 正确密码")
    void verify_CorrectPassword() {
        String password = "password123";
        String encrypted = passwordUtil.encrypt(password);

        boolean result = passwordUtil.verify(password, encrypted);

        assertTrue(result);
    }

    @Test
    @DisplayName("密码验证 - 错误密码")
    void verify_WrongPassword() {
        String password = "password123";
        String encrypted = passwordUtil.encrypt(password);

        boolean result = passwordUtil.verify("wrongpassword", encrypted);

        assertFalse(result);
    }

    @Test
    @DisplayName("密码加密 - 相同密码生成不同哈希(BCrypt)")
    void encrypt_DifferentHashSamePassword() {
        String password = "password123";
        String encrypted1 = passwordUtil.encrypt(password);
        String encrypted2 = passwordUtil.encrypt(password);

        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    @DisplayName("密码加密 - 空密码")
    void encrypt_EmptyPassword() {
        String encrypted = passwordUtil.encrypt("");

        assertNotNull(encrypted);
    }
}
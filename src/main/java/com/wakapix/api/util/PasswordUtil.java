package com.wakapix.api.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtil {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String encrypt(String password) {
        return encoder.encode(password);
    }

    public boolean verify(String password, String encryptedPassword) {
        return encoder.matches(password, encryptedPassword);
    }
}

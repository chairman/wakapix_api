package com.wakapix.api.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
public class SmsCodeUtil {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Random random = new Random();

    @Value("${sms.expire-time}")
    private Long expireTime;

    public SmsCodeUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateCode() {
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    public void sendCode(String phone) {
        String code = generateCode();
        String key = "sms:code:" + phone;
        redisTemplate.opsForValue().set(key, code, expireTime, TimeUnit.MILLISECONDS);
    }

    public boolean verifyCode(String phone, String code) {
        String key = "sms:code:" + phone;
        Object storedCode = redisTemplate.opsForValue().get(key);
        if (storedCode == null) {
            return false;
        }
        boolean result = storedCode.toString().equals(code);
        if (result) {
            redisTemplate.delete(key);
        }
        return result;
    }

    public void deleteCode(String phone) {
        String key = "sms:code:" + phone;
        redisTemplate.delete(key);
    }
}

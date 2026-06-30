package com.wakapix.api.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TokenManager {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.expiration}")
    private Long expiration;

    public TokenManager(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveToken(Long userId, String token) {
        String key = "token:user:" + userId;
        redisTemplate.opsForValue().set(key, token, expiration, TimeUnit.MILLISECONDS);
    }

    public String getToken(Long userId) {
        String key = "token:user:" + userId;
        Object token = redisTemplate.opsForValue().get(key);
        return token != null ? token.toString() : null;
    }

    public void removeToken(Long userId) {
        String key = "token:user:" + userId;
        redisTemplate.delete(key);
    }

    public boolean isTokenValid(Long userId, String token) {
        String storedToken = getToken(userId);
        return token != null && token.equals(storedToken);
    }
}

package com.hospital.wechat.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易的后台 Token 服务（演示用途）。
 * - 登录成功后生成随机 token
 * - token 存内存并设置过期时间
 */
@Service
public class AdminTokenService {

    @Value("${admin.token.ttl-ms:86400000}") // 1天
    private long ttlMs;

    private final ConcurrentHashMap<String, Long> tokenExpiry = new ConcurrentHashMap<>();

    public String createToken(String username) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenExpiry.put(token, System.currentTimeMillis() + ttlMs);
        return token;
    }

    public boolean verify(String token) {
        if (!StringUtils.hasText(token)) return false;
        Long expiry = tokenExpiry.get(token);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            tokenExpiry.remove(token);
            return false;
        }
        return true;
    }

    public void revoke(String token) {
        if (!StringUtils.hasText(token)) return;
        tokenExpiry.remove(token);
    }
}


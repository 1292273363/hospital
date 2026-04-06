package com.hospital.wechat.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 演示用的密码哈希器（SHA-256）。
 * 注意：真实生产建议使用 BCrypt/SCrypt/Argon2。
 */
@Component
public class AdminPasswordHasher {

    @Value("${admin.password-salt:hospital-admin-demo-salt}")
    private String salt;

    public String hash(String rawPassword) {
        if (rawPassword == null) return null;
        String input = salt + rawPassword;
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 一般必定存在
            throw new IllegalStateException("SHA-256 not available", e);
        }
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return toHex(digest);
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}


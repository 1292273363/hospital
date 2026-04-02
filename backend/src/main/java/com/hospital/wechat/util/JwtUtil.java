package com.hospital.wechat.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public enum Role {
        PATIENT,
        DOCTOR
    }

    /**
     * 生成token
     */
    public String generateToken(Long userId, String openid) {
        // 兼容旧版微信登录 token（历史代码仍可能调用）
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("openid", openid);
        claims.put("role", "WX_USER");

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRoleToken(Role role, Long id) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role.name());
        if (role == Role.PATIENT) {
            claims.put("patientId", id);
        } else if (role == Role.DOCTOR) {
            claims.put("doctorId", id);
        }
        return Jwts.builder()
                .claims(claims)
                .subject(role.name() + ":" + id)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析token
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        Object role = claims.get("role");
        return role == null ? null : String.valueOf(role);
    }

    public Long getPatientIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("patientId", Long.class);
    }

    public Long getDoctorIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("doctorId", Long.class);
    }

    /**
     * 验证token是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}


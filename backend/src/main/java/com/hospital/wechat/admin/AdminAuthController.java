package com.hospital.wechat.admin;

import com.hospital.wechat.admin.dto.AdminLoginRequest;
import com.hospital.wechat.admin.dto.AdminLoginResponse;
import com.hospital.wechat.dto.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.beans.factory.annotation.Value;
import java.util.Map;

import static org.springframework.http.ResponseEntity.ok;

/**
 * 管理后台登录：POST /admin/api/login
 */
@RestController
public class AdminAuthController {

    private final JdbcTemplate jdbcTemplate;
    private final AdminPasswordHasher passwordHasher;
    private final AdminTokenService tokenService;

    @Value("${admin.username:admin}")
    private String defaultUsername;

    @Value("${admin.default-password:123456}")
    private String defaultPassword;

    public AdminAuthController(
            JdbcTemplate jdbcTemplate,
            AdminPasswordHasher passwordHasher,
            AdminTokenService tokenService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
    }

    @PostMapping("/admin/api/login")
    public ResponseEntity<?> login(@RequestBody AdminLoginRequest body) {
        String username = body == null ? null : body.getUsername();
        String password = body == null ? null : body.getPassword();
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "username/password不能为空"));
        }

        // 允许：如果数据库没有 admin_user 表，就退回默认账号（配合 schema.sql 初始化）
        String normalizedUsername = username.trim();
        String expectedHash = null;

        try {
            expectedHash = jdbcTemplate.queryForObject(
                    "SELECT password_hash FROM admin_user WHERE username = ? LIMIT 1",
                    new Object[]{normalizedUsername},
                    String.class
            );
        } catch (Exception ignored) {
            // 表不存在或其他异常：走默认账号策略
        }

        if (expectedHash == null) {
            if (!normalizedUsername.equals(defaultUsername)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "账号或密码错误"));
            }
            expectedHash = passwordHasher.hash(defaultPassword);
        }

        String actualHash = passwordHasher.hash(password);
        if (expectedHash == null || !expectedHash.equals(actualHash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "账号或密码错误"));
        }

        String token = tokenService.createToken(normalizedUsername);
        AdminLoginResponse resp = AdminLoginResponse.builder().token(token).username(normalizedUsername).build();
        return ok(Result.success(resp));
    }
}


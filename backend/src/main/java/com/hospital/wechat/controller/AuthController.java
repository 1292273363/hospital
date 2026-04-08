package com.hospital.wechat.controller;

import com.hospital.wechat.dto.AuthLoginRequest;
import com.hospital.wechat.dto.AuthSendCodeRequest;
import com.hospital.wechat.dto.LoginResponse;
import com.hospital.wechat.dto.Result;
import com.hospital.wechat.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-code")
    public Result<Map<String, String>> sendCode(@RequestBody AuthSendCodeRequest request) {
        try {
            String code = authService.sendCode(request);
            // smsCode 与 Result 顶层 code 区分，避免联调时误读；保留 code 兼容旧前端
            return Result.success(Map.of(
                    "smsCode", code,
                    "code", code));
        } catch (Exception e) {
            log.warn("send-code failed", e);
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody AuthLoginRequest request) {
        try {
            return Result.success(authService.login(request));
        } catch (Exception e) {
            log.warn("auth login failed", e);
            return Result.fail(e.getMessage());
        }
    }
}


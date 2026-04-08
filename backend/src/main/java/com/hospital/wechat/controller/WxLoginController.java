package com.hospital.wechat.controller;

import com.hospital.wechat.dto.LoginResponse;
import com.hospital.wechat.dto.Result;
import com.hospital.wechat.dto.WxLoginRequest;
import com.hospital.wechat.service.WxLoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 微信登录接口
 */
@Slf4j
@RestController
@RequestMapping("/api/wx")
@RequiredArgsConstructor
public class WxLoginController {

    private final WxLoginService wxLoginService;

    /**
     * 微信一键登录
     * POST /api/wx/login
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody WxLoginRequest request) {
        log.info("微信登录请求, code: {}", request.getCode());
        if (request.getCode() == null || request.getCode().isBlank()) {
            return Result.fail("code不能为空");
        }
        if (request.getPhoneCode() == null || request.getPhoneCode().isBlank()) {
            return Result.fail("phoneCode不能为空");
        }
        try {
            LoginResponse response = wxLoginService.login(request);
            return Result.success(response);
        } catch (Exception e) {
            log.error("登录失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("服务正常运行");
    }
}


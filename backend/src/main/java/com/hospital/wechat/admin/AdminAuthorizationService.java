package com.hospital.wechat.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 管理后台鉴权：校验请求头 {@code X-Admin-Token}。
 */
@Service
public class AdminAuthorizationService {

    @Value("${admin.token:}") // 兼容旧配置
    private String configuredToken;

    private final AdminTokenService tokenService;

    public AdminAuthorizationService(AdminTokenService tokenService) {
        this.tokenService = tokenService;
    }

    public boolean authorized(HttpServletRequest request) {
        String headerToken = request.getHeader("X-Admin-Token");
        if (!StringUtils.hasText(headerToken)) return false;

        // 兼容：配置了固定 token 时直接放行
        if (StringUtils.hasText(configuredToken) && configuredToken.equals(headerToken)) {
            return true;
        }

        // 登录后签发的 token
        return tokenService.verify(headerToken);
    }
}


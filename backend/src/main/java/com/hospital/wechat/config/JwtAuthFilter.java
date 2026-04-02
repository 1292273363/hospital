package com.hospital.wechat.config;

import com.hospital.wechat.dto.Result;
import com.hospital.wechat.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 请求过滤器
 * 对需要登录的接口进行token验证
 */
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    /** 白名单路径（不需要token） */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/",
            "/api/wx/login",
            "/api/wx/health"
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 白名单放行
        if (WHITE_LIST.stream().anyMatch(uri::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, "请先登录");
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            writeUnauthorized(response, "登录已过期，请重新登录");
            return;
        }

        // 将角色/ID写入request属性，供后续使用
        String role = jwtUtil.getRoleFromToken(token);
        request.setAttribute("role", role);

        Long patientId = jwtUtil.getPatientIdFromToken(token);
        if (patientId != null) {
            request.setAttribute("patientId", patientId);
        }
        Long doctorId = jwtUtil.getDoctorIdFromToken(token);
        if (doctorId != null) {
            request.setAttribute("doctorId", doctorId);
        }

        // 兼容旧微信登录：保留 userId 属性（历史接口还在用）
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId != null) {
            request.setAttribute("userId", userId);
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        Result<?> result = Result.fail(401, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}


package com.hospital.wechat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.wechat.dto.LoginResponse;
import com.hospital.wechat.dto.WxLoginRequest;
import com.hospital.wechat.entity.WxUser;
import com.hospital.wechat.entity.WxUserMapper;
import com.hospital.wechat.service.WxLoginService;
import com.hospital.wechat.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WxLoginServiceImpl implements WxLoginService {

    private final WxUserMapper wxUserMapper;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${wechat.miniprogram.appid}")
    private String appid;

    @Value("${wechat.miniprogram.secret}")
    private String secret;

    @Value("${wechat.miniprogram.code2session-url}")
    private String code2sessionUrl;

    @Value("${wechat.miniprogram.phone-url}")
    private String phoneUrl;

    @Value("${wechat.miniprogram.mock-login-enabled:false}")
    private boolean mockLoginEnabled;

    @Override
    public LoginResponse login(WxLoginRequest request) {
        // 1. 用code换取openid（本地调试可走模拟登录）
        String openid = mockLoginEnabled ? buildMockOpenid(request.getCode()) : getOpenidByCode(request.getCode());
        log.info("微信登录 openid: {}", openid);

        // 2. 查询或创建用户
        WxUser user = getOrCreateUser(openid);

        // 3. 若有phoneCode，获取手机号并绑定（模拟登录下直接写测试手机号）
        if (request.getPhoneCode() != null && !request.getPhoneCode().isBlank()) {
            String phone = mockLoginEnabled ? "13800000000" : getPhoneByCode(request.getPhoneCode());
            if (phone != null && !phone.equals(user.getPhone())) {
                user.setPhone(phone);
                wxUserMapper.updateById(user);
                log.info("用户 {} 绑定手机号: {}", openid, phone);
            }
        }

        // 4. 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        wxUserMapper.updateById(user);

        // 5. 生成JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getOpenid());

        // 6. 构建响应
        return LoginResponse.builder()
                .token(token)
                .userInfo(LoginResponse.UserInfoVO.builder()
                        .id(user.getId())
                        .openid(user.getOpenid())
                        .nickName(user.getNickName())
                        .avatarUrl(user.getAvatarUrl())
                        .phone(user.getPhone())
                        .build())
                .build();
    }

    /**
     * 本地调试模拟 openid（不调用微信接口）
     */
    private String buildMockOpenid(String code) {
        // 本地调试时固定为同一账号，避免每次登录都生成新用户导致档案丢失
        return "mock_openid_demo_user";
    }

    /**
     * 通过code换取openid
     */
    private String getOpenidByCode(String code) {
        String url = UriComponentsBuilder.fromHttpUrl(code2sessionUrl)
                .queryParam("appid", appid)
                .queryParam("secret", secret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .toUriString();

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(response);
            if (node.has("errcode") && node.get("errcode").asInt() != 0) {
                throw new RuntimeException("微信code2session失败: " + node.get("errmsg").asText());
            }
            return node.get("openid").asText();
        } catch (Exception e) {
            log.error("获取openid失败", e);
            throw new RuntimeException("微信登录失败，请重试");
        }
    }

    /**
     * 查询用户，不存在则创建
     */
    private WxUser getOrCreateUser(String openid) {
        WxUser user = wxUserMapper.selectOne(
                new LambdaQueryWrapper<WxUser>().eq(WxUser::getOpenid, openid)
        );
        if (user == null) {
            user = new WxUser();
            user.setOpenid(openid);
            user.setNickName("微信用户");
            user.setStatus(0);
            wxUserMapper.insert(user);
            log.info("新用户注册, openid: {}", openid);
        }
        return user;
    }

    /**
     * 通过phoneCode获取手机号（微信新版API）
     */
    private String getPhoneByCode(String phoneCode) {
        // 先获取access_token（简化版，生产环境应缓存access_token）
        String accessToken = getAccessToken();
        if (accessToken == null) return null;

        String url = phoneUrl + "?access_token=" + accessToken;
        try {
            // 使用 ObjectMapper 构建 JSON，避免字符串拼接
            java.util.Map<String, String> requestBody = new java.util.HashMap<>();
            requestBody.put("code", phoneCode);
            String body = objectMapper.writeValueAsString(requestBody);
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(url, entity, String.class);
            JsonNode node = objectMapper.readTree(response);
            if (node.has("phone_info")) {
                return node.get("phone_info").get("phoneNumber").asText();
            }
        } catch (Exception e) {
            log.warn("获取手机号失败", e);
        }
        return null;
    }

    /**
     * 获取微信access_token（生产环境建议缓存，有效期2小时）
     */
    private String getAccessToken() {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.weixin.qq.com/cgi-bin/token")
                .queryParam("grant_type", "client_credential")
                .queryParam("appid", appid)
                .queryParam("secret", secret)
                .toUriString();
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(response);
            if (node.has("access_token")) {
                return node.get("access_token").asText();
            }
        } catch (Exception e) {
            log.error("获取access_token失败", e);
        }
        return null;
    }
}


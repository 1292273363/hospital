package com.hospital.wechat.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 登录响应DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    /** JWT token */
    private String token;

    /** 用户信息 */
    private UserInfoVO userInfo;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInfoVO {
        private Long id;
        private String openid;
        private String nickName;
        private String avatarUrl;
        private String phone;
    }
}


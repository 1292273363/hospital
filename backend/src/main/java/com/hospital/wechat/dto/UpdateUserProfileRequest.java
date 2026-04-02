package com.hospital.wechat.dto;

import lombok.Data;

@Data
public class UpdateUserProfileRequest {
    private String nickName;
    private String avatarUrl;
    private String phone;
}

package com.hospital.wechat.admin.dto;

import lombok.Data;

@Data
public class AdminPatientUpdateRequest {
    private String nickName;
    private String avatarUrl;
    private Integer status; // 0/1
}


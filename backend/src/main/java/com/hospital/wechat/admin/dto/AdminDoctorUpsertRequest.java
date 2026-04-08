package com.hospital.wechat.admin.dto;

import lombok.Data;

@Data
public class AdminDoctorUpsertRequest {
    private String phone;
    private String doctorName;
    private String doctorLevel;
    private Integer status; // 0/1
    private String password; // 可选：用于设置 doctor.password_hash
}


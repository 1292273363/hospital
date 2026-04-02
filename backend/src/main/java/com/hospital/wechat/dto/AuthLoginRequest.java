package com.hospital.wechat.dto;

import lombok.Data;

@Data
public class AuthLoginRequest {
    /** patient / doctor */
    private String role;
    private String phone;
    private String code;
}


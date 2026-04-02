package com.hospital.wechat.dto;

import lombok.Data;

@Data
public class AuthSendCodeRequest {
    /** patient / doctor */
    private String role;
    private String phone;
}


package com.hospital.wechat.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

@Data
public class AuthLoginRequest {
    /** patient / doctor */
    private String role;
    private String phone;

    @JsonDeserialize(using = FlexibleCodeDeserializer.class)
    private String code;
}


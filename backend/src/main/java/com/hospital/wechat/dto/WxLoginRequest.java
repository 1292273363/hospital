package com.hospital.wechat.dto;

import lombok.Data;

/**
 * 微信登录请求DTO
 */
@Data
public class WxLoginRequest {

    /** wx.login() 获取的code */
    private String code;

    /** 手机号授权code（新版微信API）*/
    private String phoneCode;
}


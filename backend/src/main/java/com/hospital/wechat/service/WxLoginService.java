package com.hospital.wechat.service;

import com.hospital.wechat.dto.LoginResponse;
import com.hospital.wechat.dto.WxLoginRequest;

/**
 * 微信登录 Service 接口
 */
public interface WxLoginService {

    /**
     * 微信登录
     * @param request 登录请求（包含code，可选phoneCode）
     * @return 登录响应（token + 用户信息）
     */
    LoginResponse login(WxLoginRequest request);
}


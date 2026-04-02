package com.hospital.wechat.service;

import com.hospital.wechat.dto.AuthLoginRequest;
import com.hospital.wechat.dto.AuthSendCodeRequest;
import com.hospital.wechat.dto.LoginResponse;

public interface AuthService {
    /** 返回开发期验证码（生产环境应通过短信服务发送，不返回给前端） */
    String sendCode(AuthSendCodeRequest request);

    LoginResponse login(AuthLoginRequest request);
}


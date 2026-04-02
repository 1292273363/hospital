package com.hospital.wechat.dto;

import lombok.Data;

@Data
public class DoctorReplyRequest {
    private Long consultationId;
    private String reply;
}


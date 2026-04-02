package com.hospital.wechat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OnlineConsultationResponse {
    private Long id;
    private Long visitRecordId;
    private String scarImageUrl;
    private String patientMessage;
    private String doctorReply;
    private String status;
    private String createTime;
}

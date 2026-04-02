package com.hospital.wechat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VisitRecordOptionResponse {
    private Long id;
    private String doctorName;
    private String doctorLevel;
    private String visitTime;
}

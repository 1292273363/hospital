package com.hospital.wechat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VisitReportResponse {
    private Long visitId;
    private String doctorName;
    private String doctorLevel;
    private String diagnosisReport;
    private String visitTime;
}

package com.hospital.wechat.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PatientVisitLookupResponse {
    private Long patientRecordId;
    private String patientName;
    private String phone;
    private String diseaseType;
    private List<VisitItem> visits;

    @Data
    @Builder
    public static class VisitItem {
        private Long visitId;
        private String doctorName;
        private String doctorLevel;
        private String visitTime;
        private String diagnosisReport;
        private Integer consultationCount;
    }
}

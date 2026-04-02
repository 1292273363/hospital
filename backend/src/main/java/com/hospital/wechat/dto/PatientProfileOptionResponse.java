package com.hospital.wechat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PatientProfileOptionResponse {
    private Long id;
    private String patientName;
    private String diseaseType;
    private String phone;
}

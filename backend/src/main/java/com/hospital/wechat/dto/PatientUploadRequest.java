package com.hospital.wechat.dto;

import lombok.Data;

@Data
public class PatientUploadRequest {
    private String patientName;
    private String gender;
    private Integer age;
    private String phone;
    private String diseaseType;
    private String idCard;
    private String remark;
}

package com.hospital.wechat.controller;

import com.hospital.wechat.dto.PatientUploadRequest;
import com.hospital.wechat.dto.PatientProfileOptionResponse;
import com.hospital.wechat.dto.PatientVisitLookupResponse;
import com.hospital.wechat.dto.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hospital.wechat.entity.OnlineConsultation;
import com.hospital.wechat.entity.OnlineConsultationMapper;
import com.hospital.wechat.entity.PatientRecord;
import com.hospital.wechat.entity.PatientRecordMapper;
import com.hospital.wechat.entity.VisitRecord;
import com.hospital.wechat.entity.VisitRecordMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientRecordMapper patientRecordMapper;
    private final VisitRecordMapper visitRecordMapper;
    private final OnlineConsultationMapper onlineConsultationMapper;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping("/list")
    public Result<List<PatientProfileOptionResponse>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        List<PatientRecord> list = patientRecordMapper.selectList(new LambdaQueryWrapper<PatientRecord>()
                .eq(PatientRecord::getUploaderUserId, userId)
                .orderByDesc(PatientRecord::getCreateTime));
        List<PatientProfileOptionResponse> response = list.stream().map(item ->
                PatientProfileOptionResponse.builder()
                        .id(item.getId())
                        .patientName(item.getPatientName())
                        .diseaseType(item.getDiseaseType())
                        .phone(item.getPhone())
                        .build()
        ).toList();
        return Result.success(response);
    }

    @GetMapping("/{id}")
    public Result<PatientRecord> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        PatientRecord record = getOwnedPatient(id, userId);
        if (record == null) {
            return Result.fail("患者档案不存在");
        }
        return Result.success(record);
    }

    @GetMapping("/lookup-by-phone")
    public Result<PatientVisitLookupResponse> lookupByPhone(@RequestParam String phone, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        if (isBlank(phone)) {
            return Result.fail("手机号不能为空");
        }
        PatientRecord patient = patientRecordMapper.selectOne(new LambdaQueryWrapper<PatientRecord>()
                .eq(PatientRecord::getUploaderUserId, userId)
                .eq(PatientRecord::getPhone, phone.trim())
                .orderByDesc(PatientRecord::getCreateTime)
                .last("LIMIT 1"));
        if (patient == null) {
            return Result.fail("未找到该手机号对应的患者档案");
        }

        List<VisitRecord> visits = visitRecordMapper.selectList(new LambdaQueryWrapper<VisitRecord>()
                .eq(VisitRecord::getPatientRecordId, patient.getId())
                .orderByDesc(VisitRecord::getVisitTime));
        List<PatientVisitLookupResponse.VisitItem> visitItems = visits.stream().map(item -> {
            Long count = onlineConsultationMapper.selectCount(new LambdaQueryWrapper<OnlineConsultation>()
                    .eq(OnlineConsultation::getVisitRecordId, item.getId()));
            return PatientVisitLookupResponse.VisitItem.builder()
                    .visitId(item.getId())
                    .doctorName(item.getDoctorName())
                    .doctorLevel(item.getDoctorLevel())
                    .visitTime(item.getVisitTime() == null ? "" : item.getVisitTime().format(DATE_TIME))
                    .diagnosisReport(item.getDiagnosisReport())
                    .consultationCount(count == null ? 0 : count.intValue())
                    .build();
        }).toList();

        return Result.success(PatientVisitLookupResponse.builder()
                .patientRecordId(patient.getId())
                .patientName(patient.getPatientName())
                .phone(patient.getPhone())
                .diseaseType(patient.getDiseaseType())
                .visits(visitItems)
                .build());
    }

    @PostMapping("/upload")
    public Result<Long> uploadPatient(@RequestBody PatientUploadRequest uploadRequest,
                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        if (isBlank(uploadRequest.getPatientName()) ||
                isBlank(uploadRequest.getPhone()) ||
                isBlank(uploadRequest.getDiseaseType()) ||
                uploadRequest.getAge() == null) {
            return Result.fail("请填写完整必填信息");
        }

        PatientRecord record = new PatientRecord();
        record.setUploaderUserId(userId);
        record.setPatientName(uploadRequest.getPatientName().trim());
        record.setGender(defaultGender(uploadRequest.getGender()));
        record.setAge(uploadRequest.getAge());
        record.setPhone(uploadRequest.getPhone().trim());
        record.setDiseaseType(uploadRequest.getDiseaseType().trim());
        record.setIdCard(trimOrNull(uploadRequest.getIdCard()));
        record.setRemark(trimOrNull(uploadRequest.getRemark()));
        patientRecordMapper.insert(record);
        return Result.success(record.getId());
    }

    @PutMapping("/{id}")
    public Result<Long> updatePatient(@PathVariable Long id,
                                      @RequestBody PatientUploadRequest updateRequest,
                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        if (isBlank(updateRequest.getPatientName()) ||
                isBlank(updateRequest.getPhone()) ||
                isBlank(updateRequest.getDiseaseType()) ||
                updateRequest.getAge() == null) {
            return Result.fail("请填写完整必填信息");
        }
        PatientRecord record = getOwnedPatient(id, userId);
        if (record == null) {
            return Result.fail("患者档案不存在");
        }

        record.setPatientName(updateRequest.getPatientName().trim());
        record.setGender(defaultGender(updateRequest.getGender()));
        record.setAge(updateRequest.getAge());
        record.setPhone(updateRequest.getPhone().trim());
        record.setDiseaseType(updateRequest.getDiseaseType().trim());
        record.setIdCard(trimOrNull(updateRequest.getIdCard()));
        record.setRemark(trimOrNull(updateRequest.getRemark()));
        patientRecordMapper.updateById(record);
        return Result.success(record.getId());
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePatient(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        PatientRecord record = getOwnedPatient(id, userId);
        if (record == null) {
            return Result.fail("患者档案不存在");
        }
        patientRecordMapper.deleteById(id);
        return Result.success();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultGender(String gender) {
        if (gender == null || gender.isBlank()) {
            return "男";
        }
        return gender.trim();
    }

    private PatientRecord getOwnedPatient(Long id, Long userId) {
        return patientRecordMapper.selectOne(new LambdaQueryWrapper<PatientRecord>()
                .eq(PatientRecord::getId, id)
                .eq(PatientRecord::getUploaderUserId, userId));
    }
}

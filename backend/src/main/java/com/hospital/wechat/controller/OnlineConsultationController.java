package com.hospital.wechat.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hospital.wechat.dto.OnlineConsultationResponse;
import com.hospital.wechat.dto.Result;
import com.hospital.wechat.dto.VisitRecordOptionResponse;
import com.hospital.wechat.dto.VisitReportResponse;
import com.hospital.wechat.entity.OnlineConsultation;
import com.hospital.wechat.entity.OnlineConsultationMapper;
import com.hospital.wechat.entity.PatientRecord;
import com.hospital.wechat.entity.PatientRecordMapper;
import com.hospital.wechat.entity.VisitRecord;
import com.hospital.wechat.entity.VisitRecordMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/consult")
@RequiredArgsConstructor
public class OnlineConsultationController {

    private final VisitRecordMapper visitRecordMapper;
    private final OnlineConsultationMapper consultationMapper;
    private final PatientRecordMapper patientRecordMapper;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping("/eligible-visits")
    public Result<List<VisitRecordOptionResponse>> eligibleVisits(@RequestParam Long patientRecordId,
                                                                  @NonNull HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        if (!isPatientBelongsToCurrentUser(patientRecordId, userId)) {
            return Result.fail("患者档案不存在或无权限");
        }
        List<VisitRecord> list = visitRecordMapper.selectList(new LambdaQueryWrapper<VisitRecord>()
                .eq(VisitRecord::getPatientRecordId, patientRecordId)
                .eq(VisitRecord::getDoctorLevel, "专家")
                .orderByDesc(VisitRecord::getVisitTime));
        List<VisitRecordOptionResponse> response = list.stream().map(item ->
                VisitRecordOptionResponse.builder()
                        .id(item.getId())
                        .doctorName(item.getDoctorName())
                        .doctorLevel(item.getDoctorLevel())
                        .visitTime(item.getVisitTime() == null ? "" : item.getVisitTime().format(DATE_TIME))
                        .build()
        ).toList();
        return Result.success(response);
    }

    @GetMapping("/history")
    public Result<List<OnlineConsultationResponse>> history(@RequestParam Long patientRecordId,
                                                            @NonNull HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        if (!isPatientBelongsToCurrentUser(patientRecordId, userId)) {
            return Result.fail("患者档案不存在或无权限");
        }
        List<VisitRecord> visitRecords = visitRecordMapper.selectList(new LambdaQueryWrapper<VisitRecord>()
                .eq(VisitRecord::getPatientRecordId, patientRecordId));
        if (visitRecords.isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        List<Long> visitIds = visitRecords.stream().map(VisitRecord::getId).collect(Collectors.toList());
        List<OnlineConsultation> consultations = consultationMapper.selectList(new LambdaQueryWrapper<OnlineConsultation>()
                .eq(OnlineConsultation::getPatientUserId, userId)
                .in(OnlineConsultation::getVisitRecordId, visitIds)
                .orderByDesc(OnlineConsultation::getCreateTime));
        List<OnlineConsultationResponse> response = consultations.stream().map(item ->
                OnlineConsultationResponse.builder()
                        .id(item.getId())
                        .visitRecordId(item.getVisitRecordId())
                        .scarImageUrl(item.getScarImageUrl())
                        .patientMessage(item.getPatientMessage())
                        .doctorReply(item.getDoctorReply())
                        .status(item.getStatus())
                        .createTime(item.getCreateTime() == null ? "" : item.getCreateTime().format(DATE_TIME))
                        .build()
        ).toList();
        return Result.success(response);
    }

    @GetMapping("/report/{visitId}")
    public Result<VisitReportResponse> report(@PathVariable Long visitId, @NonNull HttpServletRequest request) {
        VisitRecord record = validExpertVisitForCurrentUser(visitId, request);
        if (record == null) {
            return Result.fail("仅可选择本人专家看诊记录");
        }
        return Result.success(VisitReportResponse.builder()
                .visitId(record.getId())
                .doctorName(record.getDoctorName())
                .doctorLevel(record.getDoctorLevel())
                .diagnosisReport(record.getDiagnosisReport())
                .visitTime(record.getVisitTime() == null ? "" : record.getVisitTime().format(DATE_TIME))
                .build());
    }

    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<OnlineConsultationResponse> submit(@RequestParam Long visitId,
                                                     @RequestParam(required = false) String message,
                                                     @RequestParam("file") MultipartFile file,
                                                     @NonNull HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        VisitRecord record = validExpertVisitForCurrentUser(visitId, request);
        if (record == null) {
            return Result.fail("仅可选择本人专家看诊记录");
        }
        if (file == null || file.isEmpty()) {
            return Result.fail("请上传疤痕图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.fail("仅支持图片文件");
        }

        try {
            Path uploadDir = resolveUploadDir().resolve("consult");
            Files.createDirectories(uploadDir);
            String ext = getExtension(file.getOriginalFilename());
            String fileName = "consult_" + userId + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            OnlineConsultation consultation = new OnlineConsultation();
            consultation.setVisitRecordId(visitId);
            consultation.setPatientUserId(userId);
            consultation.setScarImageUrl("/upload/consult/" + fileName);
            consultation.setPatientMessage(message == null ? "" : message.trim());
            consultation.setDoctorReply("");
            consultation.setStatus("待诊断");
            consultationMapper.insert(consultation);

            return Result.success(OnlineConsultationResponse.builder()
                    .id(consultation.getId())
                    .visitRecordId(consultation.getVisitRecordId())
                    .scarImageUrl(consultation.getScarImageUrl())
                    .patientMessage(consultation.getPatientMessage())
                    .doctorReply(consultation.getDoctorReply())
                    .status(consultation.getStatus())
                    .createTime(consultation.getCreateTime() == null ? "" : consultation.getCreateTime().format(DATE_TIME))
                    .build());
        } catch (IOException e) {
            return Result.fail("提交失败，请稍后重试");
        }
    }

    private VisitRecord validExpertVisitForCurrentUser(Long visitId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return null;
        }
        VisitRecord record = visitRecordMapper.selectOne(new LambdaQueryWrapper<VisitRecord>()
                .eq(VisitRecord::getId, visitId)
                .eq(VisitRecord::getDoctorLevel, "专家"));
        if (record == null || record.getPatientRecordId() == null) {
            return null;
        }
        return isPatientBelongsToCurrentUser(record.getPatientRecordId(), userId) ? record : null;
    }

    private boolean isPatientBelongsToCurrentUser(Long patientRecordId, Long userId) {
        PatientRecord patient = patientRecordMapper.selectOne(new LambdaQueryWrapper<PatientRecord>()
                .eq(PatientRecord::getId, patientRecordId)
                .eq(PatientRecord::getUploaderUserId, userId));
        return patient != null;
    }

    private Path resolveUploadDir() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        Path root = current.getFileName() != null && "backend".equalsIgnoreCase(current.getFileName().toString())
                ? current.getParent()
                : current;
        return root.resolve("upload");
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        String ext = filename.substring(filename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        return ext.length() > 6 ? ".jpg" : ext;
    }
}

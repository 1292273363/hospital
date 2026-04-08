package com.hospital.wechat.controller;

import com.hospital.wechat.dto.Result;
import com.hospital.wechat.dto.BindDoctorRequest;
import com.hospital.wechat.dto.ChangePasswordRequest;
import com.hospital.wechat.dto.UpdateUserProfileRequest;
import com.hospital.wechat.dto.UserProfileResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hospital.wechat.entity.Doctor;
import com.hospital.wechat.entity.DoctorMapper;
import com.hospital.wechat.entity.Patient;
import com.hospital.wechat.entity.PatientMapper;
import com.hospital.wechat.entity.WxUser;
import com.hospital.wechat.entity.WxUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final WxUserMapper wxUserMapper;
    private final PatientMapper patientMapper;
    private final DoctorMapper doctorMapper;
    @Value("${auth.password-salt:hospital-auth-demo-salt}")
    private String passwordSalt;

    @GetMapping("/me")
    public Result<UserProfileResponse> getCurrentUser(@NonNull HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if ("PATIENT".equals(role)) {
            Long patientId = (Long) request.getAttribute("patientId");
            if (patientId == null) {
                return Result.fail(401, "请先登录");
            }
            Patient patient = patientMapper.selectById(patientId);
            if (patient == null) {
                return Result.fail("用户不存在");
            }
            return Result.success(UserProfileResponse.builder()
                    .id(patient.getId())
                    .openid(null)
                    .nickName(patient.getNickName() == null ? "患者" : patient.getNickName())
                    .avatarUrl(buildAvatarUrl(patient.getAvatarUrl(), request))
                    .phone(patient.getPhone())
                    .build());
        }
        if ("DOCTOR".equals(role)) {
            Long doctorId = (Long) request.getAttribute("doctorId");
            if (doctorId == null) {
                return Result.fail(401, "请先登录");
            }
            Doctor doctor = doctorMapper.selectById(doctorId);
            if (doctor == null) {
                return Result.fail("用户不存在");
            }
            return Result.success(UserProfileResponse.builder()
                    .id(doctor.getId())
                    .openid(null)
                    .nickName(doctor.getDoctorName())
                    .avatarUrl(null)
                    .phone(doctor.getPhone())
                    .build());
        }

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        WxUser user = wxUserMapper.selectById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        return Result.success(toResponse(user, request));
    }

    @PutMapping("/me")
    public Result<UserProfileResponse> updateCurrentUser(@RequestBody UpdateUserProfileRequest updateRequest,
                                                         @NonNull HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        WxUser user = wxUserMapper.selectById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        if (updateRequest.getNickName() != null && !updateRequest.getNickName().isBlank()) {
            user.setNickName(updateRequest.getNickName().trim());
        }
        if (updateRequest.getPhone() != null) {
            user.setPhone(updateRequest.getPhone().trim());
        }
        if (updateRequest.getAvatarUrl() != null) {
            user.setAvatarUrl(updateRequest.getAvatarUrl().trim());
        }

        wxUserMapper.updateById(user);
        return Result.success(toResponse(user, request));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest body,
                                       @NonNull HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"PATIENT".equals(role)) {
            return Result.fail("当前账号暂不支持修改密码");
        }
        Long patientId = (Long) request.getAttribute("patientId");
        if (patientId == null) {
            return Result.fail(401, "请先登录");
        }
        String oldPwd = body == null ? null : trimOrNull(body.getOldPassword());
        String newPwd = body == null ? null : trimOrNull(body.getNewPassword());
        if (oldPwd == null || newPwd == null) {
            return Result.fail("请输入完整密码信息");
        }
        if (newPwd.length() < 4 || newPwd.length() > 20) {
            return Result.fail("新密码长度需在4到20位之间");
        }

        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) {
            return Result.fail("用户不存在");
        }
        String currentHash = trimOrNull(patient.getPasswordHash());
        if (currentHash == null) {
            currentHash = hashPassword(right4(patient.getPhone()));
        }
        if (!Objects.equals(currentHash, hashPassword(oldPwd))) {
            return Result.fail("旧密码错误");
        }
        patient.setPasswordHash(hashPassword(newPwd));
        patientMapper.updateById(patient);
        return Result.success();
    }

    @GetMapping("/doctors")
    public Result<List<Map<String, Object>>> doctorOptions(@NonNull HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"PATIENT".equals(role)) {
            return Result.fail(401, "请使用患者账号登录");
        }
        List<Doctor> doctors = doctorMapper.selectList(new LambdaQueryWrapper<Doctor>()
                .eq(Doctor::getStatus, 0)
                .orderByAsc(Doctor::getId));
        List<Map<String, Object>> data = doctors.stream().map(d -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", d.getId());
            item.put("doctorName", d.getDoctorName() == null ? "医生" + d.getId() : d.getDoctorName());
            item.put("doctorLevel", d.getDoctorLevel() == null ? "医生" : d.getDoctorLevel());
            return item;
        }).toList();
        return Result.success(data);
    }

    @GetMapping("/binding-status")
    public Result<Map<String, Object>> bindingStatus(@NonNull HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"PATIENT".equals(role)) {
            return Result.fail(401, "请使用患者账号登录");
        }
        Long patientId = (Long) request.getAttribute("patientId");
        if (patientId == null) {
            return Result.fail(401, "请先登录");
        }
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) {
            return Result.fail("用户不存在");
        }
        Long doctorId = patient.getPrimaryDoctorId();
        if (doctorId == null) {
            return Result.success(Map.of(
                    "bound", false,
                    "doctorId", 0,
                    "doctorName", "",
                    "doctorLevel", ""
            ));
        }
        Doctor doctor = doctorMapper.selectById(doctorId);
        if (doctor == null) {
            return Result.success(Map.of(
                    "bound", false,
                    "doctorId", 0,
                    "doctorName", "",
                    "doctorLevel", ""
            ));
        }
        return Result.success(Map.of(
                "bound", true,
                "doctorId", doctor.getId(),
                "doctorName", doctor.getDoctorName() == null ? "医生" + doctor.getId() : doctor.getDoctorName(),
                "doctorLevel", doctor.getDoctorLevel() == null ? "医生" : doctor.getDoctorLevel()
        ));
    }

    @PutMapping("/bind-doctor")
    public Result<Void> bindDoctor(@RequestBody BindDoctorRequest body, @NonNull HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"PATIENT".equals(role)) {
            return Result.fail(401, "请使用患者账号登录");
        }
        Long patientId = (Long) request.getAttribute("patientId");
        if (patientId == null) {
            return Result.fail(401, "请先登录");
        }
        Long doctorId = body == null ? null : body.getDoctorId();
        if (doctorId == null) {
            return Result.fail("doctorId不能为空");
        }
        Doctor doctor = doctorMapper.selectById(doctorId);
        if (doctor == null || doctor.getStatus() == null || doctor.getStatus() != 0) {
            return Result.fail("医生不存在或不可绑定");
        }
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) {
            return Result.fail("用户不存在");
        }
        patient.setPrimaryDoctorId(doctorId);
        patientMapper.updateById(patient);
        return Result.success();
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UserProfileResponse> uploadAvatar(MultipartFile file, @NonNull HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        if (file == null || file.isEmpty()) {
            return Result.fail("请先选择图片");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.fail("仅支持图片文件");
        }

        WxUser user = wxUserMapper.selectById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        try {
            Path uploadDir = resolveUploadDir();
            Files.createDirectories(uploadDir);

            String ext = getExtension(file.getOriginalFilename());
            String fileName = "avatar_" + userId + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            user.setAvatarUrl("/upload/" + fileName);
            wxUserMapper.updateById(user);
            return Result.success(toResponse(user, request));
        } catch (IOException e) {
            return Result.fail("头像上传失败，请稍后重试");
        }
    }

    private UserProfileResponse toResponse(WxUser user, HttpServletRequest request) {
        String avatarUrl = buildAvatarUrl(user.getAvatarUrl(), request);
        return UserProfileResponse.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .nickName(user.getNickName())
                .avatarUrl(avatarUrl)
                .phone(user.getPhone())
                .build();
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
        if (ext.length() > 6) {
            return ".jpg";
        }
        return ext;
    }

    private String buildAvatarUrl(String avatarPath, HttpServletRequest request) {
        if (avatarPath == null || avatarPath.isBlank()) {
            return avatarPath;
        }
        if (avatarPath.startsWith("http://") || avatarPath.startsWith("https://")) {
            return avatarPath;
        }
        HttpServletRequest safeRequest = Objects.requireNonNull(request);
        return ServletUriComponentsBuilder.fromRequestUri(safeRequest)
                .replacePath(avatarPath)
                .replaceQuery(null)
                .build()
                .toUriString();
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String right4(String phone) {
        if (phone == null) return "0000";
        return phone.substring(Math.max(0, phone.length() - 4));
    }

    private String hashPassword(String rawPassword) {
        String input = passwordSalt + rawPassword;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

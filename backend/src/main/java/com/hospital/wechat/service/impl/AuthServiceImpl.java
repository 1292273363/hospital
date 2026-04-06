package com.hospital.wechat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hospital.wechat.dto.AuthLoginRequest;
import com.hospital.wechat.dto.AuthSendCodeRequest;
import com.hospital.wechat.dto.LoginResponse;
import com.hospital.wechat.entity.Doctor;
import com.hospital.wechat.entity.DoctorMapper;
import com.hospital.wechat.entity.Patient;
import com.hospital.wechat.entity.PatientMapper;
import com.hospital.wechat.service.AuthService;
import com.hospital.wechat.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int CODE_TTL_SECONDS = 300;
    private static final Random RANDOM = new Random();

    private final PatientMapper patientMapper;
    private final DoctorMapper doctorMapper;
    private final JwtUtil jwtUtil;

    private final Map<String, CodeItem> codeStore = new ConcurrentHashMap<>();

    @Override
    public String sendCode(AuthSendCodeRequest request) {
        String role = normalizeRole(request == null ? null : request.getRole());
        String phone = normalizePhone(request == null ? null : request.getPhone());
        if (role == null) {
            throw new RuntimeException("role不能为空（patient/doctor）");
        }
        if (phone == null) {
            throw new RuntimeException("手机号不合法");
        }

        String code = buildCode();
        String key = role + ":" + phone;
        codeStore.put(key, new CodeItem(code, System.currentTimeMillis() + CODE_TTL_SECONDS * 1000L));

        // 开发期：直接返回验证码，便于联调；生产环境应接入短信服务并不返回
        log.info("send-code role={}, phone={}, code={}", role, phone, code);
        return code;
    }

    @Override
    public LoginResponse login(AuthLoginRequest request) {
        String role = normalizeRole(request == null ? null : request.getRole());
        String phone = normalizePhone(request == null ? null : request.getPhone());
        String code = normalizeVerifyCode(request == null ? null : request.getCode());
        if (role == null) {
            throw new RuntimeException("role不能为空（patient/doctor）");
        }
        if (phone == null) {
            throw new RuntimeException("手机号不合法");
        }
        if (code == null) {
            throw new RuntimeException("验证码须为6位数字");
        }

        verifyCode(role, phone, code);

        if ("patient".equals(role)) {
            Patient patient = getOrCreatePatient(phone);
            patient.setLastLoginTime(LocalDateTime.now());
            patientMapper.updateById(patient);

            String token = jwtUtil.generateRoleToken(JwtUtil.Role.PATIENT, patient.getId());
            return LoginResponse.builder()
                    .token(token)
                    .userInfo(LoginResponse.UserInfoVO.builder()
                            .id(patient.getId())
                            .openid(null)
                            .nickName(patient.getNickName() == null ? "患者" : patient.getNickName())
                            .avatarUrl(patient.getAvatarUrl())
                            .phone(patient.getPhone())
                            .build())
                    .build();
        }

        Doctor doctor = getOrCreateDoctor(phone);
        doctor.setLastLoginTime(LocalDateTime.now());
        doctorMapper.updateById(doctor);

        String token = jwtUtil.generateRoleToken(JwtUtil.Role.DOCTOR, doctor.getId());
        return LoginResponse.builder()
                .token(token)
                .userInfo(LoginResponse.UserInfoVO.builder()
                        .id(doctor.getId())
                        .openid(null)
                        .nickName(doctor.getDoctorName())
                        .avatarUrl(null)
                        .phone(doctor.getPhone())
                        .build())
                .build();
    }

    private void verifyCode(String role, String phone, String code) {
        String key = role + ":" + phone;
        CodeItem item = codeStore.get(key);
        if (item == null) {
            throw new RuntimeException("验证码不存在或已过期，请重新获取");
        }
        if (System.currentTimeMillis() > item.expiresAtMs) {
            codeStore.remove(key);
            throw new RuntimeException("验证码已过期，请重新获取");
        }
        if (!Objects.equals(item.code, code)) {
            log.warn("verify-code mismatch role={} phoneMasked={} expectLast2=**{} gotLast2=**{}",
                    role, maskPhone(phone),
                    last2(item.code), last2(code));
            throw new RuntimeException("验证码错误");
        }
        // 单次使用
        codeStore.remove(key);
    }

    private static String last2(String s) {
        if (s == null || s.length() < 2) {
            return "?";
        }
        return s.substring(s.length() - 2);
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private Patient getOrCreatePatient(String phone) {
        Patient patient = patientMapper.selectOne(new LambdaQueryWrapper<Patient>()
                .eq(Patient::getPhone, phone)
                .last("LIMIT 1"));
        if (patient != null) {
            return patient;
        }
        patient = new Patient();
        patient.setPhone(phone);
        patient.setNickName("患者");
        patient.setStatus(0);
        patientMapper.insert(patient);
        return patient;
    }

    private Doctor getOrCreateDoctor(String phone) {
        Doctor doctor = doctorMapper.selectOne(new LambdaQueryWrapper<Doctor>()
                .eq(Doctor::getPhone, phone)
                .last("LIMIT 1"));
        if (doctor != null) {
            return doctor;
        }
        // 最小可用：自动注册医生（你可后续改为只允许预置医生登录）
        doctor = new Doctor();
        doctor.setPhone(phone);
        doctor.setDoctorName("医生" + phone.substring(Math.max(0, phone.length() - 4)));
        doctor.setDoctorLevel("专家");
        doctor.setStatus(0);
        doctorMapper.insert(doctor);
        return doctor;
    }

    private String buildCode() {
        int n = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(n);
    }

    private String normalizeRole(String role) {
        String v = trimOrNull(role);
        if (v == null) return null;
        v = v.toLowerCase();
        if (v.equals("patient") || v.equals("doctor")) {
            return v;
        }
        return null;
    }

    private String normalizePhone(String phone) {
        String v = trimOrNull(phone);
        if (v == null) {
            return null;
        }
        v = v.replaceAll("\\s+", "");
        if (v.startsWith("+86")) {
            v = v.substring(3);
        } else if (v.startsWith("86") && v.length() == 13) {
            v = v.substring(2);
        }
        if (!v.matches("^\\d{11}$")) {
            return null;
        }
        return v;
    }

    /**
     * 仅保留数字，全角数字转半角，凑满6位才视为合法验证码。
     */
    private String normalizeVerifyCode(String raw) {
        String v = trimOrNull(raw);
        if (v == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            } else if (c >= '０' && c <= '９') {
                sb.append((char) ('0' + (c - '０')));
            }
        }
        String digits = sb.toString();
        return digits.length() == 6 ? digits : null;
    }

    private String trimOrNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.strip();
        return t.isEmpty() ? null : t;
    }

    private record CodeItem(String code, long expiresAtMs) {}
}


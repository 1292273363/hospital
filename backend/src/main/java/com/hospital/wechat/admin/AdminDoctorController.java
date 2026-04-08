package com.hospital.wechat.admin;

import com.hospital.wechat.admin.dto.AdminDoctorUpsertRequest;
import com.hospital.wechat.dto.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 医生管理（后台）。
 */
@RestController
@RequestMapping("/admin/api/doctors")
public class AdminDoctorController {

    private final JdbcTemplate jdbcTemplate;
    private final AdminAuthorizationService authorizationService;
    private final AdminPasswordHasher passwordHasher;

    public AdminDoctorController(
            JdbcTemplate jdbcTemplate,
            AdminAuthorizationService authorizationService,
            AdminPasswordHasher passwordHasher
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.authorizationService = authorizationService;
        this.passwordHasher = passwordHasher;
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request
    ) {
        if (!authorizationService.authorized(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.fail(401, "未授权"));
        }

        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safePage - 1) * safePageSize;

        String where = " WHERE 1=1 ";
        Object[] args;

        if (StringUtils.hasText(keyword)) {
            where += " AND (phone LIKE ? OR doctor_name LIKE ?) ";
            String like = "%" + keyword.trim() + "%";
            args = new Object[]{like, like};
        } else {
            args = new Object[]{};
        }

        String countSql = "SELECT COUNT(*) FROM doctor " + where;
        Long total = jdbcTemplate.queryForObject(countSql, args, Long.class);
        long safeTotal = total == null ? 0L : total;

        String listSql = ""
                + "SELECT "
                + " id, phone, doctor_name AS doctorName, doctor_level AS doctorLevel, "
                + " status, last_login_time AS lastLoginTime, create_time AS createTime, update_time AS updateTime "
                + "FROM doctor "
                + where
                + " ORDER BY id DESC LIMIT ? OFFSET ?";

        Object[] listArgs;
        if (args.length == 0) {
            listArgs = new Object[]{safePageSize, offset};
        } else {
            Object[] tmp = new Object[args.length + 2];
            System.arraycopy(args, 0, tmp, 0, args.length);
            tmp[args.length] = safePageSize;
            tmp[args.length + 1] = offset;
            listArgs = tmp;
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(listSql, listArgs);

        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("page", safePage);
        data.put("pageSize", safePageSize);
        data.put("total", safeTotal);
        return ResponseEntity.ok(Result.success(data));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody AdminDoctorUpsertRequest body, HttpServletRequest request) {
        if (!authorizationService.authorized(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.fail(401, "未授权"));
        }
        if (body == null || !StringUtils.hasText(body.getPhone()) || !StringUtils.hasText(body.getDoctorName())
                || !StringUtils.hasText(body.getDoctorLevel())) {
            return ResponseEntity.badRequest().body(Result.fail("doctorName/phone/doctorLevel不能为空"));
        }

        int status = body.getStatus() == null ? 0 : body.getStatus();
        String passwordHash = StringUtils.hasText(body.getPassword()) ? passwordHasher.hash(body.getPassword()) : null;

        jdbcTemplate.update(
                "INSERT INTO doctor (phone, doctor_name, doctor_level, status, password_hash) VALUES (?, ?, ?, ?, ?)",
                body.getPhone().trim(),
                body.getDoctorName().trim(),
                body.getDoctorLevel().trim(),
                status,
                passwordHash
        );

        return ResponseEntity.ok(Result.success());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody AdminDoctorUpsertRequest body,
            HttpServletRequest request
    ) {
        if (!authorizationService.authorized(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.fail(401, "未授权"));
        }
        if (body == null) return ResponseEntity.badRequest().body(Result.fail("body不能为空"));

        String passwordHash = StringUtils.hasText(body.getPassword()) ? passwordHasher.hash(body.getPassword()) : null;
        Integer status = body.getStatus() == null ? null : body.getStatus();

        // 允许：不传字段则保持原值
        jdbcTemplate.update(
                "UPDATE doctor "
                        + "SET doctor_name = CASE WHEN ? IS NULL THEN doctor_name ELSE ? END, "
                        + "    doctor_level = CASE WHEN ? IS NULL THEN doctor_level ELSE ? END, "
                        + "    status = CASE WHEN ? IS NULL THEN status ELSE ? END, "
                        + "    password_hash = CASE WHEN ? IS NULL THEN password_hash ELSE ? END "
                        + "WHERE id = ?",
                body.getDoctorName(), body.getDoctorName(),
                body.getDoctorLevel(), body.getDoctorLevel(),
                status, status,
                passwordHash, passwordHash,
                id
        );

        return ResponseEntity.ok(Result.success());
    }
}


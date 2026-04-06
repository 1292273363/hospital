package com.hospital.wechat.admin;

import com.hospital.wechat.dto.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 诊断记录管理（后台）。
 */
@RestController
@RequestMapping("/admin/api/diagnosis")
public class AdminDiagnosisController {

    private final JdbcTemplate jdbcTemplate;
    private final AdminAuthorizationService authorizationService;

    public AdminDiagnosisController(JdbcTemplate jdbcTemplate, AdminAuthorizationService authorizationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(HttpServletRequest request) {
        if (!authorizationService.authorized(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.fail(401, "未授权"));
        }

        String sql = ""
                + "SELECT "
                + "  COUNT(*) AS total, "
                + "  SUM(CASE WHEN doctor_level = '专家' THEN 1 ELSE 0 END) AS expertCount, "
                + "  SUM(CASE WHEN doctor_level <> '专家' OR doctor_level IS NULL THEN 1 ELSE 0 END) AS otherCount "
                + "FROM visit_record";
        Map<String, Object> row = jdbcTemplate.queryForMap(sql);
        return ResponseEntity.ok(Result.success(row));
    }

    @GetMapping("/records")
    public ResponseEntity<?> records(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dateFrom, // yyyy-MM-dd
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String doctorLevel,
            HttpServletRequest request
    ) {
        if (!authorizationService.authorized(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.fail(401, "未授权"));
        }

        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safePage - 1) * safePageSize;

        String where = " WHERE 1=1 ";
        List<Object> args = new java.util.ArrayList<>();

        if (StringUtils.hasText(keyword)) {
            where += " AND (pr.patient_name LIKE ? OR vr.doctor_name LIKE ? OR vr.diagnosis_report LIKE ?) ";
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }

        if (StringUtils.hasText(dateFrom)) {
            where += " AND vr.visit_time >= ? ";
            args.add(LocalDate.parse(dateFrom).atStartOfDay());
        }

        if (StringUtils.hasText(dateTo)) {
            where += " AND vr.visit_time <= ? ";
            // dateTo 当天 23:59:59
            args.add(LocalDate.parse(dateTo).plusDays(1).atStartOfDay().minusSeconds(1));
        }

        if (StringUtils.hasText(doctorLevel)) {
            where += " AND vr.doctor_level = ? ";
            args.add(doctorLevel.trim());
        }

        String baseFrom = " FROM visit_record vr LEFT JOIN patient_record pr ON pr.id = vr.patient_record_id ";

        String countSql = "SELECT COUNT(*)" + baseFrom + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, args.toArray());
        long safeTotal = total == null ? 0L : total;

        String listSql = ""
                + "SELECT "
                + " vr.id, "
                + " vr.visit_time AS visitTime, "
                + " vr.doctor_name AS doctorName, "
                + " vr.doctor_level AS doctorLevel, "
                + " pr.patient_name AS patientName, "
                + " pr.phone AS patientPhone, "
                + " vr.diagnosis_report AS diagnosisReport "
                + baseFrom
                + where
                + " ORDER BY vr.id DESC "
                + " LIMIT ? OFFSET ?";

        List<Object> listArgs = new java.util.ArrayList<>(args);
        listArgs.add(safePageSize);
        listArgs.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(listSql, listArgs.toArray());

        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("page", safePage);
        data.put("pageSize", safePageSize);
        data.put("total", safeTotal);
        return ResponseEntity.ok(Result.success(data));
    }
}


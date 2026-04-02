package com.hospital.wechat.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AdminApiController {

    private final JdbcTemplate jdbcTemplate;

    @Value("${admin.token:}")
    private String adminToken;

    public AdminApiController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private boolean authorized(HttpServletRequest request) {
        if (!StringUtils.hasText(adminToken)) {
            // 未配置 token：允许（仅用于本地/演示）
            return true;
        }
        String headerToken = request.getHeader("X-Admin-Token");
        return adminToken.equals(headerToken);
    }

    @GetMapping("/admin/api/tables")
    public ResponseEntity<?> tables(HttpServletRequest request) {
        if (!authorized(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "未授权"));
        }

        // 白名单：避免任意表名导致的越权查询
        List<Map<String, String>> tables = new ArrayList<>();
        tables.add(label("wx_user", "微信用户"));
        tables.add(label("patient_record", "患者档案"));
        tables.add(label("visit_record", "看诊记录"));
        tables.add(label("online_consultation", "在线问诊"));

        // 测试表（如果你执行了相关 SQL 会显示数据）
        tables.add(label("visit_record_test_dataset", "看诊测试数据(手机号维度)"));

        return ResponseEntity.ok(Map.of("tables", tables));
    }

    @GetMapping("/admin/api/table/{table}")
    public ResponseEntity<?> tableRows(
            @PathVariable String table,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request
    ) {
        if (!authorized(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "未授权"));
        }
        if (!isAllowedTable(table)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "不允许访问的表"));
        }

        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safePage - 1) * safePageSize;

        String countSql = "SELECT COUNT(*) FROM `" + table + "`";
        Long totalObj = jdbcTemplate.queryForObject(countSql, Long.class);
        long total = totalObj == null ? 0L : totalObj;

        // 绝大多数表都带 id 字段；用 id 倒序，便于看最新数据
        String sql = "SELECT * FROM `" + table + "` ORDER BY id DESC LIMIT ? OFFSET ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, safePageSize, offset);

        return ResponseEntity.ok(Map.of(
                "table", table,
                "page", safePage,
                "pageSize", safePageSize,
                "total", total,
                "rows", rows
        ));
    }

    @GetMapping("/admin/api/table/{table}/{id}")
    public ResponseEntity<?> tableRow(
            @PathVariable String table,
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        if (!authorized(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "未授权"));
        }
        if (!isAllowedTable(table)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "不允许访问的表"));
        }

        String sql = "SELECT * FROM `" + table + "` WHERE id = ? LIMIT 1";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, id);
        if (rows.isEmpty()) {
            return ResponseEntity.ok(Map.of("table", table, "id", id, "row", null));
        }
        return ResponseEntity.ok(Map.of("table", table, "id", id, "row", rows.get(0)));
    }

    private boolean isAllowedTable(String table) {
        return table.equals("wx_user")
                || table.equals("patient_record")
                || table.equals("visit_record")
                || table.equals("online_consultation")
                || table.equals("visit_record_test_dataset");
    }

    private Map<String, String> label(String name, String label) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("label", label);
        return m;
    }
}


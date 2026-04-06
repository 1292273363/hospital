package com.hospital.wechat.admin;

import com.hospital.wechat.admin.dto.AdminNoticeUpsertRequest;
import com.hospital.wechat.dto.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公告管理（后台）。
 */
@RestController
@RequestMapping("/admin/api/notices")
public class AdminNoticeController {

    private final JdbcTemplate jdbcTemplate;
    private final AdminAuthorizationService authorizationService;

    public AdminNoticeController(JdbcTemplate jdbcTemplate, AdminAuthorizationService authorizationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request
    ) {
        if (!authorizationService.authorized(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.fail(401, "未授权"));
        }

        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safePage - 1) * safePageSize;

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_notice", Long.class);
        long safeTotal = total == null ? 0L : total;

        String sql = ""
                + "SELECT id, title, content, status, sort_no AS sortNo, "
                + "       DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') AS createTime "
                + "FROM app_notice "
                + "ORDER BY sort_no DESC, id DESC "
                + "LIMIT ? OFFSET ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, safePageSize, offset);

        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("page", safePage);
        data.put("pageSize", safePageSize);
        data.put("total", safeTotal);
        return ResponseEntity.ok(Result.success(data));
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody AdminNoticeUpsertRequest body,
            HttpServletRequest request
    ) {
        if (!authorizationService.authorized(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.fail(401, "未授权"));
        }
        if (body == null || !StringUtils.hasText(body.getTitle()) || !StringUtils.hasText(body.getContent())) {
            return ResponseEntity.badRequest().body(Result.fail("title/content不能为空"));
        }

        int status = body.getStatus() == null ? 1 : body.getStatus();
        int sortNo = body.getSortNo() == null ? 0 : body.getSortNo();

        jdbcTemplate.update(
                "INSERT INTO app_notice (title, content, status, sort_no) VALUES (?, ?, ?, ?)",
                body.getTitle().trim(),
                body.getContent().trim(),
                status,
                sortNo
        );
        return ResponseEntity.ok(Result.success());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody AdminNoticeUpsertRequest body,
            HttpServletRequest request
    ) {
        if (!authorizationService.authorized(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.fail(401, "未授权"));
        }
        if (body == null) return ResponseEntity.badRequest().body(Result.fail("body不能为空"));

        String title = body.getTitle();
        String content = body.getContent();
        Integer status = body.getStatus();
        Integer sortNo = body.getSortNo();

        // 允许：不传字段就保持原值（更适合“编辑”）
        String sql = ""
                + "UPDATE app_notice SET "
                + "  title = CASE WHEN ? IS NULL THEN title ELSE ? END, "
                + "  content = CASE WHEN ? IS NULL THEN content ELSE ? END, "
                + "  status = CASE WHEN ? IS NULL THEN status ELSE ? END, "
                + "  sort_no = CASE WHEN ? IS NULL THEN sort_no ELSE ? END "
                + "WHERE id = ?";

        jdbcTemplate.update(
                sql,
                title, title,
                content, content,
                status, status,
                sortNo, sortNo,
                id
        );

        return ResponseEntity.ok(Result.success());
    }
}


package com.hospital.wechat.admin;

import com.hospital.wechat.admin.dto.AdminPatientUpdateRequest;
import com.hospital.wechat.dto.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 患者管理（后台）。
 */
@RestController
@RequestMapping("/admin/api/patients")
public class AdminPatientController {

    private final JdbcTemplate jdbcTemplate;
    private final AdminAuthorizationService authorizationService;

    public AdminPatientController(JdbcTemplate jdbcTemplate, AdminAuthorizationService authorizationService) {
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
                + "  SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS activeCount, "
                + "  SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS disabledCount "
                + "FROM patient";
        Map<String, Object> row = jdbcTemplate.queryForMap(sql);
        return ResponseEntity.ok(Result.success(row));
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
            where += " AND (phone LIKE ? OR nick_name LIKE ?) ";
            String like = "%" + keyword.trim() + "%";
            args = new Object[]{like, like};
        } else {
            args = new Object[]{};
        }

        String countSql = "SELECT COUNT(*) FROM patient " + where;
        Long total = jdbcTemplate.queryForObject(countSql, args, Long.class);
        long safeTotal = total == null ? 0L : total;

        String listSql = ""
                + "SELECT "
                + " id, phone, nick_name AS nickName, avatar_url AS avatarUrl, status, "
                + " last_login_time AS lastLoginTime, create_time AS createTime, update_time AS updateTime "
                + "FROM patient "
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

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody AdminPatientUpdateRequest body,
            HttpServletRequest request
    ) {
        if (!authorizationService.authorized(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.fail(401, "未授权"));
        }
        if (body == null) return ResponseEntity.badRequest().body(Result.fail("body不能为空"));

        String sql = ""
                + "UPDATE patient SET "
                + " nick_name = CASE WHEN ? IS NULL THEN nick_name ELSE ? END, "
                + " avatar_url = CASE WHEN ? IS NULL THEN avatar_url ELSE ? END, "
                + " status = ? "
                + "WHERE id = ?";

        Integer status = body.getStatus() == null ? 0 : body.getStatus();
        jdbcTemplate.update(
                sql,
                body.getNickName(), body.getNickName(),
                body.getAvatarUrl(), body.getAvatarUrl(),
                status,
                id
        );
        return ResponseEntity.ok(Result.success());
    }
}


package com.hospital.wechat.controller;

import com.hospital.wechat.dto.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 小程序公告（医院公告/通知）。
 */
@RestController
public class NoticeController {

    private final JdbcTemplate jdbcTemplate;

    public NoticeController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/notice/active")
    public Result<List<Map<String, Object>>> active() {
        // 小程序主页只展示：id/title/time
        String sql = ""
                + "SELECT id, title, DATE_FORMAT(create_time, '%Y-%m-%d') AS time "
                + "FROM app_notice "
                + "WHERE status = 1 "
                + "ORDER BY sort_no DESC, create_time DESC "
                + "LIMIT 10";
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            return Result.success(rows);
        } catch (BadSqlGrammarException e) {
            // 兼容：数据库尚未初始化到 app_notice 表时，直接返回空列表
            return Result.success(Collections.emptyList());
        }
    }
}


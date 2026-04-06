package com.hospital.wechat.admin.dto;

import lombok.Data;

@Data
public class AdminNoticeUpsertRequest {
    private String title;
    private String content;
    private Integer status; // 0/1
    private Integer sortNo; // 可选：置顶排序，数字越大越靠前
}


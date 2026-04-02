package com.hospital.wechat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("visit_record")
public class VisitRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long patientRecordId;

    private Long patientUserId;

    private Long patientId;

    private String doctorName;

    private String doctorLevel;

    private Long doctorId;

    private String diagnosisReport;

    private LocalDateTime visitTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

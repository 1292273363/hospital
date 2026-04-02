package com.hospital.wechat.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("online_consultation")
public class OnlineConsultation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long visitRecordId;

    private Long patientUserId;

    private Long patientId;

    private Long doctorId;

    private String scarImageUrl;

    private String patientMessage;

    private String doctorReply;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

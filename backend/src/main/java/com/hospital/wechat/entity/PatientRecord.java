package com.hospital.wechat.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("patient_record")
public class PatientRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long uploaderUserId;

    private Long uploaderPatientId;

    private String patientName;

    private String gender;

    private Integer age;

    private String phone;

    private String diseaseType;

    private String idCard;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

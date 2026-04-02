CREATE TABLE IF NOT EXISTS `patient_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uploader_user_id` BIGINT NOT NULL COMMENT '上传人用户ID',
  `uploader_patient_id` BIGINT NULL COMMENT '上传人患者ID（引用patient.id）',
  `patient_name` VARCHAR(64) NOT NULL COMMENT '患者姓名',
  `gender` VARCHAR(8) NOT NULL DEFAULT '男' COMMENT '性别',
  `age` INT NOT NULL COMMENT '年龄',
  `phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
  `disease_type` VARCHAR(128) NOT NULL COMMENT '病种',
  `id_card` VARCHAR(32) COMMENT '身份证号',
  `remark` VARCHAR(500) COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_uploader_user_id` (`uploader_user_id`),
  KEY `idx_uploader_patient_id` (`uploader_patient_id`),
  KEY `idx_patient_phone` (`phone`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者上传信息表';

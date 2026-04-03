CREATE TABLE IF NOT EXISTS `visit_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `patient_record_id` BIGINT COMMENT '关联患者档案ID',
  `patient_user_id` BIGINT NOT NULL COMMENT '患者用户ID',
  `patient_id` BIGINT NULL COMMENT '患者ID（引用patient.id）',
  `doctor_name` VARCHAR(64) NOT NULL COMMENT '医生姓名',
  `doctor_level` VARCHAR(32) NOT NULL COMMENT '医生级别，如专家/普通',
  `doctor_id` BIGINT NULL COMMENT '医生ID（引用doctor.id）',
  `diagnosis_report` TEXT COMMENT '诊断报告',
  `visit_time` DATETIME COMMENT '看诊时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_patient_record_id` (`patient_record_id`),
  KEY `idx_patient_user_id` (`patient_user_id`),
  KEY `idx_visit_patient_id` (`patient_id`),
  KEY `idx_visit_doctor_id` (`doctor_id`),
  KEY `idx_doctor_level` (`doctor_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='线下看诊记录表';

CREATE TABLE IF NOT EXISTS `online_consultation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `visit_record_id` BIGINT NOT NULL COMMENT '关联看诊记录ID',
  `patient_user_id` BIGINT NOT NULL COMMENT '患者用户ID',
  `patient_id` BIGINT NULL COMMENT '患者ID（引用patient.id）',
  `doctor_id` BIGINT NULL COMMENT '医生ID（引用doctor.id）',
  `scar_image_url` VARCHAR(512) NOT NULL COMMENT '疤痕图片地址',
  `patient_message` VARCHAR(1000) COMMENT '患者留言',
  `doctor_reply` VARCHAR(1000) COMMENT '专家回复',
  `status` VARCHAR(32) NOT NULL DEFAULT '待诊断' COMMENT '状态：待诊断/已回复',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_visit_record_id` (`visit_record_id`),
  KEY `idx_patient_user_id` (`patient_user_id`),
  KEY `idx_consult_patient_id` (`patient_id`),
  KEY `idx_consult_doctor_id` (`doctor_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='在线问诊记录表';

-- =========================
-- 专家看诊测试数据（在线问诊可选）
-- 说明：patient_user_id 需替换为你系统里真实的患者用户ID
-- =========================
INSERT INTO `visit_record` (`patient_user_id`, `doctor_name`, `doctor_level`, `diagnosis_report`, `visit_time`)
VALUES
  (1, '张建国', '专家', '瘢痕增生期，建议继续使用硅凝胶并配合弹力加压，4周后复诊评估。', '2026-03-05 10:30:00'),
  (1, '李晓梅', '专家', '局部色素沉着明显，建议加强防晒并短期外用药物，必要时联合激光治疗。', '2026-03-12 15:20:00'),
  (1, '王海峰', '专家', '瘢痕边缘轻度炎症反应，建议减少刺激，保持创面清洁，2周内复查。', '2026-03-20 09:40:00'),
  (2, '赵明远', '专家', '恢复期总体稳定，可逐步增加关节活动训练，避免过度牵拉瘢痕组织。', '2026-03-18 14:10:00'),
  (2, '周文静', '专家', '瘢痕厚度较前减轻，建议继续当前治疗方案并补充保湿护理。', '2026-03-25 11:00:00');

-- 看诊测试信息表（手机号维度）
CREATE TABLE IF NOT EXISTS `visit_record_test_dataset` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `patient_phone` VARCHAR(20) NOT NULL COMMENT '患者手机号',
  `doctor_name` VARCHAR(64) NOT NULL COMMENT '医生姓名',
  `doctor_level` VARCHAR(32) NOT NULL DEFAULT '专家' COMMENT '医生级别',
  `diagnosis_report` TEXT COMMENT '诊断报告',
  `visit_time` DATETIME COMMENT '看诊时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_patient_phone` (`patient_phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手机号-看诊测试数据表';

-- 目标手机号（按你的需求固定为该号码）
SET @target_phone := '18268068665';

-- 取该手机号最新创建的患者档案ID
SET @target_patient_record_id := (
  SELECT id
  FROM patient_record
  WHERE phone = @target_phone
  ORDER BY id DESC
  LIMIT 1
);

SET @target_uploader_user_id := (
  SELECT uploader_user_id
  FROM patient_record
  WHERE id = @target_patient_record_id
);

-- 清理该手机号旧测试数据，避免重复
DELETE FROM `visit_record_test_dataset`
WHERE patient_phone = @target_phone;

-- 清理该患者档案已存在的同名测试看诊记录（可重复执行）
DELETE FROM `visit_record`
WHERE patient_record_id = @target_patient_record_id
  AND doctor_name IN ('张建国', '李晓梅', '王海峰');

-- 重新写入该手机号测试数据
INSERT INTO `visit_record_test_dataset` (`patient_phone`, `doctor_name`, `doctor_level`, `diagnosis_report`, `visit_time`)
VALUES
  (@target_phone, '张建国', '专家', '瘢痕增生期，建议继续使用硅凝胶并配合弹力加压，4周后复诊评估。', '2026-03-05 10:30:00'),
  (@target_phone, '李晓梅', '专家', '局部色素沉着明显，建议加强防晒并短期外用药物，必要时联合激光治疗。', '2026-03-12 15:20:00'),
  (@target_phone, '王海峰', '专家', '瘢痕边缘轻度炎症反应，建议减少刺激，保持创面清洁，2周内复查。', '2026-03-20 09:40:00');

-- 将测试数据绑定到“最新 patient_record_id”
INSERT INTO `visit_record` (`patient_record_id`, `patient_user_id`, `doctor_name`, `doctor_level`, `diagnosis_report`, `visit_time`)
SELECT
  @target_patient_record_id AS patient_record_id,
  @target_uploader_user_id AS patient_user_id,
  d.doctor_name,
  d.doctor_level,
  d.diagnosis_report,
  d.visit_time
FROM `visit_record_test_dataset` d
WHERE d.patient_phone = @target_phone
  AND @target_patient_record_id IS NOT NULL;

-- =========================
-- 新增手机号 17051365950 的测试样例
-- =========================
SET @target_phone := '17051365950';

SET @target_patient_record_id := (
  SELECT id
  FROM patient_record
  WHERE phone = @target_phone
  ORDER BY id DESC
  LIMIT 1
);

SET @target_uploader_user_id := (
  SELECT uploader_user_id
  FROM patient_record
  WHERE id = @target_patient_record_id
);

DELETE FROM `visit_record_test_dataset`
WHERE patient_phone = @target_phone;

DELETE FROM `visit_record`
WHERE patient_record_id = @target_patient_record_id
  AND doctor_name IN ('陈立', '吴敏', '刘峰');

INSERT INTO `visit_record_test_dataset` (`patient_phone`, `doctor_name`, `doctor_level`, `diagnosis_report`, `visit_time`)
VALUES
  (@target_phone, '陈立', '专家', '瘢痕颜色较深，建议外用硅酮凝胶并加强防晒，3周后复评。', '2026-03-08 09:20:00'),
  (@target_phone, '吴敏', '专家', '局部轻度瘙痒，建议减少抓挠刺激，必要时短期抗炎处理。', '2026-03-16 16:10:00'),
  (@target_phone, '刘峰', '专家', '恢复情况稳定，可逐步增加功能锻炼，持续观察瘢痕厚度变化。', '2026-03-24 11:30:00');

INSERT INTO `visit_record` (`patient_record_id`, `patient_user_id`, `doctor_name`, `doctor_level`, `diagnosis_report`, `visit_time`)
SELECT
  @target_patient_record_id AS patient_record_id,
  @target_uploader_user_id AS patient_user_id,
  d.doctor_name,
  d.doctor_level,
  d.diagnosis_report,
  d.visit_time
FROM `visit_record_test_dataset` d
WHERE d.patient_phone = @target_phone
  AND @target_patient_record_id IS NOT NULL;

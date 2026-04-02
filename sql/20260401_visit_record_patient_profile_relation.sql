-- 给看诊记录表补充患者档案关联字段
ALTER TABLE `visit_record`
  ADD COLUMN `patient_record_id` BIGINT NULL COMMENT '关联患者档案ID' AFTER `id`;

ALTER TABLE `visit_record`
  ADD KEY `idx_patient_record_id` (`patient_record_id`);

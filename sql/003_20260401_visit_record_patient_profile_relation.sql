-- 给看诊记录表补充患者档案关联字段（幂等）
SET @ddl := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'visit_record'
        AND COLUMN_NAME = 'patient_record_id'
    ),
    'SELECT ''skip: visit_record.patient_record_id already exists''',
    'ALTER TABLE `visit_record` ADD COLUMN `patient_record_id` BIGINT NULL COMMENT ''关联患者档案ID'' AFTER `id`'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'visit_record'
        AND INDEX_NAME = 'idx_patient_record_id'
    ),
    'SELECT ''skip: idx_patient_record_id already exists''',
    'ALTER TABLE `visit_record` ADD KEY `idx_patient_record_id` (`patient_record_id`)'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

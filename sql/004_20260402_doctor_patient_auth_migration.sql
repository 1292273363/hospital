-- 2026-04-02
-- 医生/患者分表 + 角色化登录所需字段迁移
-- v2: 幂等迁移（可重复执行，已存在列/索引会自动跳过）
--
-- 说明：
-- 1) 为尽量不破坏现有演示/数据，本脚本采用“新增列”的方式迁移：
--    - patient_record: 增加 uploader_patient_id（未来替代 uploader_user_id）
--    - visit_record: 增加 patient_id / doctor_id（未来替代 patient_user_id 与 doctor_name 的弱关联）
--    - online_consultation: 增加 patient_id / doctor_id（未来替代 patient_user_id）
-- 2) 现阶段后端代码会优先使用新列（patient_id/doctor_id/uploader_patient_id）。
-- 3) 第 2～4 段 ALTER 已做幂等判断；第 5 段数据迁移依赖 wx_user 表，见下方说明。

-- =========================
-- 1) 新增 patient / doctor 表
-- =========================
CREATE TABLE IF NOT EXISTS `patient` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号(登录账号)',
  `nick_name` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
  `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-正常, 1-禁用',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_patient_phone` (`phone`),
  KEY `idx_patient_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者账号表';

CREATE TABLE IF NOT EXISTS `doctor` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号(登录账号)',
  `doctor_name` VARCHAR(64) NOT NULL COMMENT '医生姓名',
  `doctor_level` VARCHAR(32) NOT NULL DEFAULT '专家' COMMENT '医生级别，如专家/普通',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-正常, 1-禁用',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_phone` (`phone`),
  KEY `idx_doctor_level` (`doctor_level`),
  KEY `idx_doctor_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生账号表';

-- =========================
-- 2) patient_record 增加 uploader_patient_id
-- =========================
SET @ddl := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'patient_record'
        AND COLUMN_NAME = 'uploader_patient_id'
    ),
    'SELECT ''skip: patient_record.uploader_patient_id already exists''',
    'ALTER TABLE `patient_record` ADD COLUMN `uploader_patient_id` BIGINT NULL COMMENT ''上传人患者ID（引用patient.id）'' AFTER `uploader_user_id`'
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
        AND TABLE_NAME = 'patient_record'
        AND INDEX_NAME = 'idx_uploader_patient_id'
    ),
    'SELECT ''skip: idx_uploader_patient_id already exists''',
    'ALTER TABLE `patient_record` ADD KEY `idx_uploader_patient_id` (`uploader_patient_id`)'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- 3) visit_record 增加 patient_id / doctor_id
-- =========================
SET @ddl := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'visit_record'
        AND COLUMN_NAME = 'patient_id'
    ),
    'SELECT ''skip: visit_record.patient_id already exists''',
    'ALTER TABLE `visit_record` ADD COLUMN `patient_id` BIGINT NULL COMMENT ''患者ID（引用patient.id）'' AFTER `patient_user_id`'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'visit_record'
        AND COLUMN_NAME = 'doctor_id'
    ),
    'SELECT ''skip: visit_record.doctor_id already exists''',
    'ALTER TABLE `visit_record` ADD COLUMN `doctor_id` BIGINT NULL COMMENT ''医生ID（引用doctor.id）'' AFTER `doctor_level`'
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
        AND INDEX_NAME = 'idx_visit_patient_id'
    ),
    'SELECT ''skip: idx_visit_patient_id already exists''',
    'ALTER TABLE `visit_record` ADD KEY `idx_visit_patient_id` (`patient_id`)'
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
        AND INDEX_NAME = 'idx_visit_doctor_id'
    ),
    'SELECT ''skip: idx_visit_doctor_id already exists''',
    'ALTER TABLE `visit_record` ADD KEY `idx_visit_doctor_id` (`doctor_id`)'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- 4) online_consultation 增加 patient_id / doctor_id
-- =========================
SET @ddl := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'online_consultation'
        AND COLUMN_NAME = 'patient_id'
    ),
    'SELECT ''skip: online_consultation.patient_id already exists''',
    'ALTER TABLE `online_consultation` ADD COLUMN `patient_id` BIGINT NULL COMMENT ''患者ID（引用patient.id）'' AFTER `patient_user_id`'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'online_consultation'
        AND COLUMN_NAME = 'doctor_id'
    ),
    'SELECT ''skip: online_consultation.doctor_id already exists''',
    'ALTER TABLE `online_consultation` ADD COLUMN `doctor_id` BIGINT NULL COMMENT ''医生ID（引用doctor.id）'' AFTER `patient_id`'
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
        AND TABLE_NAME = 'online_consultation'
        AND INDEX_NAME = 'idx_consult_patient_id'
    ),
    'SELECT ''skip: idx_consult_patient_id already exists''',
    'ALTER TABLE `online_consultation` ADD KEY `idx_consult_patient_id` (`patient_id`)'
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
        AND TABLE_NAME = 'online_consultation'
        AND INDEX_NAME = 'idx_consult_doctor_id'
    ),
    'SELECT ''skip: idx_consult_doctor_id already exists''',
    'ALTER TABLE `online_consultation` ADD KEY `idx_consult_doctor_id` (`doctor_id`)'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- 5) 辅助迁移：用手机号从 wx_user/patient_record 推导 patient.id，并回填 uploader_patient_id / patient_id
--    （仅用于已有历史数据的快速迁移；若你不需要可跳过执行）
--    前置条件：必须已存在表 wx_user（可先执行 backend/src/main/resources/schema.sql 中建库+wx_user 部分）
-- =========================
-- 5.1 从 wx_user.phone 或 patient_record.phone 生成 patient（去重）
INSERT INTO `patient` (`phone`, `nick_name`, `avatar_url`, `status`, `last_login_time`)
SELECT DISTINCT
  wu.phone,
  wu.nick_name,
  wu.avatar_url,
  0,
  wu.last_login_time
FROM `wx_user` wu
WHERE wu.phone IS NOT NULL AND wu.phone <> ''
ON DUPLICATE KEY UPDATE
  `nick_name` = COALESCE(VALUES(`nick_name`), `patient`.`nick_name`),
  `avatar_url` = COALESCE(VALUES(`avatar_url`), `patient`.`avatar_url`),
  `last_login_time` = COALESCE(VALUES(`last_login_time`), `patient`.`last_login_time`);

INSERT INTO `patient` (`phone`, `status`)
SELECT DISTINCT
  pr.phone,
  0
FROM `patient_record` pr
WHERE pr.phone IS NOT NULL AND pr.phone <> ''
ON DUPLICATE KEY UPDATE `phone` = `patient`.`phone`;

-- 5.2 回填 patient_record.uploader_patient_id：优先用 uploader_user_id -> wx_user.phone -> patient.id
UPDATE `patient_record` pr
JOIN `wx_user` wu ON wu.id = pr.uploader_user_id
JOIN `patient` p ON p.phone = wu.phone
SET pr.uploader_patient_id = p.id
WHERE pr.uploader_patient_id IS NULL
  AND wu.phone IS NOT NULL AND wu.phone <> '';

-- 5.3 回填 visit_record.patient_id：用 patient_record.phone -> patient.id
UPDATE `visit_record` vr
JOIN `patient_record` pr ON pr.id = vr.patient_record_id
JOIN `patient` p ON p.phone = pr.phone
SET vr.patient_id = p.id
WHERE vr.patient_id IS NULL
  AND pr.phone IS NOT NULL AND pr.phone <> '';

-- 5.4 回填 online_consultation.patient_id：用 visit_record.patient_id
UPDATE `online_consultation` oc
JOIN `visit_record` vr ON vr.id = oc.visit_record_id
SET oc.patient_id = vr.patient_id
WHERE oc.patient_id IS NULL
  AND vr.patient_id IS NOT NULL;


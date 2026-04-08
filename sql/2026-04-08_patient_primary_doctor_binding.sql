-- 迁移：为 patient 表增加 primary_doctor_id（主治医生绑定）
-- 目的：修复登录时报错 Unknown column 'primary_doctor_id'

ALTER TABLE patient
    ADD COLUMN primary_doctor_id BIGINT NULL COMMENT '主治医生ID';

-- 可选：提升根据主治医生查询患者的性能
CREATE INDEX idx_patient_primary_doctor_id
    ON patient (primary_doctor_id);


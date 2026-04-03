-- 按手机号自动匹配 patient.id 并插入专家看诊测试记录
-- 使用前请修改手机号变量

SET @patient_phone := '18268068665';

-- 取患者ID（patient表）
SET @patient_id := (
  SELECT id
  FROM patient
  WHERE phone = @patient_phone
  ORDER BY id DESC
  LIMIT 1
);

SET @patient_record_id := (
  SELECT id
  FROM patient_record
  WHERE phone = @patient_phone
  ORDER BY id DESC
  LIMIT 1
);

SET @patient_user_id := (
  SELECT uploader_user_id
  FROM patient_record
  WHERE id = @patient_record_id
);

INSERT INTO visit_record (
  patient_record_id,
  patient_user_id,
  patient_id,
  doctor_name,
  doctor_level,
  diagnosis_report,
  visit_time
)
SELECT
  @patient_record_id,
  @patient_user_id,
  @patient_id,
  t.doctor_name,
  '专家',
  t.diagnosis_report,
  t.visit_time
FROM (
  SELECT '张建国' AS doctor_name, '瘢痕增生期，建议继续使用硅凝胶并配合弹力加压，4周后复诊评估。' AS diagnosis_report, '2026-03-05 10:30:00' AS visit_time
  UNION ALL
  SELECT '李晓梅', '局部色素沉着明显，建议加强防晒并短期外用药物，必要时联合激光治疗。', '2026-03-12 15:20:00'
  UNION ALL
  SELECT '王海峰', '瘢痕边缘轻度炎症反应，建议减少刺激，保持创面清洁，2周内复查。', '2026-03-20 09:40:00'
) t
WHERE @patient_id IS NOT NULL
  AND @patient_record_id IS NOT NULL
  AND @patient_user_id IS NOT NULL;

SELECT
  @patient_phone AS patient_phone,
  @patient_id AS patient_id,
  @patient_record_id AS patient_record_id,
  @patient_user_id AS patient_user_id,
  CASE
    WHEN @patient_id IS NULL THEN '未找到该手机号对应 patient，未插入数据（请先执行迁移脚本或通过登录自动创建）'
    WHEN @patient_record_id IS NULL THEN '该手机号没有患者档案，请先在上传页创建患者档案'
    WHEN @patient_user_id IS NULL THEN '该手机号档案缺少 uploader_user_id，未插入数据'
    ELSE '插入完成'
  END AS result;

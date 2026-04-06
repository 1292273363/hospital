-- =============================================================================
-- 医院微信小程序 — 全量数据库脚本（MySQL 5.7+）
-- 说明：
--   1. 合并原 001～005 及 resources/schema.sql 中的 wx_user 表定义。
--   2. 使用 utf8mb4 / InnoDB，避免 MySQL 8.0 专有语法（无 CTE/窗口函数等）。
--   3. 首次安装：直接执行本文件。重建库：取消下方 DROP DATABASE 注释后执行。
-- =============================================================================

/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

-- 如需清空并重建整个库，取消下一行注释（注意会删除库内全部数据）
-- DROP DATABASE IF EXISTS `hospital_db`;

CREATE DATABASE IF NOT EXISTS `hospital_db`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `hospital_db`;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `online_consultation`;
DROP TABLE IF EXISTS `visit_record`;
DROP TABLE IF EXISTS `visit_record_test_dataset`;
DROP TABLE IF EXISTS `app_notice`;
DROP TABLE IF EXISTS `admin_user`;
DROP TABLE IF EXISTS `patient_record`;
DROP TABLE IF EXISTS `patient`;
DROP TABLE IF EXISTS `doctor`;
DROP TABLE IF EXISTS `wx_user`;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 微信用户表（小程序登录）
-- -----------------------------------------------------------------------------
CREATE TABLE `wx_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `openid` VARCHAR(64) NOT NULL COMMENT '微信openid',
  `unionid` VARCHAR(64) DEFAULT NULL COMMENT '微信unionid',
  `nick_name` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
  `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-正常 1-禁用',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_phone` (`phone`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信用户表';

-- -----------------------------------------------------------------------------
-- 患者账号表（手机号登录）
-- -----------------------------------------------------------------------------
CREATE TABLE `patient` (
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

-- -----------------------------------------------------------------------------
-- 医生账号表
-- -----------------------------------------------------------------------------
CREATE TABLE `doctor` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号(登录账号)',
  `doctor_name` VARCHAR(64) NOT NULL COMMENT '医生姓名',
  `doctor_level` VARCHAR(32) NOT NULL DEFAULT '专家' COMMENT '医生级别，如专家/普通',
  `password_hash` VARCHAR(64) DEFAULT NULL COMMENT '密码哈希（后台可设置/重置）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-正常, 1-禁用',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_phone` (`phone`),
  KEY `idx_doctor_level` (`doctor_level`),
  KEY `idx_doctor_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生账号表';

-- -----------------------------------------------------------------------------
-- 管理员账号表（后台登录）
-- -----------------------------------------------------------------------------
CREATE TABLE `admin_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password_hash` VARCHAR(64) NOT NULL COMMENT '密码哈希',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-正常 1-禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员账号表';

-- -----------------------------------------------------------------------------
-- 医院公告表（小程序展示 + 后台管理）
-- -----------------------------------------------------------------------------
CREATE TABLE `app_notice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(128) NOT NULL COMMENT '公告标题',
  `content` TEXT COMMENT '公告内容（后台可编辑）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-隐藏 1-显示',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '置顶排序：数字越大越靠前',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_notice_status_sort` (`status`, `sort_no`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医院公告表';

-- -----------------------------------------------------------------------------
-- 患者档案（上传信息）
-- -----------------------------------------------------------------------------
CREATE TABLE `patient_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `uploader_user_id` BIGINT NOT NULL COMMENT '上传人用户ID(wx_user.id)',
  `uploader_patient_id` BIGINT DEFAULT NULL COMMENT '上传人患者ID（patient.id）',
  `patient_name` VARCHAR(64) NOT NULL COMMENT '患者姓名',
  `gender` VARCHAR(8) NOT NULL DEFAULT '男' COMMENT '性别',
  `age` INT NOT NULL COMMENT '年龄',
  `phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
  `disease_type` VARCHAR(128) NOT NULL COMMENT '病种',
  `id_card` VARCHAR(32) DEFAULT NULL COMMENT '身份证号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_uploader_user_id` (`uploader_user_id`),
  KEY `idx_uploader_patient_id` (`uploader_patient_id`),
  KEY `idx_patient_phone` (`phone`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者上传信息表';

-- -----------------------------------------------------------------------------
-- 线下看诊记录
-- -----------------------------------------------------------------------------
CREATE TABLE `visit_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `patient_record_id` BIGINT DEFAULT NULL COMMENT '关联患者档案ID',
  `patient_user_id` BIGINT NOT NULL COMMENT '患者用户ID(wx_user.id)',
  `patient_id` BIGINT DEFAULT NULL COMMENT '患者ID（patient.id）',
  `doctor_name` VARCHAR(64) NOT NULL COMMENT '医生姓名',
  `doctor_level` VARCHAR(32) NOT NULL COMMENT '医生级别，如专家/普通',
  `doctor_id` BIGINT DEFAULT NULL COMMENT '医生ID（doctor.id）',
  `diagnosis_report` TEXT COMMENT '诊断报告',
  `visit_time` DATETIME DEFAULT NULL COMMENT '看诊时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_patient_record_id` (`patient_record_id`),
  KEY `idx_patient_user_id` (`patient_user_id`),
  KEY `idx_visit_patient_id` (`patient_id`),
  KEY `idx_visit_doctor_id` (`doctor_id`),
  KEY `idx_doctor_level` (`doctor_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='线下看诊记录表';

-- -----------------------------------------------------------------------------
-- 在线问诊
-- -----------------------------------------------------------------------------
CREATE TABLE `online_consultation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `visit_record_id` BIGINT NOT NULL COMMENT '关联看诊记录ID',
  `patient_user_id` BIGINT NOT NULL COMMENT '患者用户ID',
  `patient_id` BIGINT DEFAULT NULL COMMENT '患者ID（patient.id）',
  `doctor_id` BIGINT DEFAULT NULL COMMENT '医生ID（doctor.id）',
  `scar_image_url` VARCHAR(512) NOT NULL COMMENT '疤痕图片地址',
  `patient_message` VARCHAR(1000) DEFAULT NULL COMMENT '患者留言',
  `doctor_reply` VARCHAR(1000) DEFAULT NULL COMMENT '专家回复',
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

-- -----------------------------------------------------------------------------
-- 看诊测试数据缓存表（按手机号导入 visit_record 时使用）
-- -----------------------------------------------------------------------------
CREATE TABLE `visit_record_test_dataset` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `patient_phone` VARCHAR(20) NOT NULL COMMENT '患者手机号',
  `doctor_name` VARCHAR(64) NOT NULL COMMENT '医生姓名',
  `doctor_level` VARCHAR(32) NOT NULL DEFAULT '专家' COMMENT '医生级别',
  `diagnosis_report` TEXT COMMENT '诊断报告',
  `visit_time` DATETIME DEFAULT NULL COMMENT '看诊时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_patient_phone` (`patient_phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手机号-看诊测试数据表';

-- =============================================================================
-- 可选：演示数据（与历史 002 脚本一致，依赖自增 ID 从 1 开始）
-- 若不需要演示数据，可删除自本行起至文件末尾的 DML。
-- =============================================================================

INSERT INTO `wx_user` (`openid`, `nick_name`, `avatar_url`, `phone`, `status`, `deleted`)
VALUES
  ('demo_openid_user_001', '演示患者甲', NULL, '13800000001', 0, 0),
  ('demo_openid_user_002', '演示患者乙', NULL, '13800000002', 0, 0);

INSERT INTO `patient` (`phone`, `nick_name`, `status`)
VALUES
  ('13800000001', '演示患者甲', 0),
  ('13800000002', '演示患者乙', 0);

-- 默认后台管理员：admin / 123456
-- password_hash = SHA256('hospital-admin-demo-salt' + '123456')
INSERT INTO `admin_user` (`username`, `password_hash`, `status`)
VALUES
  ('admin', '5b23d9e9878f66916d2bbcfa3aebfef61e2fd2bc98ed0a291d0ea1fd038191e8', 0);

INSERT INTO `visit_record` (`patient_user_id`, `doctor_name`, `doctor_level`, `diagnosis_report`, `visit_time`)
VALUES
  (1, '张建国', '专家', '瘢痕增生期，建议继续使用硅凝胶并配合弹力加压，4周后复诊评估。', '2026-03-05 10:30:00'),
  (1, '李晓梅', '专家', '局部色素沉着明显，建议加强防晒并短期外用药物，必要时联合激光治疗。', '2026-03-12 15:20:00'),
  (1, '王海峰', '专家', '瘢痕边缘轻度炎症反应，建议减少刺激，保持创面清洁，2周内复查。', '2026-03-20 09:40:00'),
  (2, '赵明远', '专家', '恢复期总体稳定，可逐步增加关节活动训练，避免过度牵拉瘢痕组织。', '2026-03-18 14:10:00'),
  (2, '周文静', '专家', '瘢痕厚度较前减轻，建议继续当前治疗方案并补充保湿护理。', '2026-03-25 11:00:00');

INSERT INTO `app_notice` (`title`, `content`, `status`, `sort_no`, `create_time`)
VALUES
  ('五一节假日门诊安排调整通知', '五一期间门诊安排调整，请关注具体科室出诊信息。', 1, 100, '2023-04-28 09:00:00'),
  ('我院新增整形美容专家门诊', '我院新增整形美容专家门诊，欢迎预约挂号。', 1, 80, '2023-04-15 10:00:00'),
  ('夏季整形优惠活动开始啦', '夏季优惠活动开启，详情以咨询台说明为准。', 1, 60, '2023-04-02 16:00:00');

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS hospital_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE hospital_db;

-- 微信用户表
CREATE TABLE IF NOT EXISTS `wx_user` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `openid`          VARCHAR(64)  NOT NULL                COMMENT '微信openid',
  `unionid`         VARCHAR(64)                          COMMENT '微信unionid',
  `nick_name`       VARCHAR(64)                          COMMENT '昵称',
  `avatar_url`      VARCHAR(512)                         COMMENT '头像URL',
  `phone`           VARCHAR(20)                          COMMENT '手机号',
  `status`          TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0-正常 1-禁用',
  `last_login_time` DATETIME                             COMMENT '最后登录时间',
  `create_time`     DATETIME     NOT NULL                COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL                COMMENT '更新时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_phone` (`phone`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信用户表';


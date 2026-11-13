-- “好帮手”平台 · 登录模块建表脚本（MySQL 8.x）
-- 应用启动时 Hibernate（spring.jpa.hibernate.ddl-auto=update）也会自动补建缺失的表，
-- 本脚本用于手动初始化数据库，可重复执行。

CREATE DATABASE IF NOT EXISTS `bupt_helper`
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE `bupt_helper`;

-- 用户信息表（《需求说明.md》三.1）
CREATE TABLE IF NOT EXISTS `sys_user`
(
    `user_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户标识',
    `username`      VARCHAR(50)  NOT NULL COMMENT '用户名（登录名）',
    `password`      VARCHAR(100) NOT NULL COMMENT '登录密码（BCrypt）',
    `user_type`     VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '用户类型：ADMIN 系统管理员 / USER 普通用户',
    `real_name`     VARCHAR(50)  NOT NULL COMMENT '用户姓名',
    `phone`         VARCHAR(11)  NOT NULL COMMENT '手机号码（11 位）',
    `bio`           VARCHAR(200) DEFAULT NULL COMMENT '用户简介',
    `register_time` DATETIME(6)  NOT NULL COMMENT '注册时间',
    `update_time`   DATETIME(6)  NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_sys_user_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户信息表';

-- 内置管理员账号 admin / admin 由应用启动时的 DataInitializer 写入（密码为 BCrypt 密文）

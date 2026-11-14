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

-- ============================================================================
-- “请帮忙”模块建表（《需求说明.md》三.2 ~ 三.5，见 doc/接口设计-请帮忙模块.md 6.2）
-- 应用启动时 Hibernate（ddl-auto=update）也会自动补建，本脚本用于手动初始化。
-- ============================================================================

-- 帮忙类型
CREATE TABLE IF NOT EXISTS `help_type`
(
    `type_code` VARCHAR(30) NOT NULL COMMENT '类型编码',
    `type_name` VARCHAR(30) NOT NULL COMMENT '类型名称',
    `sort_no`   INT         NOT NULL DEFAULT 0 COMMENT '排序号',
    PRIMARY KEY (`type_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='帮忙类型表';

-- 地域
CREATE TABLE IF NOT EXISTS `region`
(
    `region_id`   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '地域标识',
    `region_name` VARCHAR(50) NOT NULL COMMENT '地域名称',
    `city`        VARCHAR(50) NOT NULL COMMENT '所属地市',
    `province`    VARCHAR(50) NOT NULL COMMENT '所属省份',
    `sort_no`     INT         NOT NULL DEFAULT 0 COMMENT '排序号',
    PRIMARY KEY (`region_id`),
    KEY `idx_region_province_city` (`province`, `city`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='地域信息表';

-- “请帮忙”需求
CREATE TABLE IF NOT EXISTS `help_request`
(
    `request_id`      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '需求标识',
    `publish_user_id` BIGINT        NOT NULL COMMENT '发布用户标识',
    `region_id`       BIGINT        NOT NULL COMMENT '地域标识',
    `type_code`       VARCHAR(30)   NOT NULL COMMENT '帮忙类型编码',
    `title`           VARCHAR(50)   NOT NULL COMMENT '需求主题',
    `description`     VARCHAR(1000) NOT NULL COMMENT '需求描述',
    `status`          TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0 已发布；-1 已取消',
    `create_time`     DATETIME(6)   NOT NULL COMMENT '创建时间',
    `update_time`     DATETIME(6)   NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`request_id`),
    KEY `idx_help_request_user` (`publish_user_id`, `status`, `create_time`),
    KEY `idx_help_request_type` (`type_code`, `status`),
    KEY `idx_help_request_region` (`region_id`),
    CONSTRAINT `fk_help_request_user` FOREIGN KEY (`publish_user_id`) REFERENCES `sys_user` (`user_id`),
    CONSTRAINT `fk_help_request_region` FOREIGN KEY (`region_id`) REFERENCES `region` (`region_id`),
    CONSTRAINT `fk_help_request_type` FOREIGN KEY (`type_code`) REFERENCES `help_type` (`type_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='请帮忙需求表';

-- “我来也”响应
CREATE TABLE IF NOT EXISTS `help_response`
(
    `response_id`  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '响应标识',
    `request_id`   BIGINT        NOT NULL COMMENT '需求标识',
    `responder_id` BIGINT        NOT NULL COMMENT '响应用户标识',
    `description`  VARCHAR(1000) NOT NULL COMMENT '响应描述',
    `status`       TINYINT       NOT NULL DEFAULT 0 COMMENT '0 待接受；1 同意；2 拒绝；3 取消',
    `create_time`  DATETIME(6)   NOT NULL COMMENT '创建时间',
    `update_time`  DATETIME(6)   NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`response_id`),
    KEY `idx_help_response_request` (`request_id`, `status`),
    KEY `idx_help_response_responder` (`responder_id`, `status`),
    CONSTRAINT `fk_help_response_request` FOREIGN KEY (`request_id`) REFERENCES `help_request` (`request_id`),
    CONSTRAINT `fk_help_response_user` FOREIGN KEY (`responder_id`) REFERENCES `sys_user` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='我来也响应表';

-- 需求 / 响应附件（统一存储，“请帮忙”与“我来也”共用）
CREATE TABLE IF NOT EXISTS `help_request_file`
(
    `file_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文件标识',
    `request_id`   BIGINT       DEFAULT NULL COMMENT '需求标识，NULL 表示未绑定到需求',
    `response_id`  BIGINT       DEFAULT NULL COMMENT '响应标识，NULL 表示未绑定到响应',
    `uploader_id`  BIGINT       NOT NULL COMMENT '上传用户标识',
    `file_name`    VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `stored_name`  VARCHAR(100) NOT NULL COMMENT '存储文件名（yyyy/MM/uuid.ext）',
    `media_type`   VARCHAR(10)  NOT NULL COMMENT 'IMAGE / VIDEO',
    `content_type` VARCHAR(100) NOT NULL COMMENT 'MIME 类型',
    `file_size`    BIGINT       NOT NULL COMMENT '文件大小（字节）',
    `sort_no`      INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    `create_time`  DATETIME(6)  NOT NULL COMMENT '上传时间',
    PRIMARY KEY (`file_id`),
    UNIQUE KEY `uk_help_request_file_stored` (`stored_name`),
    KEY `idx_help_request_file_request` (`request_id`, `sort_no`),
    KEY `idx_help_request_file_response` (`response_id`, `sort_no`),
    KEY `idx_help_request_file_uploader` (`uploader_id`, `request_id`),
    CONSTRAINT `fk_help_request_file_request` FOREIGN KEY (`request_id`) REFERENCES `help_request` (`request_id`),
    CONSTRAINT `fk_help_request_file_response` FOREIGN KEY (`response_id`) REFERENCES `help_response` (`response_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='请帮忙需求/我来也响应附件表';

-- “好帮手”响应成功明细
CREATE TABLE IF NOT EXISTS `help_success`
(
    `success_id`        BIGINT      NOT NULL AUTO_INCREMENT,
    `request_id`        BIGINT      NOT NULL COMMENT '需求标识',
    `publish_user_id`   BIGINT      NOT NULL COMMENT '发布需求用户标识',
    `response_id`       BIGINT      NOT NULL COMMENT '响应标识',
    `responder_user_id` BIGINT      NOT NULL COMMENT '响应用户标识',
    `accept_time`       DATETIME(6) NOT NULL COMMENT '接受日期',
    PRIMARY KEY (`success_id`),
    UNIQUE KEY `uk_help_success_request` (`request_id`),
    UNIQUE KEY `uk_help_success_response` (`response_id`),
    KEY `idx_help_success_accept_time` (`accept_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='好帮手响应成功明细表';

-- 字典种子数据（应用启动时 HelpDictInitializer 也会写入，此处供手动初始化）
INSERT INTO `help_type` (`type_code`, `type_name`, `sort_no`) VALUES
    ('TOUR_GUIDE', '导游', 1),
    ('SHOPPING', '代购', 2),
    ('PET_CARE', '宠物代管', 3),
    ('PLANT_CARE', '绿植代培', 4),
    ('HOSPITAL_CARE', '住院陪护', 5),
    ('OTHER', '其他', 99)
ON DUPLICATE KEY UPDATE `type_name` = VALUES(`type_name`), `sort_no` = VALUES(`sort_no`);

INSERT INTO `region` (`region_name`, `city`, `province`, `sort_no`) VALUES
    ('海淀区', '北京市', '北京市', 1),
    ('朝阳区', '北京市', '北京市', 2),
    ('西城区', '北京市', '北京市', 3),
    ('昌平区', '北京市', '北京市', 4),
    ('浦东新区', '上海市', '上海市', 5),
    ('徐汇区', '上海市', '上海市', 6),
    ('长安区', '石家庄市', '河北省', 7),
    ('天河区', '广州市', '广东省', 8);

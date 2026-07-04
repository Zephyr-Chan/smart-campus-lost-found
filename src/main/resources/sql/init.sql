-- ============================================================
-- 校园失物招领智能匹配系统 - 数据库初始化脚本
-- 数据库: lost_found  |  MySQL 8.0+  |  字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS lost_found DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE lost_found;

-- ============================================================
-- 1. 用户表 (user)
-- ============================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    `real_name`   VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像路径',
    `role`        TINYINT      NOT NULL DEFAULT 0 COMMENT '角色（0-普通用户，1-管理员）',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态（0-禁用，1-正常）',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 2. 丢失物品表 (lost_item)
-- ============================================================
DROP TABLE IF EXISTS `lost_item`;
CREATE TABLE `lost_item` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`      BIGINT       NOT NULL COMMENT '发布者用户ID',
    `title`        VARCHAR(100) NOT NULL COMMENT '物品标题',
    `description`  TEXT         DEFAULT NULL COMMENT '物品描述',
    `category`     VARCHAR(20)  DEFAULT NULL COMMENT '物品分类（electronics/certificate/book/clothing/other）',
    `location`     VARCHAR(200) DEFAULT NULL COMMENT '丢失地点',
    `event_time`   DATETIME     DEFAULT NULL COMMENT '丢失时间',
    `contact_info` VARCHAR(100) DEFAULT NULL COMMENT '联系方式',
    `images`       VARCHAR(500) DEFAULT NULL COMMENT '图片路径（逗号分隔）',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态（0-待匹配，1-已匹配，2-已关闭）',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='丢失物品表';

-- ============================================================
-- 3. 拾到物品表 (found_item)
-- ============================================================
DROP TABLE IF EXISTS `found_item`;
CREATE TABLE `found_item` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`      BIGINT       NOT NULL COMMENT '发布者用户ID',
    `title`        VARCHAR(100) NOT NULL COMMENT '物品标题',
    `description`  TEXT         DEFAULT NULL COMMENT '物品描述',
    `category`     VARCHAR(20)  DEFAULT NULL COMMENT '物品分类',
    `location`     VARCHAR(200) DEFAULT NULL COMMENT '拾取地点',
    `event_time`   DATETIME     DEFAULT NULL COMMENT '拾取时间',
    `contact_info` VARCHAR(100) DEFAULT NULL COMMENT '联系方式',
    `images`       VARCHAR(500) DEFAULT NULL COMMENT '图片路径（逗号分隔）',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态（0-待匹配，1-已匹配，2-已关闭）',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拾到物品表';

-- ============================================================
-- 4. 认领记录表 (claim)
-- ============================================================
DROP TABLE IF EXISTS `claim`;
CREATE TABLE `claim` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `lost_item_id`  BIGINT        NOT NULL COMMENT '失物ID',
    `found_item_id` BIGINT        NOT NULL COMMENT '拾物ID',
    `claimant_id`   BIGINT        NOT NULL COMMENT '认领人ID（失物发布者）',
    `respondent_id` BIGINT        DEFAULT NULL COMMENT '被认领人ID（拾物发布者）',
    `match_score`   DECIMAL(5,2)  DEFAULT NULL COMMENT '匹配分数',
    `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '状态（0-待审核，1-已通过，2-已拒绝，3-已完成）',
    `remark`        VARCHAR(500)  DEFAULT NULL COMMENT '备注',
    `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_lost_item_id` (`lost_item_id`),
    KEY `idx_found_item_id` (`found_item_id`),
    KEY `idx_claimant_id` (`claimant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认领记录表';

-- ============================================================
-- 5. 公告表 (announcement)
-- ============================================================
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title`       VARCHAR(200) NOT NULL COMMENT '公告标题',
    `content`     TEXT         NOT NULL COMMENT '公告内容',
    `publisher_id` BIGINT      NOT NULL COMMENT '发布者ID',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态（0-草稿，1-已发布，2-已下架）',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- ============================================================
-- 6. 操作日志表 (operation_log)
-- ============================================================
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT       DEFAULT NULL COMMENT '操作用户ID',
    `username`    VARCHAR(50)  DEFAULT NULL COMMENT '操作用户名',
    `operation`   VARCHAR(200) DEFAULT NULL COMMENT '操作描述',
    `method`      VARCHAR(200) DEFAULT NULL COMMENT '操作方法',
    `params`      TEXT         DEFAULT NULL COMMENT '请求参数',
    `ip`          VARCHAR(50)  DEFAULT NULL COMMENT '请求IP',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ============================================================
-- 初始数据
-- ============================================================

-- 管理员账号（用户名: admin, 密码: debug123）
INSERT INTO `user` (`username`, `password`, `real_name`, `role`, `status`) VALUES
('admin', '$2a$10$MqJ4vMmnlCuhmm6xvC54xelKmpsq66raJ6DpLL1cGLpcGb5O7WI/i', '系统管理员', 1, 1);

-- 系统公告
INSERT INTO `announcement` (`title`, `content`, `publisher_id`, `status`) VALUES
('欢迎使用校园失物招领智能匹配系统',
 '本系统采用TF-IDF算法实现失物智能匹配，发布丢失物品后系统将自动为您匹配拾到物品。',
 1, 1);

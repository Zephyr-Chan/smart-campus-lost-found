-- ============================================
-- 校园失物招领智能匹配系统 数据库初始化脚本
-- Database: lost_found
-- Charset: utf8mb4
-- ============================================

CREATE DATABASE IF NOT EXISTS lost_found DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE lost_found;

-- -------------------------------------------
-- 1. 用户表
-- -------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    `real_name`   VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像路径',
    `role`        TINYINT      NOT NULL DEFAULT 0 COMMENT '角色:0学生/1管理员',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:0禁用/1正常',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- -------------------------------------------
-- 2. 丢失物品表
-- -------------------------------------------
DROP TABLE IF EXISTS `lost_item`;
CREATE TABLE `lost_item` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`      BIGINT       NOT NULL COMMENT '发布者ID',
    `title`        VARCHAR(100) NOT NULL COMMENT '物品标题',
    `description`  TEXT         NOT NULL COMMENT '详细描述(用于TF-IDF匹配)',
    `category`     VARCHAR(30)  NOT NULL COMMENT '分类:electronics/certificate/book/clothing/other',
    `location`     VARCHAR(200) NOT NULL COMMENT '丢失地点',
    `event_time`   DATETIME     NOT NULL COMMENT '丢失时间',
    `contact_info` VARCHAR(200) DEFAULT NULL COMMENT '联系方式',
    `images`       VARCHAR(500) DEFAULT NULL COMMENT '图片路径(JSON数组)',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '0待处理/1已匹配/2已关闭',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='丢失物品表';

-- -------------------------------------------
-- 3. 拾到物品表
-- -------------------------------------------
DROP TABLE IF EXISTS `found_item`;
CREATE TABLE `found_item` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`      BIGINT       NOT NULL COMMENT '发布者ID',
    `title`        VARCHAR(100) NOT NULL COMMENT '物品标题',
    `description`  TEXT         NOT NULL COMMENT '详细描述(用于TF-IDF匹配)',
    `category`     VARCHAR(30)  NOT NULL COMMENT '分类:electronics/certificate/book/clothing/other',
    `location`     VARCHAR(200) NOT NULL COMMENT '拾到地点',
    `event_time`   DATETIME     NOT NULL COMMENT '拾到时间',
    `contact_info` VARCHAR(200) DEFAULT NULL COMMENT '联系方式',
    `images`       VARCHAR(500) DEFAULT NULL COMMENT '图片路径(JSON数组)',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '0待处理/1已匹配/2已关闭',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拾到物品表';

-- -------------------------------------------
-- 4. 认领记录表
-- -------------------------------------------
DROP TABLE IF EXISTS `claim`;
CREATE TABLE `claim` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `lost_item_id`  BIGINT       DEFAULT NULL COMMENT '丢失物品ID',
    `found_item_id` BIGINT       DEFAULT NULL COMMENT '拾到物品ID',
    `claimant_id`   BIGINT       NOT NULL COMMENT '认领人ID',
    `respondent_id` BIGINT       NOT NULL COMMENT '发布人ID',
    `match_score`   DECIMAL(5,4) DEFAULT NULL COMMENT 'TF-IDF匹配分数(0~1)',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审核/1已通过/2已拒绝/3已完成',
    `remark`        VARCHAR(500) DEFAULT NULL COMMENT '认领备注',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_lost_item_id` (`lost_item_id`),
    KEY `idx_found_item_id` (`found_item_id`),
    KEY `idx_claimant_id` (`claimant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认领记录表';

-- -------------------------------------------
-- 5. 公告表
-- -------------------------------------------
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`        VARCHAR(200) NOT NULL COMMENT '公告标题',
    `content`      TEXT         NOT NULL COMMENT '公告内容',
    `publisher_id` BIGINT       DEFAULT NULL COMMENT '发布人ID',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '0下架/1发布',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- -------------------------------------------
-- 6. 操作日志表
-- -------------------------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       DEFAULT NULL COMMENT '操作用户ID',
    `username`    VARCHAR(50)  DEFAULT NULL COMMENT '用户名(冗余)',
    `operation`   VARCHAR(100) NOT NULL COMMENT '操作描述',
    `method`      VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
    `params`      TEXT         DEFAULT NULL COMMENT '请求参数',
    `ip`          VARCHAR(50)  DEFAULT NULL COMMENT '请求IP',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- -------------------------------------------
-- 初始数据
-- -------------------------------------------
-- 管理员账号: admin / admin123 (BCrypt加密)
INSERT INTO `user` (`username`, `password`, `real_name`, `role`, `status`) VALUES
('admin', '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7pCu6v2XyFDtC4q.jeeGm/TEZyj3C6', '系统管理员', 1, 1);

-- 测试学生账号: student / student123 (BCrypt加密)
INSERT INTO `user` (`username`, `password`, `real_name`, `phone`, `role`, `status`) VALUES
('student', '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7pCu6v2XyFDtC4q.jeeGm/TEZyj3C6', '测试学生', '13800138000', 0, 1);

-- 初始公告
INSERT INTO `announcement` (`title`, `content`, `publisher_id`, `status`) VALUES
('欢迎使用校园失物招领智能匹配系统', '本系统采用TF-IDF算法实现失物智能匹配，发布丢失物品后系统将自动为您匹配拾到物品。', 1, 1);

-- 测试丢失物品数据
INSERT INTO `lost_item` (`user_id`, `title`, `description`, `category`, `location`, `event_time`, `contact_info`, `status`) VALUES
(2, '黑色钱包丢失', '黑色真皮钱包，里面有学生证和银行卡，在图书馆三楼丢失', 'other', '图书馆三楼', '2026-06-28 14:30:00', '13800138000', 0),
(2, '华为手机丢失', '华为Mate40黑色手机，带有蓝色手机壳，在教学楼A栋丢失', 'electronics', '教学楼A栋', '2026-06-29 09:00:00', '13800138000', 0);

-- 测试拾到物品数据
INSERT INTO `found_item` (`user_id`, `title`, `description`, `category`, `location`, `event_time`, `contact_info`, `status`) VALUES
(2, '捡到黑色钱包', '在图书馆三楼捡到一个黑色真皮钱包，内有学生证和银行卡', 'other', '图书馆三楼', '2026-06-28 15:00:00', '13800138001', 0),
(2, '捡到华为手机', '在教学楼A栋教室捡到华为Mate40手机，蓝色手机壳', 'electronics', '教学楼A栋', '2026-06-29 10:00:00', '13800138001', 0);

-- 测试操作日志
INSERT INTO `operation_log` (`user_id`, `username`, `operation`, `method`, `ip`) VALUES
(1, 'admin', '系统初始化', 'SYSTEM_INIT', '127.0.0.1');

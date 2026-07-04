-- ============================================================
-- 校园失物招领系统 V2 升级迁移脚本
-- 新增 7 张表 + user 表新增信用积分列
-- 执行前提：已执行 init.sql 创建基础 6 张表
-- ============================================================

USE lost_found;

-- ============================================================
-- user 表新增信用积分列
-- ============================================================
ALTER TABLE `user` ADD COLUMN `credit_score` INT NOT NULL DEFAULT 100 COMMENT '信用积分' AFTER `status`;

-- ============================================================
-- 1. message 站内消息表
-- ============================================================
CREATE TABLE IF NOT EXISTS `message` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `receiver_id` BIGINT       NOT NULL COMMENT '接收者用户ID',
  `sender_id`   BIGINT       DEFAULT NULL COMMENT '发送者ID（0=系统）',
  `type`        VARCHAR(20)  NOT NULL COMMENT '消息类型（match/claim/credit/system）',
  `title`       VARCHAR(200) NOT NULL COMMENT '消息标题',
  `content`     TEXT         DEFAULT NULL COMMENT '消息内容',
  `ref_id`      BIGINT       DEFAULT NULL COMMENT '关联业务ID',
  `is_read`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读（0-未读，1-已读）',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_receiver_read` (`receiver_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

-- ============================================================
-- 2. user_credit_log 信用积分记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_credit_log` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
  `change_score` INT        NOT NULL COMMENT '变动分数（正数增加，负数减少）',
  `after_score` INT         NOT NULL COMMENT '变动后积分',
  `reason`      VARCHAR(50) NOT NULL COMMENT '变动原因',
  `ref_id`      BIGINT      DEFAULT NULL COMMENT '关联业务ID',
  `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用积分记录表';

-- ============================================================
-- 3. comment 评论表
-- ============================================================
CREATE TABLE IF NOT EXISTS `comment` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `item_id`     BIGINT       NOT NULL COMMENT '物品ID',
  `item_type`   VARCHAR(10)  NOT NULL COMMENT '物品类型（lost/found）',
  `user_id`     BIGINT       NOT NULL COMMENT '评论用户ID',
  `parent_id`   BIGINT       DEFAULT NULL COMMENT '父评论ID（空=一级评论）',
  `content`     VARCHAR(500) NOT NULL COMMENT '评论内容',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态（0-删除，1-正常）',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_item` (`item_id`, `item_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品评论表';

-- ============================================================
-- 4. favorite 收藏表
-- ============================================================
CREATE TABLE IF NOT EXISTS `favorite` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
  `item_id`     BIGINT      NOT NULL COMMENT '物品ID',
  `item_type`   VARCHAR(10) NOT NULL COMMENT '物品类型（lost/found）',
  `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_item` (`user_id`, `item_id`, `item_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品收藏表';

-- ============================================================
-- 5. item_vector 物品向量元数据表
-- ============================================================
CREATE TABLE IF NOT EXISTS `item_vector` (
  `id`               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `item_id`          BIGINT      NOT NULL COMMENT '物品ID',
  `item_type`        VARCHAR(10) NOT NULL COMMENT '物品类型（lost/found）',
  `has_text_vector`  TINYINT     NOT NULL DEFAULT 0 COMMENT '是否有文本向量',
  `has_image_vector` TINYINT     NOT NULL DEFAULT 0 COMMENT '是否有图像向量',
  `text_dim`         INT         DEFAULT NULL COMMENT '文本向量维度',
  `image_dim`        INT         DEFAULT NULL COMMENT '图像向量维度',
  `indexed_at`       DATETIME    DEFAULT NULL COMMENT '索引完成时间',
  `create_time`      DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item` (`item_id`, `item_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品向量元数据表';

-- ============================================================
-- 6. item_location 物品位置表
-- ============================================================
CREATE TABLE IF NOT EXISTS `item_location` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `item_id`     BIGINT        NOT NULL COMMENT '物品ID',
  `item_type`   VARCHAR(10)   NOT NULL COMMENT '物品类型（lost/found）',
  `longitude`   DECIMAL(10,7) NOT NULL COMMENT '经度',
  `latitude`    DECIMAL(10,7) NOT NULL COMMENT '纬度',
  `geohash`     VARCHAR(12)   DEFAULT NULL COMMENT 'GeoHash编码',
  `address`     VARCHAR(200)  DEFAULT NULL COMMENT '详细地址',
  `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item` (`item_id`, `item_type`),
  KEY `idx_geohash` (`geohash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品地理位置表';

-- ============================================================
-- 7. user_view_history 用户浏览历史表（Item-CF推荐用）
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_view_history` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
  `item_id`     BIGINT      NOT NULL COMMENT '物品ID',
  `item_type`   VARCHAR(10) NOT NULL COMMENT '物品类型（lost/found）',
  `view_time`   DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户浏览历史表';

-- ============================================================
-- 迁移完成，当前共 14 张表
-- ============================================================

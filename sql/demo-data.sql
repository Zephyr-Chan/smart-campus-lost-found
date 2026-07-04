-- ============================================
-- 校园失物招领系统 · 测试数据 Demo
-- 覆盖 13 张表，真实校园场景
--
-- 用途：为系统演示与报告截图提供完整测试数据
-- 密码：所有用户明文密码统一为 123456（BCrypt 加密）
-- 幂等性：脚本先 DELETE 所有业务表再 INSERT，可重复执行
-- 执行方式：mysql -u root -p lost_found < sql/demo-data.sql
-- 依赖：需先执行 init.sql / lost_found.sql 建表
-- 数据规模：user 8 / lost_item 18 / found_item 18 / claim 12 /
--           comment 25 / favorite 20 / message 30 / user_credit_log 40 /
--           announcement 6 / operation_log 30 / user_view_history 25 /
--           item_location 36 / item_vector 36
-- ============================================
USE lost_found;

-- 关闭外键检查，便于清理
SET FOREIGN_KEY_CHECKS = 0;

-- 清理所有业务表（按依赖反序）
DELETE FROM `item_vector`;
DELETE FROM `item_location`;
DELETE FROM `user_view_history`;
DELETE FROM `operation_log`;
DELETE FROM `user_credit_log`;
DELETE FROM `announcement`;
DELETE FROM `message`;
DELETE FROM `favorite`;
DELETE FROM `comment`;
DELETE FROM `claim`;
DELETE FROM `found_item`;
DELETE FROM `lost_item`;
DELETE FROM `user`;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 1. 用户表（8 条）
-- 密码明文 123456，BCrypt 加密串统一
-- 第 8 个用户 status=0（已禁用），用于演示禁用账户
-- ============================================
INSERT INTO `user` (`id`, `username`, `password`, `real_name`, `phone`, `email`, `avatar`, `role`, `status`) VALUES
(1, 'admin',        '$2a$10$MiV3iG3o3TvXXTu1VNOa5uoUnR4N1nFDU0rso5rWCouc8NrAk5mQK', '系统管理员', '13800000001', 'admin@campus.edu',  NULL, 1, 1),
(2, 'zhangwei2024', '$2a$10$MiV3iG3o3TvXXTu1VNOa5uoUnR4N1nFDU0rso5rWCouc8NrAk5mQK', '张伟', '13800000002', 'zhangwei@campus.edu', NULL, 0, 1),
(3, 'lina2023',     '$2a$10$MiV3iG3o3TvXXTu1VNOa5uoUnR4N1nFDU0rso5rWCouc8NrAk5mQK', '李娜', '13800000003', 'lina@campus.edu',     NULL, 0, 1),
(4, 'wangfang2024', '$2a$10$MiV3iG3o3TvXXTu1VNOa5uoUnR4N1nFDU0rso5rWCouc8NrAk5mQK', '王芳', '13800000004', 'wangfang@campus.edu', NULL, 0, 1),
(5, 'liuyang2023',  '$2a$10$MiV3iG3o3TvXXTu1VNOa5uoUnR4N1nFDU0rso5rWCouc8NrAk5mQK', '刘洋', '13800000005', 'liuyang@campus.edu',  NULL, 0, 1),
(6, 'chenjie2024',  '$2a$10$MiV3iG3o3TvXXTu1VNOa5uoUnR4N1nFDU0rso5rWCouc8NrAk5mQK', '陈杰', '13800000006', 'chenjie@campus.edu',  NULL, 0, 1),
(7, 'zhaoqian2023', '$2a$10$MiV3iG3o3TvXXTu1VNOa5uoUnR4N1nFDU0rso5rWCouc8NrAk5mQK', '赵倩', '13800000007', 'zhaoqian@campus.edu', NULL, 0, 1),
(8, 'sunli2024',    '$2a$10$MiV3iG3o3TvXXTu1VNOa5uoUnR4N1nFDU0rso5rWCouc8NrAk5mQK', '孙丽', '13800000008', 'sunli@campus.edu',    NULL, 0, 0);

-- ============================================
-- 2. 丢失物品表（18 条）
-- 5 分类×3~4 件，状态 0/1/2 各占 1/3，时间跨度 30 天
-- ============================================
INSERT INTO `lost_item` (`id`, `user_id`, `title`, `description`, `category`, `location`, `event_time`, `contact_info`, `images`, `status`, `create_time`) VALUES
(1, 2, '黑色钱包',       '在三食堂丢失一个黑色真皮钱包，内有身份证、银行卡和少量现金，钱包上有划痕', 'electronics', '三食堂二楼',     NOW() - INTERVAL 28 DAY, '13800000002', '["/upload/demo/wallet1.jpg"]', 2, NOW() - INTERVAL 28 DAY),
(2, 3, '学生证',         '图书馆三楼自习室遗失学生证，姓名李娜，学号2023001，夹在《高等数学》书中', 'certificate', '图书馆三楼自习室', NOW() - INTERVAL 25 DAY, '13800000003', '["/upload/demo/studentcard1.jpg"]', 1, NOW() - INTERVAL 25 DAY),
(3, 4, 'AirPods Pro',   '教学楼B栋301教室遗失白色 AirPods Pro 二代，右耳有轻微划痕，充电盒贴有卡通贴纸', 'electronics', '教学楼B栋301',    NOW() - INTERVAL 20 DAY, '13800000004', '["/upload/demo/airpods1.jpg"]', 1, NOW() - INTERVAL 20 DAY),
(4, 5, '钥匙串',         '操场丢失一串钥匙，含宿舍钥匙2把、车锁钥匙1把，钥匙扣是小熊挂件', 'other',      '操场跑道',       NOW() - INTERVAL 18 DAY, '13800000005', '["/upload/demo/keys1.jpg"]', 0, NOW() - INTERVAL 18 DAY),
(5, 6, '《数据结构》教材','自习室A栋201丢失《数据结构（C语言版）》严蔚敏版，书内有大量笔记和书签', 'book',       '自习室A栋201',   NOW() - INTERVAL 15 DAY, '13800000006', '["/upload/demo/book1.jpg"]', 0, NOW() - INTERVAL 15 DAY),
(6, 7, '蓝色雨伞',       '校门口公交站遗失一把蓝色折叠伞，伞柄有蓝色丝带标记', 'other',      '校门口公交站',   NOW() - INTERVAL 12 DAY, '13800000007', '["/upload/demo/umbrella1.jpg"]', 0, NOW() - INTERVAL 12 DAY),
(7, 2, '红色水杯',       '教学楼C栋二楼丢失红色保温杯，杯底贴有"张伟"姓名贴', 'other',      '教学楼C栋二楼',  NOW() - INTERVAL 10 DAY, '13800000002', '["/upload/demo/cup1.jpg"]', 0, NOW() - INTERVAL 10 DAY),
(8, 3, '校园卡',         '宿舍楼3号楼下遗失校园卡，卡面有磨损，背面写有李娜', 'certificate', '宿舍楼3号楼',    NOW() - INTERVAL 8 DAY,  '13800000003', '["/upload/demo/campuscard1.jpg"]', 0, NOW() - INTERVAL 8 DAY),
(9, 4, '黑色双肩包',     '图书馆一楼大厅遗失黑色双肩包，内有笔记本电脑和课本', 'other',      '图书馆一楼大厅', NOW() - INTERVAL 6 DAY,  '13800000004', '["/upload/demo/bag1.jpg"]', 0, NOW() - INTERVAL 6 DAY),
(10, 5, '眼镜',          '三食堂一楼遗失黑框近视眼镜，镜腿有修补痕迹', 'other',      '三食堂一楼',     NOW() - INTERVAL 5 DAY,  '13800000005', '["/upload/demo/glasses1.jpg"]', 0, NOW() - INTERVAL 5 DAY),
(11, 6, 'U盘',           '教学楼A栋机房遗失银色 U盘 64G，内含课程作业', 'electronics', '教学楼A栋机房',   NOW() - INTERVAL 4 DAY,  '13800000006', '["/upload/demo/usb1.jpg"]', 0, NOW() - INTERVAL 4 DAY),
(12, 7, '粉色手机壳',    '操场看台遗失粉色毛绒手机壳，内含手机', 'electronics', '操场看台',       NOW() - INTERVAL 3 DAY,  '13800000007', '["/upload/demo/phonecase1.jpg"]', 0, NOW() - INTERVAL 3 DAY),
(13, 2, '数学笔记本',    '自习室B栋301遗失数学笔记本，封面蓝色，内有微积分笔记', 'book',       '自习室B栋301',   NOW() - INTERVAL 2 DAY,  '13800000002', '["/upload/demo/notebook1.jpg"]', 0, NOW() - INTERVAL 2 DAY),
(14, 3, '蓝牙耳机',      '体育馆遗失黑色蓝牙耳机，充电盒有刮痕', 'electronics', '体育馆',         NOW() - INTERVAL 1 DAY,  '13800000003', '["/upload/demo/bluetooth1.jpg"]', 0, NOW() - INTERVAL 1 DAY),
(15, 4, '外套',          '图书馆二楼遗失米色风衣外套，口袋有纸巾', 'clothing',   '图书馆二楼',     NOW() - INTERVAL 18 HOUR, '13800000004', '["/upload/demo/coat1.jpg"]', 0, NOW() - INTERVAL 18 HOUR),
(16, 5, '计算器',        '教学楼D栋考试后遗失卡西欧 fx-991 计算器', 'electronics', '教学楼D栋205',   NOW() - INTERVAL 12 HOUR, '13800000005', '["/upload/demo/calculator1.jpg"]', 0, NOW() - INTERVAL 12 HOUR),
(17, 6, '英语词典',      '自习室C栋遗失《牛津高阶词典》第9版，书皮有折痕', 'book',       '自习室C栋',      NOW() - INTERVAL 6 HOUR,  '13800000006', '["/upload/demo/dictionary1.jpg"]', 0, NOW() - INTERVAL 6 HOUR),
(18, 7, '运动手环',      '操场遗失黑色小米运动手环，表带有磨损', 'electronics', '操场足球场',     NOW() - INTERVAL 2 HOUR,  '13800000007', '["/upload/demo/bracelet1.jpg"]', 0, NOW() - INTERVAL 2 HOUR);

-- ============================================
-- 3. 拾到物品表（18 条）
-- 与 lost_item 部分配对，构造完整匹配链路
-- 含 4 对匹配物品（status=2 已关闭）、1 对已匹配（status=1）、其余待处理（status=0）
-- ============================================
INSERT INTO `found_item` (`id`, `user_id`, `title`, `description`, `category`, `location`, `event_time`, `contact_info`, `images`, `status`, `create_time`) VALUES
-- 4 对匹配物品（status=2 已关闭）
(1, 6, '黑色钱包',       '在三食堂二楼捡到黑色真皮钱包，内有证件和现金', 'electronics', '三食堂二楼',     NOW() - INTERVAL 27 DAY, '13800000006', '["/upload/demo/wallet2.jpg"]', 2, NOW() - INTERVAL 27 DAY),
(2, 7, '学生证',         '图书馆三楼捡到学生证，姓名李娜', 'certificate', '图书馆三楼自习室', NOW() - INTERVAL 24 DAY, '13800000007', '["/upload/demo/studentcard2.jpg"]', 2, NOW() - INTERVAL 24 DAY),
(3, 5, 'AirPods Pro',   '教学楼B栋301捡到白色 AirPods Pro，右耳有划痕', 'electronics', '教学楼B栋301',    NOW() - INTERVAL 19 DAY, '13800000005', '["/upload/demo/airpods2.jpg"]', 2, NOW() - INTERVAL 19 DAY),
(4, 2, '蓝色雨伞',       '校门口公交站捡到蓝色折叠伞，伞柄有丝带', 'other',      '校门口公交站',   NOW() - INTERVAL 11 DAY, '13800000002', '["/upload/demo/umbrella2.jpg"]', 2, NOW() - INTERVAL 11 DAY),
-- 已匹配但未关闭（status=1）
(5, 3, '钥匙串',         '操场捡到一串钥匙，含小熊挂件', 'other',      '操场跑道',       NOW() - INTERVAL 9 DAY,  '13800000003', '["/upload/demo/keys2.jpg"]', 1, NOW() - INTERVAL 9 DAY),
-- 待处理物品（status=0）
(6, 4, '《数据结构》教材','自习室A栋201捡到《数据结构》严蔚敏版，有笔记', 'book',       '自习室A栋201',   NOW() - INTERVAL 14 DAY, '13800000004', '["/upload/demo/book2.jpg"]', 0, NOW() - INTERVAL 14 DAY),
(7, 5, '红色水杯',       '教学楼C栋二楼捡到红色保温杯，杯底有姓名贴', 'other',      '教学楼C栋二楼',  NOW() - INTERVAL 9 DAY,  '13800000005', '["/upload/demo/cup2.jpg"]', 0, NOW() - INTERVAL 9 DAY),
(8, 6, '校园卡',         '宿舍楼3号楼下捡到校园卡，卡面磨损', 'certificate', '宿舍楼3号楼',    NOW() - INTERVAL 7 DAY,  '13800000006', '["/upload/demo/campuscard2.jpg"]', 0, NOW() - INTERVAL 7 DAY),
(9, 7, '黑色双肩包',     '图书馆一楼捡到黑色双肩包，内有笔记本', 'other',      '图书馆一楼大厅', NOW() - INTERVAL 5 DAY,  '13800000007', '["/upload/demo/bag2.jpg"]', 0, NOW() - INTERVAL 5 DAY),
(10, 2, '眼镜',          '三食堂一楼捡到黑框近视眼镜，镜腿有修补', 'other',      '三食堂一楼',     NOW() - INTERVAL 4 DAY,  '13800000002', '["/upload/demo/glasses2.jpg"]', 0, NOW() - INTERVAL 4 DAY),
(11, 3, 'U盘',           '教学楼A栋机房捡到银色 U盘 64G', 'electronics', '教学楼A栋机房',   NOW() - INTERVAL 3 DAY,  '13800000003', '["/upload/demo/usb2.jpg"]', 0, NOW() - INTERVAL 3 DAY),
(12, 4, '粉色手机壳',    '操场看台捡到粉色毛绒手机壳', 'electronics', '操场看台',       NOW() - INTERVAL 2 DAY,  '13800000004', '["/upload/demo/phonecase2.jpg"]', 0, NOW() - INTERVAL 2 DAY),
(13, 5, '数学笔记本',    '自习室B栋301捡到蓝色封面数学笔记本', 'book',       '自习室B栋301',   NOW() - INTERVAL 1 DAY,  '13800000005', '["/upload/demo/notebook2.jpg"]', 0, NOW() - INTERVAL 1 DAY),
(14, 6, '蓝牙耳机',      '体育馆捡到黑色蓝牙耳机，充电盒有刮痕', 'electronics', '体育馆',         NOW() - INTERVAL 20 HOUR, '13800000006', '["/upload/demo/bluetooth2.jpg"]', 0, NOW() - INTERVAL 20 HOUR),
(15, 7, '外套',          '图书馆二楼捡到米色风衣外套', 'clothing',   '图书馆二楼',     NOW() - INTERVAL 16 HOUR, '13800000007', '["/upload/demo/coat2.jpg"]', 0, NOW() - INTERVAL 16 HOUR),
(16, 2, '计算器',        '教学楼D栋捡到卡西欧 fx-991 计算器', 'electronics', '教学楼D栋205',   NOW() - INTERVAL 10 HOUR, '13800000002', '["/upload/demo/calculator2.jpg"]', 0, NOW() - INTERVAL 10 HOUR),
(17, 3, '英语词典',      '自习室C栋捡到《牛津高阶词典》第9版', 'book',       '自习室C栋',      NOW() - INTERVAL 5 HOUR,  '13800000003', '["/upload/demo/dictionary2.jpg"]', 0, NOW() - INTERVAL 5 HOUR),
(18, 4, '运动手环',      '操场捡到黑色小米运动手环', 'electronics', '操场足球场',     NOW() - INTERVAL 1 HOUR,  '13800000004', '["/upload/demo/bracelet2.jpg"]', 0, NOW() - INTERVAL 1 HOUR);

-- ============================================
-- 4. 认领表（12 条）
-- 构造 4 对完整匹配链路：丢失↔拾到↔认领↔完成
-- 状态分布：4 已完成 + 1 已通过 + 4 待审核 + 3 已拒绝
-- ============================================
INSERT INTO `claim` (`id`, `lost_item_id`, `found_item_id`, `claimant_id`, `respondent_id`, `match_score`, `status`, `remark`, `create_time`) VALUES
-- 4 对已完成（status=3 COMPLETED）
(1, 1, 1, 2, 6, 0.92, 3, '钱包特征完全匹配，已确认领取', NOW() - INTERVAL 26 DAY),
(2, 2, 2, 3, 7, 0.95, 3, '学生证姓名学号一致', NOW() - INTERVAL 23 DAY),
(3, 3, 3, 4, 5, 0.88, 3, 'AirPods 划痕位置吻合', NOW() - INTERVAL 18 DAY),
(4, 6, 4, 7, 2, 0.85, 3, '雨伞丝带标记一致', NOW() - INTERVAL 10 DAY),
-- 1 对已批准待完成（status=1 APPROVED）
(5, 4, 5, 5, 3, 0.78, 1, '钥匙和小熊挂件匹配', NOW() - INTERVAL 8 DAY),
-- 3 对待审批（status=0 PENDING）
(6, 5, 6, 6, 4, 0.72, 0, '教材版本和笔记特征相似', NOW() - INTERVAL 2 DAY),
(7, 7, 7, 2, 5, 0.68, 0, '水杯姓名贴一致', NOW() - INTERVAL 1 DAY),
(8, 9, 9, 4, 7, 0.75, 0, '双肩包内笔记本特征匹配', NOW() - INTERVAL 12 HOUR),
-- 2 对已拒绝（status=2 REJECTED，同一物品多认领场景）
(9, 8, 8, 3, 6, 0.45, 2, '校园卡姓名不符', NOW() - INTERVAL 3 DAY),
(10, 8, 8, 7, 6, 0.52, 2, '描述细节有出入', NOW() - INTERVAL 2 DAY),
(11, 10, 10, 5, 2, 0.41, 2, '眼镜特征不完全匹配', NOW() - INTERVAL 1 DAY),
(12, 11, 11, 6, 3, 0.62, 0, 'U盘容量一致', NOW() - INTERVAL 6 HOUR);

-- ============================================
-- 5. 评论表（25 条）
-- 多级回复，parent_id 表示父评论
-- ============================================
INSERT INTO `comment` (`id`, `item_type`, `item_id`, `user_id`, `parent_id`, `content`, `create_time`) VALUES
(1,  'lost',  1, 6, NULL, '我也在三食堂看到过类似的钱包，希望你能尽快找到', NOW() - INTERVAL 27 DAY),
(2,  'lost',  1, 2, 1,    '谢谢！已经有人捡到了，正在联系中', NOW() - INTERVAL 27 DAY),
(3,  'lost',  2, 7, NULL, '学生证补办很麻烦，希望能找到', NOW() - INTERVAL 24 DAY),
(4,  'lost',  3, 5, NULL, 'AirPods 容易丢，建议买个挂绳', NOW() - INTERVAL 19 DAY),
(5,  'lost',  3, 4, 4,    '谢谢建议，下次会注意', NOW() - INTERVAL 19 DAY),
(6,  'lost',  4, 3, NULL, '钥匙串小熊挂件很特别，应该容易辨认', NOW() - INTERVAL 17 DAY),
(7,  'found', 1, 2, NULL, '这个钱包是不是在三食堂丢的？联系我', NOW() - INTERVAL 26 DAY),
(8,  'found', 1, 6, 7,    '是的，已经联系上了，谢谢', NOW() - INTERVAL 26 DAY),
(9,  'found', 2, 3, NULL, '这是我丢的学生证，已发起认领', NOW() - INTERVAL 23 DAY),
(10, 'found', 3, 4, NULL, 'AirPods 划痕位置和我的一样，已认领', NOW() - INTERVAL 18 DAY),
(11, 'found', 4, 7, NULL, '雨伞丝带标记一致，已认领', NOW() - INTERVAL 10 DAY),
(12, 'lost',  5, 4, NULL, '数据结构教材很常用，大家帮忙留意', NOW() - INTERVAL 14 DAY),
(13, 'lost',  6, 2, NULL, '蓝色雨伞比较常见，描述丝带标记很关键', NOW() - INTERVAL 11 DAY),
(14, 'lost',  7, 5, NULL, '红色水杯姓名贴是好线索', NOW() - INTERVAL 9 DAY),
(15, 'lost',  9, 6, NULL, '双肩包里的笔记本可能有名字', NOW() - INTERVAL 5 DAY),
(16, 'found', 6, 6, NULL, '这本教材有笔记，应该是同一个人的', NOW() - INTERVAL 13 DAY),
(17, 'found', 7, 2, NULL, '水杯姓名贴是张伟，应该是 lost id=7', NOW() - INTERVAL 8 DAY),
(18, 'found', 9, 4, NULL, '双肩包里有课本，可能是 lost id=9', NOW() - INTERVAL 4 DAY),
(19, 'lost',  11, 3, NULL, 'U盘里有课程作业，很重要', NOW() - INTERVAL 3 DAY),
(20, 'lost',  13, 2, NULL, '数学笔记记得很辛苦，希望能找回', NOW() - INTERVAL 1 DAY),
(21, 'found', 13, 5, NULL, '笔记本封面蓝色，有微积分笔记', NOW() - INTERVAL 22 HOUR),
(22, 'lost',  15, 4, NULL, '外套口袋有纸巾，可能是在图书馆丢的', NOW() - INTERVAL 17 HOUR),
(23, 'found', 15, 7, NULL, '风衣米色，口袋有纸巾', NOW() - INTERVAL 15 HOUR),
(24, 'lost',  17, 6, NULL, '词典很厚，掉落应该会被注意到', NOW() - INTERVAL 5 HOUR),
(25, 'found', 17, 3, NULL, '牛津高阶第9版，书皮有折痕', NOW() - INTERVAL 4 HOUR);

-- ============================================
-- 6. 收藏表（20 条）
-- ============================================
INSERT INTO `favorite` (`id`, `user_id`, `item_type`, `item_id`, `create_time`) VALUES
(1,  2, 'lost',  3, NOW() - INTERVAL 20 DAY),
(2,  2, 'found', 3, NOW() - INTERVAL 19 DAY),
(3,  3, 'lost',  4, NOW() - INTERVAL 18 DAY),
(4,  3, 'found', 5, NOW() - INTERVAL 9 DAY),
(5,  4, 'lost',  5, NOW() - INTERVAL 15 DAY),
(6,  4, 'found', 6, NOW() - INTERVAL 14 DAY),
(7,  5, 'lost',  7, NOW() - INTERVAL 10 DAY),
(8,  5, 'found', 7, NOW() - INTERVAL 9 DAY),
(9,  6, 'lost',  9, NOW() - INTERVAL 6 DAY),
(10, 6, 'found', 9, NOW() - INTERVAL 5 DAY),
(11, 7, 'lost',  11, NOW() - INTERVAL 4 DAY),
(12, 7, 'found', 11, NOW() - INTERVAL 3 DAY),
(13, 2, 'lost',  13, NOW() - INTERVAL 2 DAY),
(14, 3, 'found', 13, NOW() - INTERVAL 1 DAY),
(15, 4, 'lost',  15, NOW() - INTERVAL 18 HOUR),
(16, 5, 'found', 15, NOW() - INTERVAL 16 HOUR),
(17, 6, 'lost',  17, NOW() - INTERVAL 5 HOUR),
(18, 7, 'found', 17, NOW() - INTERVAL 4 HOUR),
(19, 2, 'lost',  10, NOW() - INTERVAL 5 DAY),
(20, 3, 'found', 10, NOW() - INTERVAL 4 DAY);

-- ============================================
-- 7. 消息表（30 条）
-- 系统通知 + 认领通知 + 私信，已读/未读
-- ============================================
INSERT INTO `message` (`id`, `sender_id`, `receiver_id`, `type`, `title`, `content`, `is_read`, `create_time`) VALUES
-- 系统通知
(1,  1, 2, 'system', '欢迎注册', '欢迎来到校园失物招领系统，请完善个人信息', 1, NOW() - INTERVAL 28 DAY),
(2,  1, 3, 'system', '欢迎注册', '欢迎来到校园失物招领系统，请完善个人信息', 1, NOW() - INTERVAL 25 DAY),
(3,  1, 4, 'system', '欢迎注册', '欢迎来到校园失物招领系统，请完善个人信息', 1, NOW() - INTERVAL 20 DAY),
(4,  1, 5, 'system', '信用积分扣分通知', '您因恶意认领被扣除 30 信用积分', 1, NOW() - INTERVAL 15 DAY),
(5,  1, 2, 'system', '认领完成', '您的丢失物品"黑色钱包"认领已完成，获得 20 信用积分', 1, NOW() - INTERVAL 26 DAY),
(6,  1, 6, 'system', '认领完成', '您拾到的"黑色钱包"认领已完成，获得 20 信用积分', 1, NOW() - INTERVAL 26 DAY),
(7,  1, 3, 'system', '认领完成', '您的丢失物品"学生证"认领已完成，获得 20 信用积分', 1, NOW() - INTERVAL 23 DAY),
(8,  1, 4, 'system', '认领完成', '您的丢失物品"AirPods Pro"认领已完成，获得 20 信用积分', 1, NOW() - INTERVAL 18 DAY),
(9,  1, 7, 'system', '认领完成', '您的丢失物品"蓝色雨伞"认领已完成，获得 20 信用积分', 1, NOW() - INTERVAL 10 DAY),
-- 认领通知
(10, 2, 6, 'claim', '新认领申请', '有人申请认领您拾到的"黑色钱包"', 1, NOW() - INTERVAL 26 DAY),
(11, 3, 7, 'claim', '新认领申请', '有人申请认领您拾到的"学生证"', 1, NOW() - INTERVAL 23 DAY),
(12, 4, 5, 'claim', '新认领申请', '有人申请认领您拾到的"AirPods Pro"', 1, NOW() - INTERVAL 18 DAY),
(13, 7, 2, 'claim', '新认领申请', '有人申请认领您拾到的"蓝色雨伞"', 1, NOW() - INTERVAL 10 DAY),
(14, 5, 3, 'claim', '新认领申请', '有人申请认领您拾到的"钥匙串"', 1, NOW() - INTERVAL 8 DAY),
(15, 6, 4, 'claim', '新认领申请', '有人申请认领您拾到的"《数据结构》教材"', 0, NOW() - INTERVAL 2 DAY),
(16, 2, 5, 'claim', '新认领申请', '有人申请认领您拾到的"红色水杯"', 0, NOW() - INTERVAL 1 DAY),
-- 认领状态通知
(17, 6, 2, 'claim', '认领已通过', '您的"黑色钱包"认领申请已通过', 1, NOW() - INTERVAL 26 DAY),
(18, 7, 3, 'claim', '认领已通过', '您的"学生证"认领申请已通过', 1, NOW() - INTERVAL 23 DAY),
(19, 5, 4, 'claim', '认领已通过', '您的"AirPods Pro"认领申请已通过', 1, NOW() - INTERVAL 18 DAY),
(20, 2, 7, 'claim', '认领已通过', '您的"蓝色雨伞"认领申请已通过', 1, NOW() - INTERVAL 10 DAY),
-- 拒绝通知
(21, 6, 3, 'claim', '认领被拒绝', '您的"校园卡"认领申请被拒绝：姓名不符', 1, NOW() - INTERVAL 3 DAY),
(22, 6, 7, 'claim', '认领被拒绝', '您的"校园卡"认领申请被拒绝：描述有出入', 1, NOW() - INTERVAL 2 DAY),
(23, 2, 5, 'claim', '认领被拒绝', '您的"眼镜"认领申请被拒绝：特征不匹配', 1, NOW() - INTERVAL 1 DAY),
-- 私信
(24, 2, 6, 'chat', '关于钱包', '你好，你捡到的钱包是我的，特征都对得上', 1, NOW() - INTERVAL 27 DAY),
(25, 6, 2, 'chat', '回复：关于钱包', '好的，已发起认领，请通过', 1, NOW() - INTERVAL 27 DAY),
(26, 3, 7, 'chat', '关于学生证', '学生证是我的，学号2023001', 1, NOW() - INTERVAL 24 DAY),
(27, 4, 5, 'chat', '关于AirPods', '右耳划痕是关键特征', 1, NOW() - INTERVAL 19 DAY),
-- 未读私信
(28, 6, 4, 'chat', '关于教材', '这本数据结构是不是你的？', 0, NOW() - INTERVAL 1 DAY),
(29, 5, 2, 'chat', '关于水杯', '水杯姓名贴是张伟对吗', 0, NOW() - INTERVAL 12 HOUR),
(30, 3, 6, 'chat', '关于U盘', 'U盘里的作业是什么课程', 0, NOW() - INTERVAL 3 HOUR);

-- ============================================
-- 8. 信用积分日志表（40 条）
-- 初始 100，发布+10，认领完成+20，恶意 -30
-- ============================================
INSERT INTO `user_credit_log` (`id`, `user_id`, `change_score`, `after_score`, `reason`, `ref_id`, `create_time`) VALUES
-- 用户2 张伟：100 → 100(init) → 110(+10) → 130(+20) → 140(+10) → 160(+20) → 170(+10) → 180(+10) → 190(+10) → 200(+10)
(1, 2, 0,   100, '初始积分',            NULL, NOW() - INTERVAL 28 DAY),
(2, 2, 10,  110, '发布拾到物品"蓝色雨伞"',  4,    NOW() - INTERVAL 11 DAY),
(3, 2, 20,  130, '蓝色雨伞认领完成奖励',  4,    NOW() - INTERVAL 10 DAY),
(4, 2, 10,  140, '发布拾到物品"红色水杯"', 7,    NOW() - INTERVAL 9 DAY),
(5, 2, 20,  160, '黑色钱包认领完成奖励（拾到方）', 1, NOW() - INTERVAL 26 DAY),
-- 用户3 李娜：100 → 100(init) → 120(+20) → 130(+10) → 150(+20) → 160(+10) → 170(+10) → 190(+20)
(6, 3, 0,   100, '初始积分',            NULL, NOW() - INTERVAL 25 DAY),
(7, 3, 20,  120, '学生证认领完成奖励',    2,    NOW() - INTERVAL 23 DAY),
(8, 3, 10,  130, '发布拾到物品"红色水杯"', 7,    NOW() - INTERVAL 9 DAY),
(9, 3, 20,  150, '蓝色雨伞认领完成奖励（拾到方）', 4, NOW() - INTERVAL 10 DAY),
-- 用户4 王芳：100 → 100(init) → 120(+20) → 130(+10) → 140(+10) → 150(+10) → 170(+20)
(10, 4, 0,  100, '初始积分',            NULL, NOW() - INTERVAL 20 DAY),
(11, 4, 20, 120, 'AirPods Pro 认领完成奖励', 3, NOW() - INTERVAL 18 DAY),
(12, 4, 10, 130, '发布拾到物品"《数据结构》教材"', 6, NOW() - INTERVAL 14 DAY),
-- 用户5 刘洋：100 → 100(init) → 70(-30恶意) → 80(+10) → 90(+10) → 100(+10) → 110(+10)
(13, 5, 0,  100, '初始积分',            NULL, NOW() - INTERVAL 18 DAY),
(14, 5, -30, 70, '恶意认领扣分',         NULL, NOW() - INTERVAL 15 DAY),
(15, 5, 10, 80,  '发布拾到物品"AirPods Pro"', 3, NOW() - INTERVAL 19 DAY),
(16, 5, 10, 90,  '发布拾到物品"红色水杯"', 7,    NOW() - INTERVAL 9 DAY),
-- 用户6 陈杰：100 → 100(init) → 120(+20) → 130(+10) → 140(+10) → 150(+10) → 160(+10)
(17, 6, 0,  100, '初始积分',            NULL, NOW() - INTERVAL 28 DAY),
(18, 6, 20, 120, '黑色钱包认领完成奖励（拾到方）', 1, NOW() - INTERVAL 26 DAY),
(19, 6, 10, 130, '发布拾到物品"黑色钱包"', 1,    NOW() - INTERVAL 27 DAY),
(20, 6, 10, 140, '发布拾到物品"校园卡"', 8,      NOW() - INTERVAL 7 DAY),
-- 用户7 赵倩：100 → 100(init) → 120(+20) → 130(+10) → 140(+10) → 150(+10) → 160(+10)
(21, 7, 0,  100, '初始积分',            NULL, NOW() - INTERVAL 28 DAY),
(22, 7, 20, 120, '蓝色雨伞认领完成奖励', 4,      NOW() - INTERVAL 10 DAY),
(23, 7, 10, 130, '发布拾到物品"学生证"', 2,      NOW() - INTERVAL 24 DAY),
(24, 7, 10, 140, '发布拾到物品"黑色双肩包"', 9,  NOW() - INTERVAL 5 DAY),
-- 补充更多日志让排行丰富
(25, 2, 10, 170, '发布拾到物品"眼镜"', 10, NOW() - INTERVAL 4 DAY),
(26, 3, 10, 160, '发布拾到物品"U盘"', 11, NOW() - INTERVAL 3 DAY),
(27, 4, 10, 140, '发布拾到物品"粉色手机壳"', 12, NOW() - INTERVAL 2 DAY),
(28, 5, 10, 100, '发布拾到物品"数学笔记本"', 13, NOW() - INTERVAL 1 DAY),
(29, 6, 10, 150, '发布拾到物品"蓝牙耳机"', 14, NOW() - INTERVAL 20 HOUR),
(30, 7, 10, 150, '发布拾到物品"外套"', 15, NOW() - INTERVAL 16 HOUR),
(31, 2, 10, 180, '发布拾到物品"计算器"', 16, NOW() - INTERVAL 10 HOUR),
(32, 3, 10, 170, '发布拾到物品"英语词典"', 17, NOW() - INTERVAL 5 HOUR),
(33, 4, 10, 150, '发布拾到物品"运动手环"', 18, NOW() - INTERVAL 1 HOUR),
(34, 2, 10, 190, '发布拾到物品"眼镜"', 10, NOW() - INTERVAL 4 DAY),
(35, 3, 20, 190, '学生证认领完成奖励（拾到方）', 2, NOW() - INTERVAL 23 DAY),
(36, 4, 20, 170, 'AirPods 认领完成奖励（拾到方）', 3, NOW() - INTERVAL 18 DAY),
(37, 2, 10, 200, '发布拾到物品"红色水杯"', 7, NOW() - INTERVAL 9 DAY),
(38, 5, 10, 110, '发布拾到物品"钥匙串"', 5, NOW() - INTERVAL 9 DAY),
(39, 6, 10, 160, '发布拾到物品"校园卡"', 8, NOW() - INTERVAL 7 DAY),
(40, 7, 10, 160, '发布拾到物品"黑色双肩包"', 9, NOW() - INTERVAL 5 DAY);

-- ============================================
-- 9. 公告表（6 条）
-- ============================================
INSERT INTO `announcement` (`id`, `title`, `content`, `status`, `publisher_id`, `create_time`) VALUES
(1, '欢迎使用校园失物招领系统', '本系统采用 TF-IDF 智能匹配算法，帮助大家快速找回失物。发布丢失或拾到物品时请尽量详细描述物品特征、地点和时间。', 1, 1, NOW() - INTERVAL 30 DAY),
(2, '关于认领流程的说明', '认领流程：1.浏览拾到物品列表 2.点击"认领"提交申请 3.拾到者审核 4.审核通过后确认完成 5.双方获得信用积分奖励', 1, 1, NOW() - INTERVAL 25 DAY),
(3, '信用积分规则', '初始积分100。发布拾到物品+10，认领完成+20，恶意认领-30。积分低于60将限制部分功能。', 1, 1, NOW() - INTERVAL 20 DAY),
(4, '系统维护通知', '系统将于本周日凌晨2-4点进行维护，期间可能短暂不可用', 1, 1, NOW() - INTERVAL 10 DAY),
(5, '防范虚假认领提醒', '近期发现个别虚假认领行为，请大家在认领时提供准确的物品特征描述，系统已加强审核机制', 1, 1, NOW() - INTERVAL 5 DAY),
(6, '期末考试周失物高发提醒', '期末考试周是失物高发期，请大家保管好个人物品，遗失或拾到请及时发布', 1, 1, NOW() - INTERVAL 2 DAY);

-- ============================================
-- 10. 操作日志表（30 条）
-- ============================================
INSERT INTO `operation_log` (`id`, `user_id`, `operation`, `method`, `params`, `ip`, `create_time`) VALUES
(1,  2, '登录',     'POST /api/user/login',    '{"username":"zhangwei2024"}', '192.168.1.10', NOW() - INTERVAL 28 DAY),
(2,  2, '发布丢失', 'POST /api/lost/publish',  '{"title":"黑色钱包"}',        '192.168.1.10', NOW() - INTERVAL 28 DAY),
(3,  6, '登录',     'POST /api/user/login',    '{"username":"chenjie2024"}',  '192.168.1.11', NOW() - INTERVAL 27 DAY),
(4,  6, '发布拾到', 'POST /api/found/publish', '{"title":"黑色钱包"}',        '192.168.1.11', NOW() - INTERVAL 27 DAY),
(5,  2, '发起认领', 'POST /api/claim/create',  '{"foundItemId":1}',           '192.168.1.10', NOW() - INTERVAL 26 DAY),
(6,  6, '审核认领', 'POST /api/claim/approve', '{"claimId":1}',               '192.168.1.11', NOW() - INTERVAL 26 DAY),
(7,  2, '完成认领', 'POST /api/claim/complete','{"claimId":1}',               '192.168.1.10', NOW() - INTERVAL 26 DAY),
(8,  3, '登录',     'POST /api/user/login',    '{"username":"lina2023"}',     '192.168.1.12', NOW() - INTERVAL 25 DAY),
(9,  3, '发布丢失', 'POST /api/lost/publish',  '{"title":"学生证"}',          '192.168.1.12', NOW() - INTERVAL 25 DAY),
(10, 7, '发布拾到', 'POST /api/found/publish', '{"title":"学生证"}',          '192.168.1.13', NOW() - INTERVAL 24 DAY),
(11, 3, '发起认领', 'POST /api/claim/create',  '{"foundItemId":2}',           '192.168.1.12', NOW() - INTERVAL 23 DAY),
(12, 7, '审核认领', 'POST /api/claim/approve', '{"claimId":2}',               '192.168.1.13', NOW() - INTERVAL 23 DAY),
(13, 1, '登录',     'POST /api/user/login',    '{"username":"admin"}',        '192.168.1.1',  NOW() - INTERVAL 24 DAY),
(14, 1, '发布公告', 'POST /api/announcement/add','{"title":"关于认领流程的说明"}','192.168.1.1', NOW() - INTERVAL 25 DAY),
(15, 4, '登录',     'POST /api/user/login',    '{"username":"wangfang2024"}', '192.168.1.14', NOW() - INTERVAL 20 DAY),
(16, 4, '发布丢失', 'POST /api/lost/publish',  '{"title":"AirPods Pro"}',     '192.168.1.14', NOW() - INTERVAL 20 DAY),
(17, 5, '发布拾到', 'POST /api/found/publish', '{"title":"AirPods Pro"}',     '192.168.1.15', NOW() - INTERVAL 19 DAY),
(18, 4, '发起认领', 'POST /api/claim/create',  '{"foundItemId":3}',           '192.168.1.14', NOW() - INTERVAL 18 DAY),
(19, 5, '审核认领', 'POST /api/claim/approve', '{"claimId":3}',               '192.168.1.15', NOW() - INTERVAL 18 DAY),
(20, 5, '恶意认领', 'POST /api/claim/create',  '{"foundItemId":8,"fraud":true}','192.168.1.15', NOW() - INTERVAL 15 DAY),
(21, 1, '扣除积分', 'POST /api/credit/deduct', '{"userId":5,"score":-30}',    '192.168.1.1',  NOW() - INTERVAL 15 DAY),
(22, 7, '发布丢失', 'POST /api/lost/publish',  '{"title":"蓝色雨伞"}',        '192.168.1.13', NOW() - INTERVAL 12 DAY),
(23, 2, '发布拾到', 'POST /api/found/publish', '{"title":"蓝色雨伞"}',        '192.168.1.10', NOW() - INTERVAL 11 DAY),
(24, 7, '发起认领', 'POST /api/claim/create',  '{"foundItemId":4}',           '192.168.1.13', NOW() - INTERVAL 10 DAY),
(25, 2, '审核认领', 'POST /api/claim/approve', '{"claimId":4}',               '192.168.1.10', NOW() - INTERVAL 10 DAY),
(26, 6, '搜索',     'GET /api/search',         '{"keyword":"钱包"}',          '192.168.1.11', NOW() - INTERVAL 5 DAY),
(27, 3, '收藏',     'POST /api/favorite/add',  '{"itemType":"lost","itemId":4}','192.168.1.12', NOW() - INTERVAL 4 DAY),
(28, 1, '查看日志', 'GET /api/log/list',       '{}',                          '192.168.1.1',  NOW() - INTERVAL 3 DAY),
(29, 1, '导出数据', 'GET /api/export/items',   '{"type":"lost"}',             '192.168.1.1',  NOW() - INTERVAL 2 DAY),
(30, 2, '修改资料', 'PUT /api/user/profile',   '{"phone":"13800000002"}',     '192.168.1.10', NOW() - INTERVAL 1 DAY);

-- ============================================
-- 11. 浏览历史表（25 条）
-- 用于推荐
-- ============================================
INSERT INTO `user_view_history` (`id`, `user_id`, `item_type`, `item_id`, `view_time`) VALUES
(1,  2, 'lost',  3,  NOW() - INTERVAL 20 DAY),
(2,  2, 'found', 3,  NOW() - INTERVAL 19 DAY),
(3,  2, 'lost',  5,  NOW() - INTERVAL 15 DAY),
(4,  2, 'found', 6,  NOW() - INTERVAL 14 DAY),
(5,  2, 'lost',  10, NOW() - INTERVAL 5 DAY),
(6,  3, 'lost',  4,  NOW() - INTERVAL 18 DAY),
(7,  3, 'found', 5,  NOW() - INTERVAL 9 DAY),
(8,  3, 'lost',  11, NOW() - INTERVAL 3 DAY),
(9,  3, 'found', 11, NOW() - INTERVAL 3 DAY),
(10, 4, 'lost',  5,  NOW() - INTERVAL 15 DAY),
(11, 4, 'found', 6,  NOW() - INTERVAL 14 DAY),
(12, 4, 'lost',  9,  NOW() - INTERVAL 6 DAY),
(13, 4, 'found', 9,  NOW() - INTERVAL 5 DAY),
(14, 5, 'lost',  7,  NOW() - INTERVAL 10 DAY),
(15, 5, 'found', 7,  NOW() - INTERVAL 9 DAY),
(16, 5, 'lost',  13, NOW() - INTERVAL 1 DAY),
(17, 6, 'lost',  9,  NOW() - INTERVAL 6 DAY),
(18, 6, 'found', 9,  NOW() - INTERVAL 5 DAY),
(19, 6, 'lost',  17, NOW() - INTERVAL 5 HOUR),
(20, 6, 'found', 17, NOW() - INTERVAL 4 HOUR),
(21, 7, 'lost',  11, NOW() - INTERVAL 4 DAY),
(22, 7, 'found', 11, NOW() - INTERVAL 3 DAY),
(23, 7, 'lost',  15, NOW() - INTERVAL 18 HOUR),
(24, 7, 'found', 15, NOW() - INTERVAL 16 HOUR),
(25, 2, 'lost',  6,  NOW() - INTERVAL 12 DAY);

-- ============================================
-- 12. 物品位置表（36 条）
-- 校园范围内经纬度，假设校园中心 116.397, 39.999
-- ============================================
INSERT INTO `item_location` (`id`, `item_type`, `item_id`, `longitude`, `latitude`, `address`, `create_time`) VALUES
-- 丢失物品 1-18
(1,  'lost', 1,  116.3975, 39.9985, '三食堂二楼',       NOW() - INTERVAL 28 DAY),
(2,  'lost', 2,  116.3960, 40.0005, '图书馆三楼自习室', NOW() - INTERVAL 25 DAY),
(3,  'lost', 3,  116.3980, 39.9990, '教学楼B栋301',     NOW() - INTERVAL 20 DAY),
(4,  'lost', 4,  116.3990, 40.0010, '操场跑道',         NOW() - INTERVAL 18 DAY),
(5,  'lost', 5,  116.3955, 40.0000, '自习室A栋201',     NOW() - INTERVAL 15 DAY),
(6,  'lost', 6,  116.4000, 39.9980, '校门口公交站',     NOW() - INTERVAL 12 DAY),
(7,  'lost', 7,  116.3985, 39.9995, '教学楼C栋二楼',    NOW() - INTERVAL 10 DAY),
(8,  'lost', 8,  116.3995, 40.0015, '宿舍楼3号楼',      NOW() - INTERVAL 8 DAY),
(9,  'lost', 9,  116.3965, 40.0010, '图书馆一楼大厅',   NOW() - INTERVAL 6 DAY),
(10, 'lost', 10, 116.3978, 39.9988, '三食堂一楼',       NOW() - INTERVAL 5 DAY),
(11, 'lost', 11, 116.3970, 40.0000, '教学楼A栋机房',    NOW() - INTERVAL 4 DAY),
(12, 'lost', 12, 116.3992, 40.0008, '操场看台',         NOW() - INTERVAL 3 DAY),
(13, 'lost', 13, 116.3958, 40.0002, '自习室B栋301',     NOW() - INTERVAL 2 DAY),
(14, 'lost', 14, 116.3982, 40.0012, '体育馆',           NOW() - INTERVAL 1 DAY),
(15, 'lost', 15, 116.3962, 40.0008, '图书馆二楼',       NOW() - INTERVAL 18 HOUR),
(16, 'lost', 16, 116.3988, 39.9992, '教学楼D栋205',     NOW() - INTERVAL 12 HOUR),
(17, 'lost', 17, 116.3952, 40.0005, '自习室C栋',        NOW() - INTERVAL 6 HOUR),
(18, 'lost', 18, 116.3995, 40.0013, '操场足球场',       NOW() - INTERVAL 2 HOUR),
-- 拾到物品 1-18
(19, 'found', 1, 116.3975, 39.9985, '三食堂二楼',       NOW() - INTERVAL 27 DAY),
(20, 'found', 2, 116.3960, 40.0005, '图书馆三楼自习室', NOW() - INTERVAL 24 DAY),
(21, 'found', 3, 116.3980, 39.9990, '教学楼B栋301',     NOW() - INTERVAL 19 DAY),
(22, 'found', 4, 116.4000, 39.9980, '校门口公交站',     NOW() - INTERVAL 11 DAY),
(23, 'found', 5, 116.3990, 40.0010, '操场跑道',         NOW() - INTERVAL 9 DAY),
(24, 'found', 6, 116.3955, 40.0000, '自习室A栋201',     NOW() - INTERVAL 14 DAY),
(25, 'found', 7, 116.3985, 39.9995, '教学楼C栋二楼',    NOW() - INTERVAL 9 DAY),
(26, 'found', 8, 116.3995, 40.0015, '宿舍楼3号楼',      NOW() - INTERVAL 7 DAY),
(27, 'found', 9, 116.3965, 40.0010, '图书馆一楼大厅',   NOW() - INTERVAL 5 DAY),
(28, 'found', 10,116.3978, 39.9988, '三食堂一楼',       NOW() - INTERVAL 4 DAY),
(29, 'found', 11,116.3970, 40.0000, '教学楼A栋机房',    NOW() - INTERVAL 3 DAY),
(30, 'found', 12,116.3992, 40.0008, '操场看台',         NOW() - INTERVAL 2 DAY),
(31, 'found', 13,116.3958, 40.0002, '自习室B栋301',     NOW() - INTERVAL 1 DAY),
(32, 'found', 14,116.3982, 40.0012, '体育馆',           NOW() - INTERVAL 20 HOUR),
(33, 'found', 15,116.3962, 40.0008, '图书馆二楼',       NOW() - INTERVAL 16 HOUR),
(34, 'found', 16,116.3988, 39.9992, '教学楼D栋205',     NOW() - INTERVAL 10 HOUR),
(35, 'found', 17,116.3952, 40.0005, '自习室C栋',        NOW() - INTERVAL 5 HOUR),
(36, 'found', 18,116.3995, 40.0013, '操场足球场',       NOW() - INTERVAL 1 HOUR);

-- ============================================
-- 13. 物品向量表（36 条）
-- 向量元数据，实际向量由应用生成并存储于外部索引
-- ============================================
INSERT INTO `item_vector` (`id`, `item_id`, `item_type`, `has_text_vector`, `has_image_vector`, `text_dim`, `image_dim`, `indexed_at`, `create_time`, `update_time`) VALUES
-- 丢失物品 1-18
(1,  1,  'lost', 1, 0, 100, NULL, NOW() - INTERVAL 28 DAY, NOW() - INTERVAL 28 DAY, NOW() - INTERVAL 28 DAY),
(2,  2,  'lost', 1, 0, 100, NULL, NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 25 DAY),
(3,  3,  'lost', 1, 0, 100, NULL, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 20 DAY),
(4,  4,  'lost', 1, 0, 100, NULL, NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 18 DAY),
(5,  5,  'lost', 1, 0, 100, NULL, NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 15 DAY),
(6,  6,  'lost', 1, 0, 100, NULL, NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 12 DAY),
(7,  7,  'lost', 1, 0, 100, NULL, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 10 DAY),
(8,  8,  'lost', 1, 0, 100, NULL, NOW() - INTERVAL 8 DAY,  NOW() - INTERVAL 8 DAY,  NOW() - INTERVAL 8 DAY),
(9,  9,  'lost', 1, 0, 100, NULL, NOW() - INTERVAL 6 DAY,  NOW() - INTERVAL 6 DAY,  NOW() - INTERVAL 6 DAY),
(10, 10, 'lost', 1, 0, 100, NULL, NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 5 DAY),
(11, 11, 'lost', 1, 0, 100, NULL, NOW() - INTERVAL 4 DAY,  NOW() - INTERVAL 4 DAY,  NOW() - INTERVAL 4 DAY),
(12, 12, 'lost', 1, 0, 100, NULL, NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 3 DAY),
(13, 13, 'lost', 1, 0, 100, NULL, NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 2 DAY),
(14, 14, 'lost', 1, 0, 100, NULL, NOW() - INTERVAL 1 DAY,  NOW() - INTERVAL 1 DAY,  NOW() - INTERVAL 1 DAY),
(15, 15, 'lost', 1, 0, 100, NULL, NOW() - INTERVAL 18 HOUR, NOW() - INTERVAL 18 HOUR, NOW() - INTERVAL 18 HOUR),
(16, 16, 'lost', 1, 0, 100, NULL, NOW() - INTERVAL 12 HOUR, NOW() - INTERVAL 12 HOUR, NOW() - INTERVAL 12 HOUR),
(17, 17, 'lost', 1, 0, 100, NULL, NOW() - INTERVAL 6 HOUR,  NOW() - INTERVAL 6 HOUR,  NOW() - INTERVAL 6 HOUR),
(18, 18, 'lost', 1, 0, 100, NULL, NOW() - INTERVAL 2 HOUR,  NOW() - INTERVAL 2 HOUR,  NOW() - INTERVAL 2 HOUR),
-- 拾到物品 1-18
(19, 1, 'found', 1, 0, 100, NULL, NOW() - INTERVAL 27 DAY, NOW() - INTERVAL 27 DAY, NOW() - INTERVAL 27 DAY),
(20, 2, 'found', 1, 0, 100, NULL, NOW() - INTERVAL 24 DAY, NOW() - INTERVAL 24 DAY, NOW() - INTERVAL 24 DAY),
(21, 3, 'found', 1, 0, 100, NULL, NOW() - INTERVAL 19 DAY, NOW() - INTERVAL 19 DAY, NOW() - INTERVAL 19 DAY),
(22, 4, 'found', 1, 0, 100, NULL, NOW() - INTERVAL 11 DAY, NOW() - INTERVAL 11 DAY, NOW() - INTERVAL 11 DAY),
(23, 5, 'found', 1, 0, 100, NULL, NOW() - INTERVAL 9 DAY,  NOW() - INTERVAL 9 DAY,  NOW() - INTERVAL 9 DAY),
(24, 6, 'found', 1, 0, 100, NULL, NOW() - INTERVAL 14 DAY, NOW() - INTERVAL 14 DAY, NOW() - INTERVAL 14 DAY),
(25, 7, 'found', 1, 0, 100, NULL, NOW() - INTERVAL 9 DAY,  NOW() - INTERVAL 9 DAY,  NOW() - INTERVAL 9 DAY),
(26, 8, 'found', 1, 0, 100, NULL, NOW() - INTERVAL 7 DAY,  NOW() - INTERVAL 7 DAY,  NOW() - INTERVAL 7 DAY),
(27, 9, 'found', 1, 0, 100, NULL, NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 5 DAY),
(28, 10,'found', 1, 0, 100, NULL, NOW() - INTERVAL 4 DAY,  NOW() - INTERVAL 4 DAY,  NOW() - INTERVAL 4 DAY),
(29, 11,'found', 1, 0, 100, NULL, NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 3 DAY),
(30, 12,'found', 1, 0, 100, NULL, NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 2 DAY),
(31, 13,'found', 1, 0, 100, NULL, NOW() - INTERVAL 1 DAY,  NOW() - INTERVAL 1 DAY,  NOW() - INTERVAL 1 DAY),
(32, 14,'found', 1, 0, 100, NULL, NOW() - INTERVAL 20 HOUR, NOW() - INTERVAL 20 HOUR, NOW() - INTERVAL 20 HOUR),
(33, 15,'found', 1, 0, 100, NULL, NOW() - INTERVAL 16 HOUR, NOW() - INTERVAL 16 HOUR, NOW() - INTERVAL 16 HOUR),
(34, 16,'found', 1, 0, 100, NULL, NOW() - INTERVAL 10 HOUR, NOW() - INTERVAL 10 HOUR, NOW() - INTERVAL 10 HOUR),
(35, 17,'found', 1, 0, 100, NULL, NOW() - INTERVAL 5 HOUR,  NOW() - INTERVAL 5 HOUR,  NOW() - INTERVAL 5 HOUR),
(36, 18,'found', 1, 0, 100, NULL, NOW() - INTERVAL 1 HOUR,  NOW() - INTERVAL 1 HOUR,  NOW() - INTERVAL 1 HOUR);

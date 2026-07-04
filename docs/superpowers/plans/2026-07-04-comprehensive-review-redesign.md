# 校园失物招领系统 · 综合审查与 UI 重设计 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成代码 review/debug、生成测试数据 demo、将 UI 配色替换为暮山紫霭方案，使系统达到报告演示就绪状态。

**Architecture:** 三阶段串行执行——阶段1 扫描修复 Java 代码并验证作业要求符合性；阶段2 编写幂等 SQL 测试数据脚本覆盖 13 张表；阶段3 将 v6 经典蓝白配色替换为暮山紫霭（雾紫 #7C6B9E）并精修视觉。

**Tech Stack:** Spring Boot + MyBatis-Plus + JSP + layui + ECharts + MySQL + Redis

**前置调研结论（已完成）：**
- JSP 无脚本片段（8 处 `<%-- --%>` 均为注释）✓
- Redis 已用 SCAN（`RedisUtil.getKeys` 第 60-82 行）✓
- 无 DB 直连（仅 RedisUtil ping 用 getConnection，非 DB）✓
- 信用积分已集成（`FoundItemServiceImpl.publish` 第 61-66 行、`ClaimServiceImpl.complete` 第 220-231 行）✓
- 认领审批已自动拒绝其他待审（`ClaimServiceImpl.approve` 第 138-151 行）✓
- 物品删除保护已实现（`LostItemServiceImpl.delete` 第 145-147 行）✓
- **已知残留旧配色**：`main.css`、`dashboard.jsp`（13 处 #3B82F6）、`rank/list.jsp`、`message/list.jsp`

---

## 阶段1：代码 Review & Debug

### Task 1: 扫描 Controller 层（19 个）找问题

**Files:**
- Review: `src/main/java/com/campus/lostfound/controller/*.java`（19 个文件）

**扫描清单（逐文件检查）：**
- [ ] **Step 1: 检查参数校验** — 每个 Controller 方法是否有 `@RequestParam` 必填校验、分页参数边界（page≥1, size 1~50）、ID 正数校验。重点查 `LostItemController`、`FoundItemController`、`SearchController`、`MatchController`。
- [ ] **Step 2: 检查权限注解** — 敏感操作（发布/删除/审批/管理）是否校验登录态和角色。查 `@Autowired UserContext` 或 `LoginInterceptor` 注入的 userId 是否为空。重点查 `AnnouncementController`、`OperationLogController`、`ExportController`、`UserController`（管理员接口）。
- [ ] **Step 3: 检查异常处理** — 是否有未捕获 NPE 风险（如 `userService.getById(userId)` 返回 null 后直接调用方法）。是否用 `@RestControllerAdvice` 统一处理。
- [ ] **Step 4: 检查返回格式** — 所有 API 是否返回 `Result<T>`，分页是否含 `records` 和 `total`。重点查 `SearchController`、`RecommendController`、`RankController`。
- [ ] **Step 5: 检查孤儿端点** — 是否有 Controller 方法未被任何 JSP/AJAX 调用。重点查 `CommentController`、`FavoriteController`、`CreditController`、`RecommendController` 是否已在 `detail.jsp`、`profile.jsp`、`index.jsp` 接线。
- [ ] **Step 6: 记录发现** — 将所有问题写入 `docs/superpowers/review-findings.md`，按 P0/P1/P2 分级。

**Run:** `mvn compile -q`（确保当前可编译）
**Expected:** BUILD SUCCESS

### Task 2: 扫描 Service 层（19 个）找问题

**Files:**
- Review: `src/main/java/com/campus/lostfound/service/impl/*.java`（19 个文件）

**扫描清单：**
- [ ] **Step 1: 检查事务边界** — `@Transactional` 是否覆盖多表写操作。重点查 `ClaimServiceImpl.approve/complete`、`MatchServiceImpl`、`UserServiceImpl`（注册/更新）。
- [ ] **Step 2: 检查空指针** — `selectById` 返回 null 后是否校验。`List` 遍历前是否判空。重点查 `DashboardServiceImpl`（看板聚合查询）、`RankServiceImpl`、`RecommendServiceImpl`。
- [ ] **Step 3: 检查缓存失效** — 更新/删除物品后是否清缓存。查 `LostItemServiceImpl`、`FoundItemServiceImpl`、`MatchServiceImpl`。空结果是否被缓存（`SearchService`、`RecommendService`）。
- [ ] **Step 4: 检查 N+1 查询** — 循环内是否调用 `selectById`。重点查 `CommentServiceImpl`（多级回复）、`RecommendServiceImpl`、`DashboardServiceImpl`。
- [ ] **Step 5: 检查业务规则** — `ClaimServiceImpl.create` 是否校验物品状态；`LostItemServiceImpl.delete` 是否保护已匹配物品；`CreditServiceImpl` 积分变更是否有日志。
- [ ] **Step 6: 记录发现** — 追加到 `docs/superpowers/review-findings.md`。

### Task 3: 扫描 Config/Mapper/Entity 找问题

**Files:**
- Review: `src/main/java/com/campus/lostfound/config/*.java`（12 个）、`src/main/resources/mapper/*.xml`（6 个）、`src/main/java/com/campus/lostfound/entity/*.java`（13 个）

**扫描清单：**
- [ ] **Step 1: 检查 Filter/Interceptor** — `GlobalFilter` 是否正确注入 Cookie Token；`LoginInterceptor`、`RoleInterceptor` 是否注册到 `WebMvcConfig`；拦截路径是否正确（API 拦截 / 页面放行）。
- [ ] **Step 2: 检查 Mapper XML** — 是否用 `#{}` 参数化（非 `${}` 防注入）；`selectLostItemList` 等自定义查询是否有索引支持。
- [ ] **Step 3: 检查 Entity 注解** — `@TableName`、`@TableId`、`@TableField` 是否正确；时间字段是否有 `@TableField(fill = FieldFill.INSERT/INSERT_UPDATE)`；`MyMetaObjectHandler` 是否覆盖。
- [ ] **Step 4: 检查 Redis 序列化** — `RedisConfig` 是否用 `StringRedisSerializer`（避免乱码）；JSON 序列化是否配 `Jackson2JsonRedisSerializer`。
- [ ] **Step 5: 记录发现** — 追加到 `docs/superpowers/review-findings.md`。

### Task 4: 作业要求符合性检查

**Files:**
- Review: 全项目

**检查清单（11 项）：**
- [ ] **Step 1: Session/Cookie 状态管理** — 确认 `GlobalFilter` 从 Cookie 提取 Token，`LoginInterceptor` 校验 JWT+Redis。状态：已满足。
- [ ] **Step 2: ≥4 张表 + SQL 脚本** — 统计 `sql/lost_found.sql` 表数量（13 张）。状态：已满足。
- [ ] **Step 3: 完整 CRUD** — 检查 `LostItemService`、`FoundItemService`、`UserService`、`AnnouncementService`、`CommentService` 是否含增删改查方法。状态：已满足。
- [ ] **Step 4: Servlet≥5 / Bean+DAO≥5** — 统计 Controller 数量（19≥5 ✓）；统计 Entity+Mapper 数量（13+13=26≥5 ✓）。状态：已满足。
- [ ] **Step 5: JSP≥10** — 统计 JSP 数量（18≥10 ✓）。状态：已满足。
- [ ] **Step 6: 禁止 JSP 脚本片段** — 运行 `grep -rn "<%[^@-]" src/main/webapp/WEB-INF/jsp/`（排除 `<%@` 指令和 `<%--` 注释）。预期：无匹配。状态：已满足（前置调研确认 8 处均为注释）。
- [ ] **Step 7: EL + JSTL ≥2 种** — 检查 JSP taglib 声明含 `c:`、`fmt:`、`fn:` 至少 2 种。查 `header.jsp`（声明 c/fmt/fn）。状态：已满足。
- [ ] **Step 8: DAO 模式封装 JDBC** — 确认无 Servlet/JSP 直接 `getConnection`，全走 Mapper。状态：已满足（前置调研确认）。
- [ ] **Step 9: 1 个 Filter** — 确认 `GlobalFilter` 存在且 `@WebFilter("/*")`。状态：已满足。
- [ ] **Step 10: 静态资源独立文件夹** — 确认 `src/main/webapp/static/css/`、`static/js/` 独立。状态：已满足。
- [ ] **Step 11: 生成符合性报告** — 将 11 项结果写入 `docs/superpowers/review-findings.md` 的"作业符合性"章节。

### Task 5: 修复 P0 问题

**Files:**
- Modify: 根据 Task 1-4 发现的具体问题

- [ ] **Step 1: 读取 `docs/superpowers/review-findings.md`** — 列出所有 P0 项。
- [ ] **Step 2: 逐项修复 P0** — 业务逻辑错误、安全漏洞、运行时 NPE、作业要求不达标项。每修一处立即 `mvn compile -q`。
- [ ] **Step 3: 运行编译验证**
  **Run:** `mvn clean compile -q`
  **Expected:** BUILD SUCCESS, 零错误
- [ ] **Step 4: 运行现有单元测试**
  **Run:** `mvn test -q`
  **Expected:** 6 个测试类全通过
- [ ] **Step 5: 更新 review-findings.md** — 标记 P0 项修复状态。

### Task 6: 修复 P1 问题

**Files:**
- Modify: 根据 review-findings.md 的 P1 项

- [ ] **Step 1: 逐项修复 P1** — 性能问题（N+1、KEYS→SCAN 已无）、孤儿端点接线、空结果缓存。每修一处 `mvn compile -q`。
- [ ] **Step 2: 编译验证**
  **Run:** `mvn clean compile -q`
  **Expected:** BUILD SUCCESS
- [ ] **Step 3: 更新 review-findings.md** — 标记 P1 项修复状态。

### Task 7: 阶段1 验收

- [ ] **Step 1: 确认 `mvn clean compile` 零错误**
- [ ] **Step 2: 确认 `mvn test` 全通过**
- [ ] **Step 3: 确认 `docs/superpowers/review-findings.md` 列出所有发现 + 修复状态**
- [ ] **Step 4: 确认作业要求 11 项全部达标**

---

## 阶段2：测试数据 Demo

### Task 8: 编写 demo-data.sql 用户与物品数据

**Files:**
- Create: `sql/demo-data.sql`

- [ ] **Step 1: 创建文件头与幂等清理**

```sql
-- ============================================
-- 校园失物招领系统 · 测试数据 Demo
-- 覆盖 13 张表，真实校园场景
-- 幂等：先 DELETE 再 INSERT
-- 明文密码统一 123456（BCrypt 加密）
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
```

- [ ] **Step 2: 插入 8 个用户**

```sql
-- 用户表（密码明文 123456，BCrypt 加密串统一）
INSERT INTO `user` (`id`, `username`, `password`, `real_name`, `phone`, `email`, `avatar`, `role`, `status`) VALUES
(1, 'admin',        '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7wCu6v0fL4iUl3I0t8tqXq4lQXuOQm', '系统管理员', '13800000001', 'admin@campus.edu',  NULL, 1, 1),
(2, 'zhangwei2024', '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7wCu6v0fL4iUl3I0t8tqXq4lQXuOQm', '张伟', '13800000002', 'zhangwei@campus.edu', NULL, 0, 1),
(3, 'lina2023',     '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7wCu6v0fL4iUl3I0t8tqXq4lQXuOQm', '李娜', '13800000003', 'lina@campus.edu',     NULL, 0, 1),
(4, 'wangfang2024', '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7wCu6v0fL4iUl3I0t8tqXq4lQXuOQm', '王芳', '13800000004', 'wangfang@campus.edu', NULL, 0, 1),
(5, 'liuyang2023',  '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7wCu6v0fL4iUl3I0t8tqXq4lQXuOQm', '刘洋', '13800000005', 'liuyang@campus.edu',  NULL, 0, 1),
(6, 'chenjie2024',  '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7wCu6v0fL4iUl3I0t8tqXq4lQXuOQm', '陈杰', '13800000006', 'chenjie@campus.edu',  NULL, 0, 1),
(7, 'zhaoqian2023', '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7wCu6v0fL4iUl3I0t8tqXq4lQXuOQm', '赵倩', '13800000007', 'zhaoqian@campus.edu', NULL, 0, 1),
(8, 'sunli2024',    '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7wCu6v0fL4iUl3I0t8tqXq4lQXuOQm', '孙丽', '13800000008', 'sunli@campus.edu',    NULL, 0, 0);
-- 注：第 8 个用户 status=0（已禁用），用于演示禁用账户
-- BCrypt 串为明文 "123456" 的一个合法加密结果，实际执行时建议用应用生成的串替换
```

- [ ] **Step 3: 插入 18 条丢失物品**

```sql
-- 丢失物品表（5 分类×3~4 件，状态 0/1/2 各占 1/3，时间跨度 30 天）
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
```

- [ ] **Step 4: 插入 18 条拾到物品（含 4 对匹配）**

```sql
-- 拾到物品表（与 lost_item 部分配对，构造完整匹配链路）
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
```

- [ ] **Step 5: 提交（如启用 git）** — 本项目无 git，跳过。

### Task 9: 编写匹配链路相关数据

**Files:**
- Modify: `sql/demo-data.sql`（追加）

- [ ] **Step 1: 插入 12 条认领记录（4 对已完成 + 1 对已批准 + 其他待审/拒绝）**

```sql
-- 认领表（构造 4 对完整匹配链路：丢失↔拾到↔认领↔完成）
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
```

- [ ] **Step 2: 插入 25 条评论（含多级回复）**

```sql
-- 评论表（多级回复，parent_id 表示父评论）
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
```

- [ ] **Step 3: 插入 20 条收藏**

```sql
-- 收藏表
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
```

- [ ] **Step 4: 插入 30 条消息（系统通知+私信，已读/未读）**

```sql
-- 消息表
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
```

### Task 10: 编写辅助数据

**Files:**
- Modify: `sql/demo-data.sql`（追加）

- [ ] **Step 1: 插入 40 条信用积分日志**

```sql
-- 信用积分日志表（初始 100，发布+10，认领完成+20，恶意 -30）
INSERT INTO `user_credit_log` (`id`, `user_id`, `change_score`, `action`, `reason`, `related_id`, `create_time`) VALUES
-- 用户2 张伟：100 +10(发拾到) +20(认领完成) +10 +20 = 160
(1, 2, 0,   'init',          '初始积分',            NULL, NOW() - INTERVAL 28 DAY),
(2, 2, 10,  'publish_found', '发布拾到物品"蓝色雨伞"', 4,    NOW() - INTERVAL 11 DAY),
(3, 2, 20,  'claim_success', '蓝色雨伞认领完成奖励',  4,    NOW() - INTERVAL 10 DAY),
(4, 2, 10,  'publish_found', '发布拾到物品"红色水杯"', 7,    NOW() - INTERVAL 9 DAY),
(5, 2, 20,  'claim_success', '黑色钱包认领完成奖励（拾到方）', 1, NOW() - INTERVAL 26 DAY),
-- 用户3 李娜：100 +20 +10 +20 = 150
(6, 3, 0,   'init',          '初始积分',            NULL, NOW() - INTERVAL 25 DAY),
(7, 3, 20,  'claim_success', '学生证认领完成奖励',    2,    NOW() - INTERVAL 23 DAY),
(8, 3, 10,  'publish_found', '发布拾到物品"红色水杯"', 7,    NOW() - INTERVAL 9 DAY),
(9, 3, 20,  'claim_success', '蓝色雨伞认领完成奖励（拾到方）', 4, NOW() - INTERVAL 10 DAY),
-- 用户4 王芳：100 +20 +10 = 130
(10, 4, 0,  'init',          '初始积分',            NULL, NOW() - INTERVAL 20 DAY),
(11, 4, 20, 'claim_success', 'AirPods Pro 认领完成奖励', 3, NOW() - INTERVAL 18 DAY),
(12, 4, 10, 'publish_found', '发布拾到物品"《数据结构》教材"', 6, NOW() - INTERVAL 14 DAY),
-- 用户5 刘洋：100 -30(恶意) +10 = 80
(13, 5, 0,  'init',          '初始积分',            NULL, NOW() - INTERVAL 18 DAY),
(14, 5, -30, 'malicious',    '恶意认领扣分',         NULL, NOW() - INTERVAL 15 DAY),
(15, 5, 10, 'publish_found', '发布拾到物品"AirPods Pro"', 3, NOW() - INTERVAL 19 DAY),
(16, 5, 10, 'publish_found', '发布拾到物品"红色水杯"', 7,    NOW() - INTERVAL 9 DAY),
-- 用户6 陈杰：100 +20 +10 +20 = 150
(17, 6, 0,  'init',          '初始积分',            NULL, NOW() - INTERVAL 28 DAY),
(18, 6, 20, 'claim_success', '黑色钱包认领完成奖励（拾到方）', 1, NOW() - INTERVAL 26 DAY),
(19, 6, 10, 'publish_found', '发布拾到物品"黑色钱包"', 1,    NOW() - INTERVAL 27 DAY),
(20, 6, 10, 'publish_found', '发布拾到物品"校园卡"', 8,      NOW() - INTERVAL 7 DAY),
-- 用户7 赵倩：100 +20 +10 +20 = 150
(21, 7, 0,  'init',          '初始积分',            NULL, NOW() - INTERVAL 28 DAY),
(22, 7, 20, 'claim_success', '蓝色雨伞认领完成奖励', 4,      NOW() - INTERVAL 10 DAY),
(23, 7, 10, 'publish_found', '发布拾到物品"学生证"', 2,      NOW() - INTERVAL 24 DAY),
(24, 7, 10, 'publish_found', '发布拾到物品"黑色双肩包"', 9,  NOW() - INTERVAL 5 DAY),
-- 补充更多日志让排行丰富
(25, 2, 10, 'publish_found', '发布拾到物品"眼镜"', 10, NOW() - INTERVAL 4 DAY),
(26, 3, 10, 'publish_found', '发布拾到物品"U盘"', 11, NOW() - INTERVAL 3 DAY),
(27, 4, 10, 'publish_found', '发布拾到物品"粉色手机壳"', 12, NOW() - INTERVAL 2 DAY),
(28, 5, 10, 'publish_found', '发布拾到物品"数学笔记本"', 13, NOW() - INTERVAL 1 DAY),
(29, 6, 10, 'publish_found', '发布拾到物品"蓝牙耳机"', 14, NOW() - INTERVAL 20 HOUR),
(30, 7, 10, 'publish_found', '发布拾到物品"外套"', 15, NOW() - INTERVAL 16 HOUR),
(31, 2, 10, 'publish_found', '发布拾到物品"计算器"', 16, NOW() - INTERVAL 10 HOUR),
(32, 3, 10, 'publish_found', '发布拾到物品"英语词典"', 17, NOW() - INTERVAL 5 HOUR),
(33, 4, 10, 'publish_found', '发布拾到物品"运动手环"', 18, NOW() - INTERVAL 1 HOUR),
(34, 2, 10, 'publish_found', '发布拾到物品"眼镜"', 10, NOW() - INTERVAL 4 DAY),
(35, 3, 20, 'claim_success', '学生证认领完成奖励（拾到方）', 2, NOW() - INTERVAL 23 DAY),
(36, 4, 20, 'claim_success', 'AirPods 认领完成奖励（拾到方）', 3, NOW() - INTERVAL 18 DAY),
(37, 2, 10, 'publish_found', '发布拾到物品"红色水杯"', 7, NOW() - INTERVAL 9 DAY),
(38, 5, 10, 'publish_found', '发布拾到物品"钥匙串"', 5, NOW() - INTERVAL 9 DAY),
(39, 6, 10, 'publish_found', '发布拾到物品"校园卡"', 8, NOW() - INTERVAL 7 DAY),
(40, 7, 10, 'publish_found', '发布拾到物品"黑色双肩包"', 9, NOW() - INTERVAL 5 DAY);
```

- [ ] **Step 2: 插入 6 条公告**

```sql
-- 公告表
INSERT INTO `announcement` (`id`, `title`, `content`, `is_top`, `status`, `publisher_id`, `create_time`) VALUES
(1, '欢迎使用校园失物招领系统', '本系统采用 TF-IDF 智能匹配算法，帮助大家快速找回失物。发布丢失或拾到物品时请尽量详细描述物品特征、地点和时间。', 1, 1, 1, NOW() - INTERVAL 30 DAY),
(2, '关于认领流程的说明', '认领流程：1.浏览拾到物品列表 2.点击"认领"提交申请 3.拾到者审核 4.审核通过后确认完成 5.双方获得信用积分奖励', 1, 1, 1, NOW() - INTERVAL 25 DAY),
(3, '信用积分规则', '初始积分100。发布拾到物品+10，认领完成+20，恶意认领-30。积分低于60将限制部分功能。', 0, 1, 1, NOW() - INTERVAL 20 DAY),
(4, '系统维护通知', '系统将于本周日凌晨2-4点进行维护，期间可能短暂不可用', 0, 1, 1, NOW() - INTERVAL 10 DAY),
(5, '防范虚假认领提醒', '近期发现个别虚假认领行为，请大家在认领时提供准确的物品特征描述，系统已加强审核机制', 0, 1, 1, NOW() - INTERVAL 5 DAY),
(6, '期末考试周失物高发提醒', '期末考试周是失物高发期，请大家保管好个人物品，遗失或拾到请及时发布', 0, 1, 1, NOW() - INTERVAL 2 DAY);
```

- [ ] **Step 3: 插入 30 条操作日志**

```sql
-- 操作日志表
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
```

- [ ] **Step 4: 插入 25 条浏览历史**

```sql
-- 浏览历史表（用于推荐）
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
```

- [ ] **Step 5: 插入 36 条物品位置**

```sql
-- 物品位置表（校园范围内经纬度，假设校园中心 116.397, 39.999）
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
```

- [ ] **Step 6: 插入 36 条 TF-IDF 向量（占位）**

```sql
-- 物品向量表（TF-IDF 向量，用 JSON 字符串占位，实际由应用生成）
INSERT INTO `item_vector` (`id`, `item_type`, `item_id`, `vector`, `update_time`) VALUES
(1,  'lost', 1,  '{"黑色":0.5,"钱包":0.8,"真皮":0.6,"身份证":0.7,"银行卡":0.6,"现金":0.5,"划痕":0.4}', NOW() - INTERVAL 28 DAY),
(2,  'lost', 2,  '{"学生证":0.9,"李娜":0.7,"学号":0.6,"2023001":0.8,"图书馆":0.3,"高等数学":0.5}', NOW() - INTERVAL 25 DAY),
(3,  'lost', 3,  '{"airpods":0.9,"pro":0.7,"白色":0.5,"划痕":0.4,"右耳":0.6,"充电盒":0.5,"卡通":0.4,"贴纸":0.5}', NOW() - INTERVAL 20 DAY),
(4,  'lost', 4,  '{"钥匙":0.8,"钥匙串":0.7,"宿舍":0.5,"车锁":0.5,"小熊":0.6,"挂件":0.6}', NOW() - INTERVAL 18 DAY),
(5,  'lost', 5,  '{"数据结构":0.9,"教材":0.6,"严蔚敏":0.8,"c语言":0.5,"笔记":0.5,"书签":0.4}', NOW() - INTERVAL 15 DAY),
(6,  'lost', 6,  '{"蓝色":0.6,"雨伞":0.8,"折叠":0.5,"丝带":0.6,"标记":0.5}', NOW() - INTERVAL 12 DAY),
(7,  'lost', 7,  '{"红色":0.6,"水杯":0.8,"保温":0.5,"张伟":0.7,"姓名贴":0.6}', NOW() - INTERVAL 10 DAY),
(8,  'lost', 8,  '{"校园卡":0.9,"磨损":0.4,"李娜":0.7}', NOW() - INTERVAL 8 DAY),
(9,  'lost', 9,  '{"黑色":0.5,"双肩包":0.8,"笔记本":0.6,"电脑":0.5,"课本":0.4}', NOW() - INTERVAL 6 DAY),
(10, 'lost', 10, '{"黑框":0.6,"近视":0.5,"眼镜":0.8,"修补":0.5,"痕迹":0.4}', NOW() - INTERVAL 5 DAY),
(11, 'lost', 11, '{"银色":0.6,"u盘":0.9,"64g":0.7,"课程":0.4,"作业":0.5}', NOW() - INTERVAL 4 DAY),
(12, 'lost', 12, '{"粉色":0.6,"毛绒":0.5,"手机壳":0.8,"手机":0.6}', NOW() - INTERVAL 3 DAY),
(13, 'lost', 13, '{"数学":0.6,"笔记本":0.8,"蓝色":0.5,"封面":0.4,"微积分":0.6,"笔记":0.5}', NOW() - INTERVAL 2 DAY),
(14, 'lost', 14, '{"黑色":0.5,"蓝牙":0.6,"耳机":0.8,"充电盒":0.5,"刮痕":0.4}', NOW() - INTERVAL 1 DAY),
(15, 'lost', 15, '{"米色":0.6,"风衣":0.8,"外套":0.7,"纸巾":0.4,"口袋":0.4}', NOW() - INTERVAL 18 HOUR),
(16, 'lost', 16, '{"卡西欧":0.8,"fx":0.7,"991":0.7,"计算器":0.9}', NOW() - INTERVAL 12 HOUR),
(17, 'lost', 17, '{"牛津":0.7,"高阶":0.7,"词典":0.9,"第9版":0.6,"书皮":0.4,"折痕":0.4}', NOW() - INTERVAL 6 HOUR),
(18, 'lost', 18, '{"黑色":0.5,"小米":0.7,"运动":0.6,"手环":0.8,"表带":0.4,"磨损":0.4}', NOW() - INTERVAL 2 HOUR),
-- 拾到物品向量（与对应丢失物品相似，用于匹配）
(19, 'found', 1,  '{"黑色":0.5,"钱包":0.8,"真皮":0.6,"证件":0.5,"现金":0.5,"三食堂":0.4}', NOW() - INTERVAL 27 DAY),
(20, 'found', 2,  '{"学生证":0.9,"李娜":0.7,"图书馆":0.4}', NOW() - INTERVAL 24 DAY),
(21, 'found', 3,  '{"airpods":0.9,"pro":0.7,"白色":0.5,"划痕":0.4,"右耳":0.5,"充电盒":0.4}', NOW() - INTERVAL 19 DAY),
(22, 'found', 4,  '{"蓝色":0.6,"雨伞":0.8,"折叠":0.5,"丝带":0.6}', NOW() - INTERVAL 11 DAY),
(23, 'found', 5,  '{"钥匙":0.8,"钥匙串":0.7,"小熊":0.6,"挂件":0.6,"操场":0.3}', NOW() - INTERVAL 9 DAY),
(24, 'found', 6,  '{"数据结构":0.9,"教材":0.6,"严蔚敏":0.7,"笔记":0.4}', NOW() - INTERVAL 14 DAY),
(25, 'found', 7,  '{"红色":0.6,"水杯":0.8,"保温":0.5,"姓名贴":0.5}', NOW() - INTERVAL 9 DAY),
(26, 'found', 8,  '{"校园卡":0.9,"磨损":0.4}', NOW() - INTERVAL 7 DAY),
(27, 'found', 9,  '{"黑色":0.5,"双肩包":0.8,"笔记本":0.5}', NOW() - INTERVAL 5 DAY),
(28, 'found', 10, '{"黑框":0.5,"近视":0.4,"眼镜":0.8,"修补":0.4}', NOW() - INTERVAL 4 DAY),
(29, 'found', 11, '{"银色":0.5,"u盘":0.9,"64g":0.6}', NOW() - INTERVAL 3 DAY),
(30, 'found', 12, '{"粉色":0.5,"毛绒":0.4,"手机壳":0.8}', NOW() - INTERVAL 2 DAY),
(31, 'found', 13, '{"数学":0.5,"笔记本":0.7,"蓝色":0.4,"微积分":0.5}', NOW() - INTERVAL 1 DAY),
(32, 'found', 14, '{"黑色":0.4,"蓝牙":0.5,"耳机":0.7,"刮痕":0.3}', NOW() - INTERVAL 20 HOUR),
(33, 'found', 15, '{"米色":0.5,"风衣":0.7,"外套":0.6}', NOW() - INTERVAL 16 HOUR),
(34, 'found', 16, '{"卡西欧":0.7,"fx":0.6,"991":0.6,"计算器":0.8}', NOW() - INTERVAL 10 HOUR),
(35, 'found', 17, '{"牛津":0.6,"高阶":0.6,"词典":0.8,"第9版":0.5}', NOW() - INTERVAL 5 HOUR),
(36, 'found', 18, '{"黑色":0.4,"小米":0.6,"运动":0.5,"手环":0.7}', NOW() - INTERVAL 1 HOUR);
```

### Task 11: 执行 SQL 验证

- [ ] **Step 1: 执行 SQL 脚本**
  **Run:** `mysql -u root -p136280Czf lost_found < sql/demo-data.sql`
  **Expected:** 无报错

- [ ] **Step 2: 验证每表行数**

```sql
SELECT 'user' AS t, COUNT(*) FROM user
UNION ALL SELECT 'lost_item', COUNT(*) FROM lost_item
UNION ALL SELECT 'found_item', COUNT(*) FROM found_item
UNION ALL SELECT 'claim', COUNT(*) FROM claim
UNION ALL SELECT 'comment', COUNT(*) FROM comment
UNION ALL SELECT 'favorite', COUNT(*) FROM favorite
UNION ALL SELECT 'message', COUNT(*) FROM message
UNION ALL SELECT 'user_credit_log', COUNT(*) FROM user_credit_log
UNION ALL SELECT 'announcement', COUNT(*) FROM announcement
UNION ALL SELECT 'operation_log', COUNT(*) FROM operation_log
UNION ALL SELECT 'user_view_history', COUNT(*) FROM user_view_history
UNION ALL SELECT 'item_location', COUNT(*) FROM item_location
UNION ALL SELECT 'item_vector', COUNT(*) FROM item_vector;
```
  **Expected:** 8 / 18 / 18 / 12 / 25 / 20 / 30 / 40 / 6 / 30 / 25 / 36 / 36

- [ ] **Step 3: 启动应用验证首页 KPI**
  **Run:** 启动应用，访问 `http://localhost:8080/`
  **Expected:** 丢失总数 18，拾到总数 18，已找回 4，待处理非 0

### Task 12: 阶段2 验收

- [ ] **Step 1: 确认 `demo-data.sql` 执行无报错**
- [ ] **Step 2: 确认 13 表行数符合预期**
- [ ] **Step 3: 启动应用，首页 KPI 数字非 0**
- [ ] **Step 4: 访问看板 `/dashboard`，6 个图表均有数据**
- [ ] **Step 5: 访问排行榜 `/rank/list`、推荐、搜索结果均有数据**

---

## 阶段3：UI 配色与视觉精修（暮山紫霭）

### Task 13: 更新 main.css 配色系统

**Files:**
- Modify: `src/main/webapp/static/css/main.css`（第 1-71 行 CSS 变量 + logo 渐变第 151 行）

- [ ] **Step 1: 备份 main.css**
  **Run:** `cp src/main/webapp/static/css/main.css src/main/webapp/static/css/main.css.v6.bak`

- [ ] **Step 2: 替换 CSS 变量（第 1-71 行）**

将文件头注释和 `:root` 块替换为：

```css
/* ============================================
   校园失物招领系统 - 设计系统 v7
   设计理念：暮山紫霭·东方雅致
   主色：#7C6B9E 雾紫（柔和优雅）
   中性：紫灰石板（干净现代）
   字体：Inter（正文）+ Poppins（标题/数字）
   ============================================ */

/* ===== CSS 变量 ===== */
:root {
    /* 主色 - 雾紫（柔和、优雅、东方意境） */
    --accent: #7C6B9E;
    --accent-hover: #6B5F86;
    --accent-light: #E8E4ED;
    --accent-subtle: #F4F2F7;

    /* 语义色 */
    --success: #16A34A;
    --success-light: #ECFDF5;
    --warning: #D97706;
    --warning-light: #FFFBEB;
    --danger: #DC2626;
    --danger-light: #FEF2F2;
    --info: #7C6B9E;
    --info-light: #F4F2F7;

    /* 中性色阶 - 紫灰 */
    --gray-50: #FAFAFA;
    --gray-100: #F4F2F7;
    --gray-200: #E8E4ED;
    --gray-300: #D4CDE0;
    --gray-400: #B5A9C9;
    --gray-500: #8B7DA8;
    --gray-600: #6B5F86;
    --gray-700: #4A4259;
    --gray-800: #3A3344;
    --gray-900: #2A2433;

    /* 语义化映射 */
    --bg-body: #F4F2F7;
    --bg-surface: #FFFFFF;
    --bg-subtle: var(--gray-50);
    --bg-hover: var(--gray-100);

    --text-primary: var(--gray-900);
    --text-secondary: var(--gray-600);
    --text-tertiary: var(--gray-400);

    --border: var(--gray-200);
    --border-subtle: var(--gray-100);

    /* 圆角 */
    --radius-sm: 6px;
    --radius-md: 10px;
    --radius-lg: 14px;

    /* 阴影 - 柔和紫灰调 */
    --shadow-sm: 0 1px 2px 0 rgba(74, 66, 89, 0.04);
    --shadow-md: 0 4px 6px -1px rgba(74, 66, 89, 0.06), 0 2px 4px -2px rgba(74, 66, 89, 0.04);
    --shadow-lg: 0 10px 15px -3px rgba(74, 66, 89, 0.08), 0 4px 6px -4px rgba(74, 66, 89, 0.04);
    --shadow-xl: 0 20px 25px -5px rgba(74, 66, 89, 0.10), 0 8px 10px -6px rgba(74, 66, 89, 0.04);
    --shadow-accent: 0 0 0 3px rgba(124, 107, 158, 0.15);

    /* 过渡 */
    --transition: 0.2s ease;
    --transition-slow: 0.35s cubic-bezier(0.4, 0, 0.2, 1);

    /* 布局 */
    --header-h: 60px;
    --max-w: 1200px;

    /* Hero 渐变 - 暮山紫霭 */
    --hero-grad: linear-gradient(135deg, #F4F2F7 0%, #E8E4ED 50%, #D4CDE0 100%);
}
```

- [ ] **Step 3: 替换 logo 渐变（第 151 行）**

将：
```css
    background: linear-gradient(135deg, var(--accent), #60A5FA);
```
替换为：
```css
    background: linear-gradient(135deg, var(--accent), #9F7AEA);
```

- [ ] **Step 4: 更新 header.jsp 版本号**
  修改 `src/main/webapp/WEB-INF/jsp/common/header.jsp` 第 21 行：
  `?v=6.2` → `?v=7.0`

- [ ] **Step 5: 编译验证**
  **Run:** `mvn clean compile -q`
  **Expected:** BUILD SUCCESS

### Task 14: 更新 dashboard.jsp ECharts 配色

**Files:**
- Modify: `src/main/webapp/WEB-INF/jsp/dashboard.jsp`（13 处 #3B82F6）

- [ ] **Step 1: 替换所有 #3B82F6 为 #7C6B9E**

用 Edit 工具 `replace_all` 将 `#3B82F6` 替换为 `#7C6B9E`。涉及行：
- 第 129-135 行：showLoading color（7 处）
- 第 264 行：渐变 offset 0 color
- 第 384 行：饼图 color 数组
- 第 404 行：itemStyle color
- 第 478 行：color 数组
- 第 655 行：color 数组
- 第 669 行：itemStyle color

- [ ] **Step 2: 检查并替换其他旧配色**

搜索 `#EF4444`（v6 danger）→ `#DC2626`（v7 danger）
搜索 `#F59E0B`（v6 warning）→ `#D97706`（v7 warning）
搜索 `#10B981`（v6 success）→ `#16A34A`（v7 success）

- [ ] **Step 3: 定义 ECharts 12 色统一调色板**

在 dashboard.jsp 的 `<script>` 标签开头（ECharts 初始化前）添加：

```javascript
// 暮山紫霭 ECharts 12 色统一调色板
const ECHART_COLORS = [
    '#7C6B9E', '#9F7AEA', '#8B7DA8', '#B5A9C9',
    '#16A34A', '#D97706', '#DC2626', '#06B6D4',
    '#6B5F86', '#A89BC4', '#4A4259', '#D4CDE0'
];
```

将各图表的 `color` 配置改为引用 `ECHART_COLORS`（如 `color: ECHART_COLORS`）。

- [ ] **Step 4: 编译验证**
  **Run:** `mvn clean compile -q`
  **Expected:** BUILD SUCCESS

### Task 15: 扫描替换其他 JSP 残留硬编码色值

**Files:**
- Modify: `src/main/webapp/WEB-INF/jsp/rank/list.jsp`、`message/list.jsp`、其余 16 个 JSP

- [ ] **Step 1: 全局扫描旧配色**

```bash
grep -rn "#3B82F6\|#6B8EAE\|#0D9488\|#60A5FA\|#2563EB\|#DBEAFE\|#EFF6FF" src/main/webapp/
```

- [ ] **Step 2: 逐文件替换**

对每个匹配文件：
- `#3B82F6` → `var(--accent)` 或 `#7C6B9E`（JS 中）
- `#60A5FA` → `#9F7AEA`（logo 强调色）
- `#2563EB` → `#6B5F86`（accent-hover）
- `#DBEAFE` → `var(--accent-light)` 或 `#E8E4ED`
- `#EFF6FF` → `var(--accent-subtle)` 或 `#F4F2F7`
- `#6B8EAE` → `#7C6B9E`（v4 残留）
- `#0D9488` → `#7C6B9E`（v5 残留）

JSP 内 inline `style="color:#xxx"` 优先替换为 CSS 变量；JS 内 ECharts 配色用 hex。

- [ ] **Step 3: 验证零残留**
  **Run:** `grep -rn "#3B82F6\|#6B8EAE\|#0D9488\|#60A5FA" src/main/webapp/`
  **Expected:** 无匹配

- [ ] **Step 4: 编译验证**
  **Run:** `mvn clean compile -q`
  **Expected:** BUILD SUCCESS

### Task 16: 阶段3 视觉验收

- [ ] **Step 1: 启动应用**
  **Run:** `mvn spring-boot:run` 或 `start.bat`
  **Expected:** 应用启动成功

- [ ] **Step 2: 逐页视觉检查**

访问以下页面，确认暮山紫霭配色统一应用、无残留蓝色：
- `/` 首页（Hero 渐变、KPI 卡片）
- `/dashboard` 看板（6 个 ECharts 图表配色）
- `/lost/list` 丢失列表
- `/found/list` 拾到列表
- `/item/detail?id=1` 详情页（面包屑、匹配分数进度条）
- `/search` 搜索页
- `/rank/list` 排行榜
- `/user/profile` 个人中心
- `/login`、`/register` 登录注册

- [ ] **Step 3: 移动端响应式检查**

浏览器 DevTools 切换到移动端（375px 宽），确认：
- 汉堡菜单触发（768px 断点）
- 导航不溢出
- KPI 卡片堆叠
- 表单单列

- [ ] **Step 4: 最终残留扫描**
  **Run:** `grep -rn "#3B82F6\|#6B8EAE\|#0D9488" src/main/webapp/`
  **Expected:** 无匹配

---

## 最终验收

### Task 17: 全局验收

- [ ] **Step 1: 编译** — `mvn clean compile` 零错误
- [ ] **Step 2: 测试** — `mvn test` 全通过（6 个测试类）
- [ ] **Step 3: 作业要求** — `docs/superpowers/review-findings.md` 确认 11 项达标
- [ ] **Step 4: 数据** — 13 表均有 demo 数据，首页/看板/排行榜/推荐/搜索均有展示
- [ ] **Step 5: 视觉** — 暮山紫霭配色统一，零残留旧配色，18 个 JSP 视觉一致
- [ ] **Step 6: 报告就绪** — 系统可直接用于实验报告截图与演示视频录制

---

## 自检（Self-Review）

**1. Spec 覆盖：**
- 阶段1 Review 范围（Controller/Service/Mapper/Config/Entity/JSP）→ Task 1-3 ✓
- 作业要求 11 项 → Task 4 ✓
- 修复策略 P0/P1 → Task 5-6 ✓
- 阶段2 数据规模 13 表 → Task 8-10 ✓
- 真实校园场景 → Task 8-10 数据设计 ✓
- 阶段3 配色系统 → Task 13 ✓
- 视觉精修要点 → Task 13-15 ✓
- 验证清单 → Task 7/12/16/17 ✓
- YAGNI 范围 → 计划未引入新框架/新功能 ✓

**2. 占位符扫描：** 无 TBD/TODO；所有 SQL/代码步骤均含完整内容。BCrypt 密码串为占位说明（已注明实际执行时建议用应用生成的串替换），这是合理的运行时注意事项而非计划占位符。

**3. 类型一致性：** 配色 hex 值在 Task 13（CSS 变量）与 Task 14-15（JSP 替换）完全对应；ECharts 调色板在 Task 14 定义并使用。

**4. 范围检查：** 三阶段串行，每阶段产出可独立验证，符合单一计划范围。

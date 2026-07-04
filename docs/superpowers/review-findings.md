# 代码 Review 发现汇总

- **日期**：2026-07-04
- **审查范围**：19 Controller + 19 Service + 12 Config + 6 Mapper XML + 13 Entity + 18 JSP
- **审查方式**：Subagent 并行扫描三层

---

## P0 问题（必须修复）

### 1. [最严重] UserMapper.xml 缺少 credit_score 列映射
- **位置**：`src/main/resources/mapper/UserMapper.xml:6-18`
- **问题**：`userResultMap` 未映射 `credit_score` 列，导致 `selectByUsername` 查出的 `User.creditScore` 恒为 null，信用积分相关逻辑失效
- **修复**：在 resultMap 中追加 `<result column="credit_score" property="creditScore"/>`

### 2. ClaimServiceImpl.complete 事务嵌套导致积分失败回滚主流程
- **位置**：`src/main/java/com/campus/lostfound/service/impl/ClaimServiceImpl.java:220-231`
- **问题**：`complete()` 标注 `@Transactional`，内部 `creditService.addCredit()` 亦为 `@Transactional`（REQUIRED 传播），共享同一事务。addCredit 抛 RuntimeException 时外层 try-catch 吞异常，但事务已标记 rollback-only，提交时抛 `UnexpectedRollbackException`，认领完成整体回滚
- **修复**：积分奖励移到事务外（complete 方法去掉 @Transactional 或 addCredit 用 REQUIRES_NEW）

### 3. CreditController.deduct 越权 + score 可传负数
- **位置**：`src/main/java/com/campus/lostfound/controller/CreditController.java:49-55`
- **问题**：`/api/credit/deduct` 未在 RoleInterceptor 拦截列表，任何登录用户都能扣他人积分；`score` 无范围校验，传负数变加分
- **修复**：① WebMvcConfig 把 `/api/credit/deduct` 加入 roleInterceptor 拦截；② 校验 `score > 0 && score <= 1000`

### 4. 5 个 Controller 重复实现 getCurrentUserId 绕过 BaseController
- **位置**：LostItemController/FoundItemController/ClaimController/AnnouncementController/UserController
- **问题**：未继承 BaseController，重复实现 JWT 解析，绕过 Redis 注销校验设计
- **修复**：让这 5 个 Controller extends BaseController，删除私有 getCurrentUserId，敏感操作用 requireUserId

### 5. CommentController.add / FavoriteController.toggle 缺参数校验
- **位置**：`CommentController.java:46-53`、`FavoriteController.java:32-37`
- **问题**：用 Map 接收请求体，parseLong/parseString 失败返回 null，未校验就传 service，易 NPE
- **修复**：方法内显式校验 itemId/itemType/content 非空，否则抛 BizException(PARAM_ERROR)

### 6. 分页参数无边界校验（7 处）
- **位置**：LostItemController/FoundItemController/SearchController/CommentController/FavoriteController/MessageController/CreditController 的 list 方法
- **问题**：可传 `?size=99999` 拉取全表
- **修复**：每个方法首部加 `if (page < 1) page = 1; if (size < 1 || size > 50) size = 10;`

### 7. PathVariable id 无正数校验（多处）
- **位置**：LostItemController/FoundItemController/AnnouncementController/ClaimController/CommentController/MessageController 的 detail/update/delete
- **修复**：加 `if (id == null || id <= 0) throw new BizException(ResultCode.PARAM_ERROR);`

### 8. GlobalFilter CORS `*` 与 Cookie 认证冲突
- **位置**：`GlobalFilter.java:52`
- **问题**：`Allow-Origin: *` 导致跨域携带 Cookie 失效，Cookie Token 注入逻辑形同虚设
- **修复**：改为回显 Origin 白名单 + `Allow-Credentials: true`

### 9. RecommendServiceImpl 严重 N+1 查询
- **位置**：`RecommendServiceImpl.java:103-131`
- **问题**：两层嵌套循环查询，单次推荐可触发数百次 DB 查询
- **修复**：用 IN 批量查询（YAGNI：本次先加缓存或限制，深度优化留后续）

### 10. UserController.updateStatus status 无范围校验
- **位置**：`UserController.java:107-109`
- **修复**：`if (status == null || (status != 0 && status != 1)) throw new BizException(PARAM_ERROR);`

### 11. SearchController keyword 空字符串全表扫描
- **位置**：`SearchController.java:39`
- **修复**：空关键词直接返回空结果

---

## P1 问题（建议修复 - 本次修复关键项）

### 1. FoundItemServiceImpl.delete 未清匹配缓存
- **位置**：`FoundItemServiceImpl.java:131-155`
- **修复**：deleteById 后追加 `clearMatchCache();`

### 2. ClaimServiceImpl.approve 未校验 lost_item 状态回退
- **位置**：`ClaimServiceImpl.java:128-134`
- **修复**：`if (lostItem.getStatus() != Constants.STATUS_CLOSED) { ... }`

### 3. AnnouncementServiceImpl update/delete 无权限校验
- **位置**：`AnnouncementServiceImpl.java:59-66, 69-76`
- **修复**：校验 userId 角色为管理员

### 4. TokenRequestWrapper 未覆盖 getHeaders/getHeaderNames
- **位置**：`GlobalFilter.java:114-129`
- **修复**：补齐两个方法重写

### 5. MyBatisPlusConfig 无 setMaxLimit
- **位置**：`MyBatisPlusConfig.java:17-21`
- **修复**：`pagination.setMaxLimit(500L);`

### 6. RedisConfig 无 JavaTimeModule
- **位置**：`RedisConfig.java:25`
- **修复**：注入带 JavaTimeModule 的 ObjectMapper

### 7. 孤儿端点接线（作业要求"遗漏功能"）
- **LocationController 4 个接口** → 接到 detail.jsp/publish.jsp
- **ExportController 4 个接口** → 接到 admin 页面或列表页导出按钮
- **LostItem/FoundItem update** → 接到 detail.jsp 编辑功能（或暂不接，标记为预留）

---

## P2 问题（可选 - 本次不做）

- 代码风格不一致（通配符 import、Swagger 注解不统一）
- RoleInterceptor 每次查 DB 校验角色
- 循环内逐条 updateById
- CreditServiceImpl 无乐观锁
- MessageServiceImpl 未读计数非原子

---

## 作业要求符合性检查（11 项）

| 要求 | 状态 | 说明 |
|---|---|---|
| Session/Cookie 状态管理 | ✓ 达标 | GlobalFilter 从 Cookie 提取 Token，LoginInterceptor 校验 JWT+Redis |
| ≥4 张表 + SQL 脚本 | ✓ 达标 | 13 表 + init.sql + migration_v2.sql |
| 完整 CRUD | ✓ 达标 | Lost/Found/User/Announcement/Comment 均含增删改查 |
| Servlet≥5 / Bean+DAO≥5 | ✓ 达标 | 19 Controller + 13 Entity + 13 Mapper |
| JSP≥10 | ✓ 达标 | 18 个 JSP |
| 禁止 JSP 脚本片段 | ✓ 达标 | 8 处 `<%-- --%>` 均为注释，无脚本片段 |
| EL + JSTL ≥2 种 | ✓ 达标 | header.jsp 声明 c/fmt/fn 三种 |
| DAO 模式封装 JDBC | ✓ 达标 | 全走 MyBatis-Plus Mapper，无 DB 直连 |
| 1 个 Filter | ✓ 达标 | GlobalFilter @WebFilter("/*") |
| 静态资源独立文件夹 | ✓ 达标 | static/css/、static/js/ 独立 |
| Servlet/JSP 禁止 DB 连接 | ✓ 达标 | 仅 RedisUtil ping 用 getConnection（非 DB） |

**结论**：作业要求 11 项全部达标。

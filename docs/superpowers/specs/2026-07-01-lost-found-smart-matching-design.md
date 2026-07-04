# 校园失物招领智能匹配系统 — 设计文档

> **项目名称**: Campus Lost & Found Smart Matching System
> **课程**: 五邑大学《Java Web开发技术》课程大作业（进阶方案）
> **日期**: 2026-07-01
> **目标**: 满足课程进阶方案全部要求 + 可作为大厂面试简历项目

---

## 一、系统设计的意义及目的

校园失物招领是学生高频痛点场景。传统失物招依赖人工浏览比对，效率低下。本系统引入 TF-IDF + 余弦相似度文本匹配算法，当用户发布丢失物品描述后，系统自动从拾到物品库中检索并计算语义相似度，按匹配度排序推荐，显著提升找回效率。同时通过 Redis 缓存优化匹配性能、WebSocket 实时推送匹配通知、ECharts 数据可视化展示找回统计，构建一个技术完整、工程规范的智能失物招领平台。

**双目标定位**：
1. 课程目标：满足进阶方案全部要求（持久层框架+MVC框架+异步交互+代码文件>25），视频答辩技术讲解清晰
2. 面试目标：涵盖算法实现、缓存设计、实时通信、权限控制等大厂面试高频考点

---

## 二、开发环境与技术选型

### 2.1 开发环境

| 项 | 版本 |
|---|---|
| JDK | 1.8 |
| Maven | 3.6+ |
| MySQL | 8.0 |
| Redis | 6.0+ |
| IDE | IntelliJ IDEA |
| 服务器 | Spring Boot 内嵌 Tomcat |

### 2.2 技术选型（对照报告模板打钩项）

- [x] **核心框架：Spring Boot 2.7 + Spring MVC**
- [x] **持久层框架：MyBatis-Plus 3.5**
- [x] **前端交互：AJAX / JSON**（jQuery AJAX + JSON 异步交互）
- [x] **辅助技术：Filter/Interceptor**（登录拦截器 + 角色权限拦截器）
- [x] EL表达式 + JSTL标签库（JSP页面使用，无scriptlet脚本片段）

### 2.3 增强技术（Spring Boot 生态自然延伸，非偏离）

| 技术 | 版本 | 用途 | 面试价值 |
|---|---|---|---|
| Redis | 6.0+ | 匹配结果缓存/热点数据/Token存储 | 缓存设计高频考点 |
| WebSocket | spring-boot-starter-websocket | 匹配成功/认领消息实时推送 | 实时通信面试题 |
| JWT | jjwt 0.9 | 无状态身份认证 | 认证方案对比面试题 |
| ECharts | 5.x | 数据看板可视化 | — |
| layui | 2.x | 前端UI框架 | — |
| Lombok | 1.18 | 简化实体类代码 | — |

### 2.4 合规性验证

| 要求来源 | 要求 | 选型 | 合规 |
|---|---|---|---|
| 考核说明 P5 进阶方案 | 持久层框架 | MyBatis-Plus | ✓ |
| 考核说明 P5 进阶方案 | MVC框架 | Spring Boot + Spring MVC | ✓ |
| 考核说明 P5 进阶方案 | 异步交互(AJAX/JSON) | jQuery AJAX + JSON | ✓ |
| 考核说明 P5 进阶方案 | 禁止JSP脚本片段 | EL表达式 + JSTL | ✓ |
| 用户要求 | 代码文件数>25(不含静态资源) | 预估60+ Java文件 | ✓ |
| 考核说明 P4 红线1 | 真实数据库交互 | MySQL + MyBatis-Plus真实CRUD | ✓ |
| 考核说明 P4 红线2 | 包含SQL初始化脚本 | 提供 lost_found.sql | ✓ |
| 考核说明 P6 提交物 | 源代码+报告+视频 | 全部提供 | ✓ |

---

## 三、系统实现的功能及总体设计

### 3.1 功能模块图

```
校园失物招领智能匹配系统
├── 用户模块
│   ├── 注册/登录(JWT认证)
│   ├── 个人信息管理(修改资料/头像)
│   └── 角色权限(学生/管理员)
├── 失物发布模块
│   ├── 发布丢失物品(含图片上传)
│   ├── 发布拾到物品(含图片上传)
│   ├── 编辑/删除/关闭自己发布的物品
│   └── 分类浏览/关键词搜索
├── 智能匹配模块 ★核心创新
│   ├── TF-IDF特征提取(中文分词+词频统计)
│   ├── 余弦相似度计算(向量化+相似度排序)
│   ├── 匹配结果Redis缓存(TTL=30min)
│   └── 匹配成功WebSocket实时推送
├── 认领流程模块
│   ├── 发起认领申请(基于匹配结果或直接认领)
│   ├── 发布人审核(通过/拒绝)
│   ├── 认领完成确认
│   └── 状态流转通知(WebSocket+站内消息)
├── 公告管理模块
│   ├── 管理员发布/编辑/删除公告
│   └── 首页公告展示
├── 数据看板模块
│   ├── 找回率统计(总体/分类)
│   ├── 品类丢失趋势(折线图)
│   └── 地点分布(柱状图)
└── 系统管理模块
    ├── 操作日志记录(AOP)
    ├── 用户管理(管理员禁用/启用)
    └── 定时归档(超期30天未认领自动关闭)
```

### 3.2 业务流程

**核心流程 — 失物发布到智能匹配**：

1. 用户A发布丢失物品（填写标题、描述、分类、地点、时间、图片）
2. 系统保存到 lost_item 表，状态=0(待处理)
3. MatchService.findMatches() 被触发：
   - 查询所有 status=0 的 found_item 记录
   - 对每条 found_item 与该 lost_item 进行 TF-IDF 匹配
   - 按相似度降序排序，取 Top10
   - 结果写入 Redis（key=match:lost:{itemId}，TTL=30min）
4. 若 Top1 相似度 > 0.3，通过 WebSocket 推送匹配通知给用户A
5. 用户A查看匹配列表，点击"发起认领"
6. 创建 claim 记录，状态=0(待审核)
7. WebSocket 通知 found_item 发布者用户B
8. 用户B审核认领（通过→状态1 / 拒绝→状态2）
9. 通过后用户A确认完成（状态3），lost_item/found_item 状态改为2(已关闭)

---

## 四、数据库设计

### 4.1 E-R 关系

```
user ──1:N──→ lost_item ←──N:1── claim ──N:1──→ found_item
  │                                          ↑
  │              user ──1:N──→ found_item ───┘
  │
  ├──1:N──→ announcement
  └──1:N──→ operation_log
```

### 4.2 表结构详细设计

#### 4.2.1 user（用户表）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 用户名 |
| password | VARCHAR(100) | NOT NULL | 密码(BCrypt加密) |
| real_name | VARCHAR(50) | | 真实姓名 |
| phone | VARCHAR(20) | | 联系电话 |
| email | VARCHAR(100) | | 邮箱 |
| avatar | VARCHAR(255) | | 头像路径 |
| role | TINYINT | DEFAULT 0 | 角色:0学生/1管理员 |
| status | TINYINT | DEFAULT 1 | 状态:0禁用/1正常 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

#### 4.2.2 lost_item（丢失物品表）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | FK→user.id, NOT NULL | 发布者ID |
| title | VARCHAR(100) | NOT NULL | 物品标题 |
| description | TEXT | NOT NULL | 详细描述(用于TF-IDF匹配) |
| category | VARCHAR(30) | NOT NULL | 分类:electronics/certificate/book/clothing/other |
| location | VARCHAR(200) | NOT NULL | 丢失地点 |
| event_time | DATETIME | NOT NULL | 丢失时间 |
| contact_info | VARCHAR(200) | | 联系方式 |
| images | VARCHAR(500) | | 图片路径(JSON数组字符串) |
| status | TINYINT | DEFAULT 0 | 0待处理/1已匹配/2已关闭 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | |

索引: idx_user_id(user_id), idx_status(status), idx_category(category)

#### 4.2.3 found_item（拾到物品表）

结构与 lost_item 完全对称，字段含义将"丢失"替换为"拾到"。

#### 4.2.4 claim（认领记录表）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| lost_item_id | BIGINT | FK→lost_item.id | 丢失物品ID |
| found_item_id | BIGINT | FK→found_item.id | 拾到物品ID(可空,直接认领) |
| claimant_id | BIGINT | FK→user.id, NOT NULL | 认领人ID |
| respondent_id | BIGINT | FK→user.id, NOT NULL | 发布人ID |
| match_score | DECIMAL(5,4) | | TF-IDF匹配分数(0~1) |
| status | TINYINT | DEFAULT 0 | 0待审核/1已通过/2已拒绝/3已完成 |
| remark | VARCHAR(500) | | 认领备注 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | |

#### 4.2.5 announcement（公告表）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| title | VARCHAR(200) | NOT NULL | 公告标题 |
| content | TEXT | NOT NULL | 公告内容 |
| publisher_id | BIGINT | FK→user.id | 发布人ID |
| status | TINYINT | DEFAULT 1 | 0下架/1发布 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | |

#### 4.2.6 operation_log（操作日志表）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | FK→user.id | 操作用户ID |
| username | VARCHAR(50) | | 用户名(冗余,便于查询) |
| operation | VARCHAR(100) | NOT NULL | 操作描述 |
| method | VARCHAR(200) | | 请求方法 |
| params | TEXT | | 请求参数 |
| ip | VARCHAR(50) | | 请求IP |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 操作时间 |

---

## 五、系统架构及关键技术说明

### 5.1 分层架构

```
┌─────────────────────────────────────────────────────┐
│              前端展示层 (View)                        │
│   JSP + jQuery + AJAX + layui + ECharts              │
│   页面渲染(EL+JSTL) + 异步请求(JSON) + 图表 + WS客户端 │
├─────────────────────────────────────────────────────┤
│              控制层 (Controller)                      │
│   Spring MVC @Controller / @RestController           │
│   请求路由 + 参数校验(@Valid) + 统一响应(Result<T>)    │
├─────────────────────────────────────────────────────┤
│              业务逻辑层 (Service)                      │
│   接口 + Impl实现类, 事务管理(@Transactional)          │
│   核心算法: TF-IDF匹配引擎 / 认领流程状态机            │
├─────────────────────────────────────────────────────┤
│              数据访问层 (Mapper/DAO)                  │
│   MyBatis-Plus BaseMapper + 自定义XML查询             │
├─────────────────────────────────────────────────────┤
│              数据库 (MySQL 8.0)                       │
│   6张表: user/lost_item/found_item/claim/            │
│   announcement/operation_log                          │
└─────────────────────────────────────────────────────┘
         │                    │              │
    ┌────┴────┐         ┌─────┴─────┐  ┌────┴────┐
    │  Redis  │         │ WebSocket │  │  JWT    │
    │  缓存   │         │  实时推送  │  │ 认证    │
    └─────────┘         └───────────┘  └─────────┘
```

### 5.2 包结构与文件清单（62个Java文件）

```
com.campus.lostfound
├── LostFoundApplication.java           # 启动类 (1)
├── config/                             # 配置类 (5)
│   ├── WebMvcConfig                       # MVC配置+拦截器注册
│   ├── RedisConfig                        # Redis序列化配置
│   ├── WebSocketConfig                    # WebSocket端点配置
│   ├── MyBatisPlusConfig                  # 分页插件配置
│   └── interceptor/
│       ├── LoginInterceptor               # 登录拦截器
│       └── RoleInterceptor                # 角色权限拦截器
├── common/                             # 公共组件 (9)
│   ├── result/
│   │   ├── Result<T>                      # 统一响应封装
│   │   └── ResultCode                     # 响应码枚举
│   ├── exception/
│   │   ├── BizException                   # 业务异常
│   │   └── GlobalExceptionHandler         # 全局异常处理器
│   ├── constant/
│   │   └── Constants                      # 系统常量
│   └── utils/
│       ├── JwtUtil                        # JWT生成/验证
│       ├── RedisUtil                      # Redis操作封装
│       ├── FileUploadUtil                 # 文件上传工具
│       ├── TfidfUtil                      # TF-IDF算法核心
│       └── CosineSimilarityUtil           # 余弦相似度计算
├── entity/                             # 实体类 (6)
│   ├── User / LostItem / FoundItem
│   ├── Claim / Announcement / OperationLog
├── dto/                                # 数据传输对象 (6)
│   ├── LoginDTO / RegisterDTO
│   ├── LostItemDTO / FoundItemDTO
│   ├── MatchResultDTO / DashboardQueryDTO
├── vo/                                 # 视图对象 (5)
│   ├── UserVO / LostItemVO / FoundItemVO
│   ├── MatchResultVO / DashboardVO
├── mapper/                             # 数据访问层 (6)
│   ├── UserMapper / LostItemMapper / FoundItemMapper
│   ├── ClaimMapper / AnnouncementMapper / OperationLogMapper
├── service/                            # 业务层 (14 = 7接口+7实现)
│   ├── UserService + Impl
│   ├── LostItemService + Impl
│   ├── FoundItemService + Impl
│   ├── MatchService + Impl               # 核心匹配服务
│   ├── ClaimService + Impl
│   ├── AnnouncementService + Impl
│   └── DashboardService + Impl
├── controller/                         # 控制层 (8)
│   ├── UserController / LostItemController
│   ├── FoundItemController / MatchController
│   ├── ClaimController / AnnouncementController
│   ├── DashboardController / FileController
├── websocket/                          # WebSocket (1)
│   └── WebSocketServer
└── task/                               # 定时任务 (1)
    └── ScheduledTask
```

文件统计: 1(启动) + 5(config) + 9(common) + 6(entity) + 6(dto) + 5(vo) + 6(mapper) + 14(service) + 8(controller) + 1(websocket) + 1(task) = **62个Java文件**

### 5.3 关键技术实现

#### 5.3.1 TF-IDF 匹配引擎（核心创新）

**算法流程**:

```
输入: lostItem.description = "黑色钱包 里面有学生证 钱包"
     foundItem.description = "捡到一个黑色钱包 含身份证件"

Step1: 中文分词(按标点+空格分割, 去停用词)
  → ["黑色","钱包","里面","学生证"] / ["捡到","一个","黑色","钱包","含","身份证件"]

Step2: 计算TF(词频/文档总词数)
  → 钱包: 1/4=0.25 / 1/6=0.17

Step3: 计算IDF(log(总文档数/包含该词的文档数))
  → 钱包: log(2/2)=0 (两篇都含,无区分度)
  → 黑色: log(2/2)=0
  → 学生证: log(2/1)=0.693

Step4: TF-IDF = TF × IDF → 构建特征向量

Step5: 余弦相似度 = dot(v1,v2) / (|v1| × |v2|)
  → 相似度范围[0,1], 越接近1越相似

Step6: 对所有found_item计算, 排序取Top10
```

**核心类设计**:

- `TfidfUtil`: 负责分词、词频统计、TF计算、IDF计算、特征向量构建
- `CosineSimilarityUtil`: 负责两个向量的余弦相似度计算
- `MatchService.findMatches(Long lostItemId)`: 编排完整匹配流程

**缓存策略**: 匹配结果写入 Redis (key=`match:lost:{itemId}`, TTL=30min), 物品状态变更时清除缓存

#### 5.3.2 Redis 缓存设计

| 缓存Key | 值 | TTL | 场景 |
|---|---|---|---|
| `match:lost:{itemId}` | 匹配结果JSON | 30min | 物品匹配结果缓存 |
| `item:hot` | 热门物品列表 | 10min | 首页热门展示 |
| `user:token:{userId}` | JWT Token | 24h | 登录状态维护 |
| `item:view:{itemId}` | 浏览量(incr) | 不过期 | 浏览计数 |

#### 5.3.3 WebSocket 实时推送

- `WebSocketConfig` 注册 `/ws/{userId}` 端点
- `WebSocketServer` 管理在线用户 Session 映射
- 推送场景: 匹配成功通知、认领申请通知、认领审核结果通知
- 消息格式: `{"type":"match","title":"...","score":0.85,"itemId":123}`

#### 5.3.4 JWT 认证与权限

- 登录成功生成 JWT Token (有效期24h), 存入 Redis
- `LoginInterceptor` 拦截非白名单请求, 验证 Token
- `RoleInterceptor` 校验管理员接口权限 (role=1)
- 前端 Token 存储在 localStorage, AJAX 请求头携带 `Authorization: Bearer {token}`

#### 5.3.5 统一响应与异常处理

```java
// 统一响应
Result<T> { Integer code; String msg; T data; }
// 成功: Result.success(data) → {200, "success", data}
// 失败: Result.fail(ResultCode.PARAM_ERROR) → {400, "参数错误", null}

// 异常分类
GlobalExceptionHandler (@RestControllerAdvice):
  - BizException → 返回业务错误信息
  - MethodArgumentNotValidException → 返回参数校验错误
  - RuntimeException → 返回"系统繁忙,请稍后重试"
```

#### 5.3.6 文件上传

- `FileController` 接收 MultipartFile
- `FileUploadUtil` 校验文件类型(jpg/png/jpeg/gif)、大小(<5MB)
- 存储路径: `/upload/items/{yyyy/MM/dd}/{uuid}.{ext}`
- 返回访问 URL, 前端通过 `<img>` 或 layui 图片预览展示

#### 5.3.7 定时任务

- `@Scheduled(cron = "0 0 2 * * ?")` 每日凌晨2点执行
- 扫描 status=0 且创建时间超过30天的 lost_item/found_item
- 自动设为 status=2(已关闭), 释放 Redis 缓存

### 5.4 前端设计

#### JSP 页面清单 (15个, 超过基础方案10个要求)

| 页面 | 路径 | 功能 | AJAX |
|---|---|---|---|
| 登录页 | /login | 用户登录 | 登录接口 |
| 注册页 | /register | 用户注册 | 注册接口 |
| 首页 | /index | 公告+热门物品+统计概览 | 热门列表 |
| 丢失物品列表 | /lost/list | 分页浏览+搜索+筛选 | 列表接口 |
| 拾到物品列表 | /found/list | 分页浏览+搜索+筛选 | 列表接口 |
| 发布丢失物品 | /lost/publish | 表单+图片上传 | 发布+上传接口 |
| 发布拾到物品 | /found/publish | 表单+图片上传 | 发布+上传接口 |
| 物品详情 | /item/detail | 详情+匹配列表+认领 | 详情+匹配接口 |
| 匹配结果 | /match/result | TF-IDF匹配结果展示 | 匹配接口 |
| 我的认领 | /claim/my | 认领记录列表 | 认领列表接口 |
| 个人中心 | /user/profile | 资料修改+头像上传 | 修改接口 |
| 数据看板 | /dashboard | ECharts图表 | 统计接口 |
| 公告管理 | /admin/announcement | 管理员CRUD | 公告接口 |
| 用户管理 | /admin/users | 管理员管理用户 | 用户接口 |
| 操作日志 | /admin/logs | 操作日志查看 | 日志接口 |

#### 前端技术规范

- JSP 使用 EL 表达式 + JSTL 标签 (c:forEach, c:if, fmt:formatDate 至少2种)
- 禁止 JSP 脚本片段 (<% %>)
- AJAX 请求返回 JSON, 前端动态渲染
- layui 提供表格/表单/弹窗/分页组件
- ECharts 渲染数据看板图表

---

## 六、错误处理与安全

### 6.1 错误处理

- **统一响应**: 所有接口返回 `Result<T>`, 含 code/msg/data
- **全局异常**: `@RestControllerAdvice` 捕获并分类处理
- **业务异常**: `BizException` 携带具体错误信息
- **参数校验**: `@Valid` + `@NotBlank/@Size/@Pattern` 注解
- **事务管理**: `@Transactional(rollbackFor = Exception.class)`

### 6.2 安全措施

- 密码 BCrypt 加密存储
- JWT Token 认证, Redis 存储 Token 状态
- 拦截器校验登录状态和角色权限
- SQL 注入防护: MyBatis-Plus 参数化查询
- XSS 防护: JSTL `<c:out>` 转义输出
- 文件上传类型和大小限制
- 数据归属校验: 操作前验证资源归属当前用户

---

## 七、测试策略

### 7.1 单元测试

- `TfidfUtilTest`: 验证分词、TF计算、IDF计算、特征向量构建正确性
- `CosineSimilarityUtilTest`: 验证相似度计算(相同文本=1, 完全不同=0, 边界值)
- `MatchServiceImplTest`: 验证匹配流程编排逻辑(Mock Mapper)

### 7.2 集成测试

- 核心接口测试: 登录/发布物品/匹配/认领全流程
- Redis 缓存命中/失效测试
- WebSocket 连接和消息推送测试

### 7.3 手动测试

- JSP 页面功能验证(15个页面)
- 角色权限验证(学生 vs 管理员)
- 文件上传验证(类型/大小限制)

---

## 八、成果提交清单

| 提交物 | 格式 | 内容 |
|---|---|---|
| 源代码 | 完整工程 + WAR包 | 62+ Java文件 + 15 JSP页面 + SQL脚本 |
| 数据库脚本 | lost_found.sql | 6张表建表语句 + 初始数据 |
| 实验报告 | .docx | 按报告模板撰写, 2000字以上 |
| 演示视频 | .mp4 | 5-10分钟, 重点技术讲解 |

---

## 九、面试价值说明

| 技术点 | 面试可讲内容 |
|---|---|
| TF-IDF算法 | "我实现了TF-IDF文本相似度匹配, 包括中文分词、特征提取、余弦相似度计算, 非调库纯Java实现" |
| Redis缓存 | "匹配结果用Redis缓存, TTL=30min, 物品状态变更时主动清除缓存, 保证数据一致性" |
| WebSocket | "实现了基于WebSocket的实时消息推送, 用于匹配成功通知和认领流程通知" |
| JWT认证 | "采用JWT无状态认证+Redis存储Token状态, 对比Session方案的优势" |
| 分层架构 | "标准Controller-Service-Mapper分层, 面向接口编程, Service层接口+实现分离" |
| 工程规范 | "统一响应格式Result<T>、全局异常处理、参数校验、事务管理" |

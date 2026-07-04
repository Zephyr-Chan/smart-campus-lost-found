# 校园失物招领智能匹配系统 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个基于Spring Boot + MyBatis-Plus + JSP/AJAX的校园失物招领智能匹配系统，含TF-IDF算法匹配引擎、Redis缓存、WebSocket实时推送、ECharts数据看板。

**Architecture:** 四层分层架构(Controller-Service-Mapper-Entity)，JSP+AJAX混合前端，Redis缓存匹配结果，WebSocket推送实时通知，JWT无状态认证。

**Tech Stack:** Spring Boot 2.7, MyBatis-Plus 3.5, MySQL 8.0, Redis 6.0, JSP+jQuery+layui+ECharts, JWT, WebSocket, Lombok, Maven

---

## File Structure

```
e:\Java_Code\JAVAWEBHW\
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/campus/lostfound/
│   │   │   ├── LostFoundApplication.java
│   │   │   ├── config/
│   │   │   │   ├── WebMvcConfig.java
│   │   │   │   ├── RedisConfig.java
│   │   │   │   ├── WebSocketConfig.java
│   │   │   │   ├── MyBatisPlusConfig.java
│   │   │   │   └── interceptor/
│   │   │   │       ├── LoginInterceptor.java
│   │   │   │       └── RoleInterceptor.java
│   │   │   ├── common/
│   │   │   │   ├── result/Result.java, ResultCode.java
│   │   │   │   ├── exception/BizException.java, GlobalExceptionHandler.java
│   │   │   │   ├── constant/Constants.java
│   │   │   │   └── utils/JwtUtil.java, RedisUtil.java, FileUploadUtil.java,
│   │   │   │              TfidfUtil.java, CosineSimilarityUtil.java
│   │   │   ├── entity/ (6: User, LostItem, FoundItem, Claim, Announcement, OperationLog)
│   │   │   ├── dto/ (6: LoginDTO, RegisterDTO, LostItemDTO, FoundItemDTO, MatchResultDTO, DashboardQueryDTO)
│   │   │   ├── vo/ (5: UserVO, LostItemVO, FoundItemVO, MatchResultVO, DashboardVO)
│   │   │   ├── mapper/ (6: UserMapper, LostItemMapper, FoundItemMapper, ClaimMapper, AnnouncementMapper, OperationLogMapper)
│   │   │   ├── service/ (7 interfaces + 7 impls)
│   │   │   ├── controller/ (8: User, LostItem, FoundItem, Match, Claim, Announcement, Dashboard, File)
│   │   │   ├── websocket/WebSocketServer.java
│   │   │   └── task/ScheduledTask.java
│   │   ├── resources/
│   │   │   ├── application.yml
│   │   │   ├── mapper/ (6 XML files)
│   │   │   └── static/ (css/js/img/lib)
│   │   └── webapp/
│   │       ├── WEB-INF/jsp/ (15 JSP pages)
│   │       └── static/ (css/js/img)
│   └── test/java/com/campus/lostfound/
│       ├── TfidfUtilTest.java
│       └── CosineSimilarityUtilTest.java
├── sql/lost_found.sql
└── docs/superpowers/
    ├── specs/2026-07-01-lost-found-smart-matching-design.md
    └── plans/2026-07-01-lost-found-smart-matching.md (this file)
```

---

## Task 1: 项目脚手架 — Maven + Spring Boot配置

**Files:**
- Create: `pom.xml`
- Create: `src/main/resources/application.yml`
- Create: `src/main/java/com/campus/lostfound/LostFoundApplication.java`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
    </parent>
    <groupId>com.campus</groupId>
    <artifactId>lost-found</artifactId>
    <version>1.0.0</version>
    <packaging>war</packaging>
    <name>campus-lost-found</name>

    <properties>
        <java.version>1.8</java.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <jwt.version>0.9.1</jwt.version>
    </properties>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- JSP -->
        <dependency>
            <groupId>org.apache.tomcat.embed</groupId>
            <artifactId>tomcat-embed-jasper</artifactId>
        </dependency>
        <!-- JSTL -->
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>jstl</artifactId>
        </dependency>
        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <!-- MySQL -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.33</version>
        </dependency>
        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <!-- WebSocket -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt</artifactId>
            <version>${jwt.version}</version>
        </dependency>
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <!-- Jackson -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 application.yml**

```yaml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lost_found?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: localhost
    port: 6379
    database: 0
    timeout: 3000ms
  mvc:
    view:
      prefix: /WEB-INF/jsp/
      suffix: .jsp
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 10MB
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

# JWT
jwt:
  secret: CampusLostFoundSecretKey2026
  expiration: 86400000

# 文件上传路径
file:
  upload-path: D:/uploads/lost-found/
  access-path: /upload/
```

- [ ] **Step 3: 创建启动类**

```java
package com.campus.lostfound;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.campus.lostfound.mapper")
@EnableScheduling
public class LostFoundApplication {
    public static void main(String[] args) {
        SpringApplication.run(LostFoundApplication.class, args);
    }
}
```

- [ ] **Step 4: 验证项目可编译**

Run: `mvn compile -f e:\Java_Code\JAVAWEBHW\pom.xml`
Expected: BUILD SUCCESS

---

## Task 2: 数据库脚本

**Files:**
- Create: `sql/lost_found.sql`

- [ ] **Step 1: 创建建表SQL + 初始数据**

包含6张表(user, lost_item, found_item, claim, announcement, operation_log)的CREATE TABLE语句，含索引、外键注释，以及1条管理员初始数据。

- [ ] **Step 2: 在MySQL中执行SQL验证**

Run: `mysql -u root -proot < sql/lost_found.sql`
Expected: 6张表创建成功

---

## Task 3: 公共组件 — 统一响应/异常/常量

**Files:**
- Create: `src/main/java/com/campus/lostfound/common/result/Result.java`
- Create: `src/main/java/com/campus/lostfound/common/result/ResultCode.java`
- Create: `src/main/java/com/campus/lostfound/common/exception/BizException.java`
- Create: `src/main/java/com/campus/lostfound/common/exception/GlobalExceptionHandler.java`
- Create: `src/main/java/com/campus/lostfound/common/constant/Constants.java`

- [ ] **Step 1: 创建Result<T>统一响应封装**
- [ ] **Step 2: 创建ResultCode枚举**
- [ ] **Step 3: 创建BizException业务异常**
- [ ] **Step 4: 创建GlobalExceptionHandler全局异常处理器**
- [ ] **Step 5: 创建Constants常量类**
- [ ] **Step 6: 编译验证**

Run: `mvn compile -f e:\Java_Code\JAVAWEBHW\pom.xml`

---

## Task 4: 实体类 — 6个Entity

**Files:**
- Create: `entity/User.java`, `entity/LostItem.java`, `entity/FoundItem.java`
- Create: `entity/Claim.java`, `entity/Announcement.java`, `entity/OperationLog.java`

- [ ] **Step 1: 创建User实体（@TableName, Lombok @Data, 字段与DB对应）**
- [ ] **Step 2: 创建LostItem实体**
- [ ] **Step 3: 创建FoundItem实体（结构与LostItem对称）**
- [ ] **Step 4: 创建Claim实体**
- [ ] **Step 5: 创建Announcement实体**
- [ ] **Step 6: 创建OperationLog实体**
- [ ] **Step 7: 编译验证**

---

## Task 5: DTO与VO类

**Files:**
- Create: 6个DTO (LoginDTO, RegisterDTO, LostItemDTO, FoundItemDTO, MatchResultDTO, DashboardQueryDTO)
- Create: 5个VO (UserVO, LostItemVO, FoundItemVO, MatchResultVO, DashboardVO)

- [ ] **Step 1-6: 逐个创建DTO类（含@NotBlank/@Size等校验注解）**
- [ ] **Step 7-11: 逐个创建VO类**
- [ ] **Step 12: 编译验证**

---

## Task 6: Mapper接口 — 6个Mapper

**Files:**
- Create: `mapper/UserMapper.java` ... `mapper/OperationLogMapper.java`
- Create: `src/main/resources/mapper/UserMapper.xml` ... `OperationLogMapper.xml`

- [ ] **Step 1-6: 创建6个Mapper接口（继承BaseMapper<T>，自定义查询方法）**
- [ ] **Step 7-12: 创建6个Mapper XML（自定义SQL：如Dashboard统计查询）**
- [ ] **Step 13: 编译验证**

---

## Task 7: TF-IDF算法工具类（核心创新）+ 单元测试

**Files:**
- Create: `common/utils/TfidfUtil.java`
- Create: `common/utils/CosineSimilarityUtil.java`
- Test: `src/test/java/com/campus/lostfound/TfidfUtilTest.java`
- Test: `src/test/java/com/campus/lostfound/CosineSimilarityUtilTest.java`

- [ ] **Step 1: 编写TfidfUtil测试（分词、TF计算、IDF计算、特征向量）**
- [ ] **Step 2: 运行测试验证失败**
- [ ] **Step 3: 实现TfidfUtil（tokenize, computeTF, computeIDF, buildTfIdfVector）**
- [ ] **Step 4: 运行测试验证通过**
- [ ] **Step 5: 编写CosineSimilarityUtil测试（相同文本=1, 完全不同=0, 部分相似）**
- [ ] **Step 6: 运行测试验证失败**
- [ ] **Step 7: 实现CosineSimilarityUtil（calculate方法）**
- [ ] **Step 8: 运行测试验证通过**
- [ ] **Step 9: Commit**

---

## Task 8: 其他工具类 — JWT/Redis/文件上传

**Files:**
- Create: `common/utils/JwtUtil.java`, `common/utils/RedisUtil.java`, `common/utils/FileUploadUtil.java`

- [ ] **Step 1: 创建JwtUtil（generateToken, parseToken, validateToken）**
- [ ] **Step 2: 创建RedisUtil（set, get, delete, expire, incr）**
- [ ] **Step 3: 创建FileUploadUtil（upload, validateFileType, validateFileSize）**
- [ ] **Step 4: 编译验证**

---

## Task 9: 配置类 — MVC/Redis/WebSocket/MyBatisPlus

**Files:**
- Create: `config/WebMvcConfig.java`, `config/RedisConfig.java`, `config/WebSocketConfig.java`, `config/MyBatisPlusConfig.java`
- Create: `config/interceptor/LoginInterceptor.java`, `config/interceptor/RoleInterceptor.java`

- [ ] **Step 1: 创建RedisConfig（RedisTemplate序列化配置）**
- [ ] **Step 2: 创建MyBatisPlusConfig（PaginationInnerInterceptor分页插件）**
- [ ] **Step 3: 创建LoginInterceptor（JWT Token验证）**
- [ ] **Step 4: 创建RoleInterceptor（管理员角色校验）**
- [ ] **Step 5: 创建WebMvcConfig（注册拦截器+静态资源映射+文件上传路径映射）**
- [ ] **Step 6: 创建WebSocketConfig（注册WebSocket端点）**
- [ ] **Step 7: 编译验证**

---

## Task 10: 用户模块 — Service + Controller

**Files:**
- Create: `service/UserService.java`, `service/impl/UserServiceImpl.java`
- Create: `controller/UserController.java`

- [ ] **Step 1: 创建UserService接口（register, login, getUserInfo, updateProfile, updateAvatar）**
- [ ] **Step 2: 实现UserServiceImpl（BCrypt密码加密, JWT生成, Redis存储Token）**
- [ ] **Step 3: 创建UserController（注册/登录/个人信息接口）**
- [ ] **Step 4: 编译验证**

---

## Task 11: 失物/拾物模块 — Service + Controller

**Files:**
- Create: `service/LostItemService.java` + Impl, `service/FoundItemService.java` + Impl
- Create: `controller/LostItemController.java`, `controller/FoundItemController.java`

- [ ] **Step 1: 创建LostItemService接口 + Impl（发布/查询/编辑/删除/关闭，含分页）**
- [ ] **Step 2: 创建FoundItemService接口 + Impl（同LostItem结构）**
- [ ] **Step 3: 创建LostItemController（发布/列表/详情/编辑/删除接口）**
- [ ] **Step 4: 创建FoundItemController（同LostItem结构）**
- [ ] **Step 5: 编译验证**

---

## Task 12: 智能匹配模块 — Service + Controller（核心创新）

**Files:**
- Create: `service/MatchService.java`, `service/impl/MatchServiceImpl.java`
- Create: `controller/MatchController.java`

- [ ] **Step 1: 创建MatchService接口（findMatches: 对lost_item与所有found_item进行TF-IDF匹配）**
- [ ] **Step 2: 实现MatchServiceImpl**
  - 查Redis缓存，命中则直接返回
  - 查询所有status=0的found_item
  - 构建所有文档的IDF词典
  - 对每条found_item与lost_item计算TF-IDF向量+余弦相似度
  - 按相似度降序排序取Top10
  - 结果写入Redis(TTL=30min)
  - 若Top1相似度>0.3，WebSocket推送通知
- [ ] **Step 3: 创建MatchController（/match/{lostItemId}接口）**
- [ ] **Step 4: 编译验证**

---

## Task 13: 认领流程模块 — Service + Controller

**Files:**
- Create: `service/ClaimService.java` + Impl
- Create: `controller/ClaimController.java`

- [ ] **Step 1: 创建ClaimService接口 + Impl（发起认领/审核/确认完成/我的认领列表）**
  - 状态机: 0待审核 → 1已通过 → 3已完成 / 2已拒绝
  - 每次状态变更WebSocket通知相关用户
- [ ] **Step 2: 创建ClaimController（认领CRUD接口）**
- [ ] **Step 3: 编译验证**

---

## Task 14: 公告模块 — Service + Controller

**Files:**
- Create: `service/AnnouncementService.java` + Impl
- Create: `controller/AnnouncementController.java`

- [ ] **Step 1: 创建AnnouncementService接口 + Impl（CRUD + 首页展示）**
- [ ] **Step 2: 创建AnnouncementController（管理员CRUD + 公开列表接口）**
- [ ] **Step 3: 编译验证**

---

## Task 15: 数据看板模块 — Service + Controller

**Files:**
- Create: `service/DashboardService.java` + Impl
- Create: `controller/DashboardController.java`

- [ ] **Step 1: 创建DashboardService接口 + Impl**
  - getRecoveryRate(): 找回率统计
  - getCategoryTrend(): 品类丢失趋势
  - getLocationDistribution(): 地点分布
  - getOverview(): 总览数据(总丢失数/总拾到数/已找回数/待处理数)
- [ ] **Step 2: 在Mapper XML中编写统计SQL**
- [ ] **Step 3: 创建DashboardController（/dashboard/*接口）**
- [ ] **Step 4: 编译验证**

---

## Task 16: 文件上传 + WebSocket + 定时任务

**Files:**
- Create: `controller/FileController.java`
- Create: `websocket/WebSocketServer.java`
- Create: `task/ScheduledTask.java`

- [ ] **Step 1: 创建FileController（/file/upload接口，调用FileUploadUtil）**
- [ ] **Step 2: 创建WebSocketServer（@ServerEndpoint, 管理在线Session, sendMessage方法）**
- [ ] **Step 3: 创建ScheduledTask（@Scheduled每日凌晨2点归档超期物品）**
- [ ] **Step 4: 编译验证**

---

## Task 17: JSP页面 — 15个页面

**Files:**
- Create: `src/main/webapp/WEB-INF/jsp/` 下15个JSP文件
- Create: `src/main/webapp/static/` 下css/js/img/lib静态资源

- [ ] **Step 1: 创建公共头部/尾部JSP片段（header.jsp, footer.jsp）**
- [ ] **Step 2: 创建登录页(login.jsp) — AJAX登录+JWT存储**
- [ ] **Step 3: 创建注册页(register.jsp)**
- [ ] **Step 4: 创建首页(index.jsp) — 公告+热门物品+统计概览**
- [ ] **Step 5: 创建丢失/拾到物品列表页(lost/list.jsp, found/list.jsp) — AJAX分页+搜索**
- [ ] **Step 6: 创建发布丢失/拾到物品页(lost/publish.jsp, found/publish.jsp) — 表单+图片上传**
- [ ] **Step 7: 创建物品详情页(item/detail.jsp) — 详情+匹配列表+认领按钮**
- [ ] **Step 8: 创建匹配结果页(match/result.jsp) — TF-IDF匹配结果展示**
- [ ] **Step 9: 创建我的认领页(claim/my.jsp)**
- [ ] **Step 10: 创建个人中心页(user/profile.jsp)**
- [ ] **Step 11: 创建数据看板页(dashboard.jsp) — ECharts图表**
- [ ] **Step 12: 创建管理员页面(announcement.jsp, users.jsp, logs.jsp)**
- [ ] **Step 13: 引入静态资源(layui/jQuery/ECharts)**

---

## Task 18: 集成测试与验证

- [ ] **Step 1: 启动应用**
- [ ] **Step 2: 验证登录/注册功能**
- [ ] **Step 3: 验证失物/拾物发布功能**
- [ ] **Step 4: 验证TF-IDF智能匹配功能**
- [ ] **Step 5: 验证认领流程功能**
- [ ] **Step 6: 验证WebSocket推送**
- [ ] **Step 7: 验证Redis缓存命中**
- [ ] **Step 8: 验证数据看板**
- [ ] **Step 9: 验证管理员功能**
- [ ] **Step 10: 统计Java文件数确认>25**
- [ ] **Step 11: Commit**

---

## Self-Review

**1. Spec coverage:**
- 持久层框架MyBatis-Plus → Task 6 (Mapper) ✓
- MVC框架Spring Boot → Task 1 (pom.xml) ✓
- AJAX/JSON异步交互 → Task 17 (JSP AJAX) ✓
- 禁止JSP脚本片段(EL+JSTL) → Task 17 (JSP规范) ✓
- 代码文件>25 → Task 1-16 (62个Java文件) ✓
- 真实数据库交互 → Task 2 (SQL) + Task 6 (Mapper) ✓
- SQL初始化脚本 → Task 2 (lost_found.sql) ✓
- TF-IDF算法 → Task 7 (TfidfUtil + CosineSimilarityUtil) ✓
- Redis缓存 → Task 8 (RedisUtil) + Task 12 (MatchService缓存) ✓
- WebSocket推送 → Task 16 (WebSocketServer) ✓
- JWT认证 → Task 8 (JwtUtil) + Task 9 (LoginInterceptor) ✓
- ECharts看板 → Task 15 (Dashboard) + Task 17 (dashboard.jsp) ✓
- 文件上传 → Task 8 (FileUploadUtil) + Task 16 (FileController) ✓
- 定时任务 → Task 16 (ScheduledTask) ✓
- 统一响应/异常 → Task 3 (Result + GlobalExceptionHandler) ✓
- 6张表 → Task 2 (SQL) ✓
- 15个JSP → Task 17 ✓

**2. Placeholder scan:** 无TBD/TODO，每个Task有具体文件和步骤 ✓

**3. Type consistency:**
- Result<T> 在Task 3定义，Task 10-16 Controller中使用 ✓
- TfidfUtil/CosineSimilarityUtil 在Task 7定义，Task 12 MatchService中使用 ✓
- RedisUtil 在Task 8定义，Task 12 MatchService中使用 ✓
- JwtUtil 在Task 8定义，Task 9 LoginInterceptor + Task 10 UserService中使用 ✓
- WebSocketServer.sendMessage() 在Task 16定义，Task 12/13中使用 ✓

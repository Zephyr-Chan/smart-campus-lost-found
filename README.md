# Smart Campus Lost & Found · 智慧校园失物招领系统

> 基于 Spring Boot + MyBatis-Plus + JSP 的全栈校园失物招领平台，集成 TF-IDF 智能匹配、Item-CF 个性化推荐、WebSocket 实时通知、信用积分体系，并采用暮山紫霭视觉设计语言。

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-green)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.5-blue)
![License](https://img.shields.io/badge/License-MIT-success)

---

## ✨ 核心特性

### 业务功能
- **失物/拾物发布**：支持图文、分类、位置、联系信息完整发布
- **智能匹配**：基于 TF-IDF 文本相似度 + GeoHash 地理近邻 + 分类权重的多因子匹配算法
- **认领流程**：申请 → 审核 → 完成，状态机驱动，自动拒绝同物品其他待审核申请
- **信用积分**：发布奖励 +10、认领成功 +20、管理员扣分，BCrypt 加密 + REQUIRES_NEW 事务
- **个性化推荐**：基于用户浏览历史的 Item-CF 协同过滤推荐
- **实时通知**：WebSocket 推送匹配结果、认领申请、积分变动消息
- **管理后台**：用户管理、公告管理、操作日志、数据看板（6 类 ECharts 图表）

### 技术亮点
- **分层架构**：Controller / Service / Mapper 三层 + DTO/VO 隔离
- **跨切面关注点**：GlobalFilter（UTF-8 + CORS + Token 注入）+ LoginInterceptor + RoleInterceptor + OperationLogAspect（AOP）
- **缓存策略**：Redis 缓存 + SCAN 替代 KEYS 防阻塞 + 空结果不缓存 + 更新失效
- **优雅降级**：Redis/RabbitMQ/Elasticsearch/Python AI 任一缺失均可降级运行
- **安全防护**：JWT + Cookie 双通道认证、参数校验、XSS 转义、SQL 注入防护、分页 size 上限
- **视觉设计**：暮山紫霭配色（雾紫 #7C6B9E）+ CSS 变量集中管理 + 响应式（1024px/768px 双断点）

---

## 🛠 技术栈

| 层次 | 技术 |
|------|------|
| 后端框架 | Spring Boot 2.7 / Spring MVC / MyBatis-Plus 3.5 |
| 安全认证 | Spring Security Crypto (BCrypt) + JWT + Redis Token |
| 实时通信 | WebSocket (原生 @ServerEndpoint) |
| 消息队列 | RabbitMQ（可选，自动降级同步） |
| 搜索引擎 | Elasticsearch（可选，自动降级 MySQL LIKE） |
| 缓存 | Redis（Lettuce 客户端） |
| 数据库 | MySQL 8.0 |
| 前端 | JSP + JSTL + EL + layui + ECharts |
| 构建 | Maven |
| AI 匹配服务 | Python FastAPI + Sentence-Transformers（可选） |

---

## 📂 项目结构

```
src/main/java/com/campus/lostfound/
├── controller/         # 19 个 REST 控制器
├── service/            # 19 个业务接口 + 实现类
├── mapper/             # 13 个 MyBatis Mapper
├── entity/             # 14 个实体类
├── dto/vo/             # 数据传输对象 & 视图对象
├── config/             # 配置类（Filter/Interceptor/AOP/Redis/WebSocket）
├── common/             # 通用工具（Result/Constants/Exception/Utils）
├── websocket/          # WebSocket 实时推送
└── task/               # 定时任务

src/main/webapp/
├── WEB-INF/jsp/        # 18 个 JSP 页面（零脚本片段，纯 EL+JSTL）
└── static/css|js/      # 静态资源独立目录

ai-service/             # Python AI 匹配微服务（FastAPI + Sentence-Transformers）
docs/                   # 设计文档 & 技术栈总结
sql/                    # 数据库初始化 & 测试数据脚本
```

---

## 🚀 快速启动

### 环境要求
- **JDK 17**（必须）
- **MySQL 8.0**（必须）
- Redis / RabbitMQ / Elasticsearch / Python（可选，自动降级）

### 三步启动

```bash
# 1. 创建数据库并导入脚本
mysql -uroot -p -e "CREATE DATABASE lost_found DEFAULT CHARACTER SET utf8mb4;"
mysql -uroot -p lost_found < src/main/resources/sql/init.sql
mysql -uroot -p lost_found < src/main/resources/sql/migration_v2.sql
mysql -uroot -p lost_found < sql/demo-data.sql   # 测试数据（可选）

# 2. 修改数据库密码（如非 root）
# 编辑 src/main/resources/application.yml 第 10 行

# 3. 启动
mvn spring-boot:run
```

访问 http://localhost:8080 即可使用。

### 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | 123456 |
| 学生 | zhangwei2024 | 123456 |

---

## 🧪 测试

```bash
mvn test
# 41 个单元测试，覆盖 TF-IDF、Cosine、Geohash、推荐服务、AI 客户端
```

---

## 📊 数据规模

13 张表完整测试数据：8 用户 / 18 丢失 / 18 拾到 / 12 认领 / 25 评论 / 20 收藏 / 30 消息 / 40 积分日志 / 6 公告 / 30 操作日志 / 25 浏览历史 / 36 位置 / 36 向量元数据。

---

## 📝 文档

- [启动说明](启动说明.md) - 老师运行指南
- [部署指南](docs/部署指南.md) - 生产部署

---

## 📄 License

MIT License - 仅供学习参考

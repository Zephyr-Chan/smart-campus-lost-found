# 校园失物招领 AI 增强服务

> **可选增强服务**：基于 CLIP + BGE 的多模态向量抽取与匹配微服务。
> 主 Java 系统在缺失本服务时可自动降级为 TF-IDF 匹配，**不影响核心业务可用性**。

## 一、定位与降级机制

本服务是校园失物招领系统的「可选」AI 增强模块，独立部署、独立扩展：

- **服务在线**：Java 侧通过 HTTP 调用本服务，获得基于深度语义的多模态匹配（文本+图像+位置融合）。
- **服务离线 / 加载失败**：Java 侧捕获异常后写入降级标记（`ai:degrade:`，TTL 60s），在标记有效期内直接走 TF-IDF 匹配，不再重复请求本服务，避免雪崩。

因此本服务挂掉、模型未下载、Redis Stack 未启用，都不会导致主系统不可用。

## 二、技术栈

| 组件 | 选型 | 说明 |
|------|------|------|
| Web 框架 | FastAPI 0.104 | 高性能异步 API |
| 文本向量 | BAAI/bge-small-zh-v1.5 | 512 维，中文语义匹配 |
| 图像向量 | openai/clip-vit-base-patch32 | 512 维，跨模态 |
| 向量库 | Redis Stack (RediSearch) | HNSW 索引 + KNN 检索 |
| 推理库 | sentence-transformers / transformers / torch | |

## 三、目录结构

```
ai-service/
├── app/
│   ├── __init__.py
│   ├── main.py            # FastAPI 入口，4 个接口
│   ├── config.py          # 配置（Redis / 模型 / HNSW / 融合权重）
│   ├── models.py          # Pydantic 请求/响应模型
│   ├── vector_service.py  # 模型懒加载、向量抽取、Redis 索引与 KNN
│   └── matching.py        # 多模态融合匹配逻辑
├── requirements.txt
├── Dockerfile
└── README.md
```

## 四、安装与运行

### 方式一：本地运行

1. 确保 Redis Stack 已启动（需含 search 模块）：

   ```bash
   docker run -d --name redis-stack -p 6379:6379 redis/redis-stack-server:latest
   ```

2. 安装依赖并启动：

   ```bash
   cd ai-service
   python -m venv venv
   venv\Scripts\activate          # Windows
   # source venv/bin/activate     # Linux/Mac
   pip install -r requirements.txt
   uvicorn app.main:app --host 0.0.0.0 --port 8001
   ```

3. 首次请求会自动下载模型（约 1~2GB），后续从本地缓存加载。

### 方式二：Docker 运行

```bash
cd ai-service
docker build -t campus-ai-service .
docker run -d -p 8001:8001 --env REDIS_HOST=host.docker.internal campus-ai-service
```

启动后访问交互式文档：http://localhost:8001/docs

## 五、API 接口

### 1. 健康检查

```
GET /api/ai/health
```

响应：
```json
{ "status": "up", "models_loaded": false, "redis_connected": true }
```

> 不触发模型加载，仅探测 Redis 连通性。

### 2. 向量抽取

```
POST /api/ai/extract
```

请求：
```json
{ "text": "黑色钱包，有拉链", "image_path": "/uploads/1.jpg" }
```

响应：
```json
{
  "text_vector": [0.0123, ...],
  "image_vector": [0.0456, ...],
  "text_dim": 512,
  "image_dim": 512
}
```

> 模型不可用时对应向量为空、dim=0。

### 3. 索引写入

```
POST /api/ai/index
```

请求：
```json
{ "item_id": 1, "item_type": "found", "text": "黑色钱包", "image_path": "/uploads/1.jpg", "location": "图书馆" }
```

响应：
```json
{ "success": true, "indexed": true }
```

> 按 `item_type` 分别建索引：`item_vector_lost` / `item_vector_found`。

### 4. 多模态匹配

```
POST /api/ai/match
```

请求：
```json
{ "item_id": 10, "item_type": "lost", "text": "黑色钱包", "image_path": "/uploads/10.jpg", "top_k": 10, "location": "图书馆" }
```

响应：
```json
{
  "results": [
    { "item_id": 1, "item_type": "found", "text_score": 0.82, "image_score": 0.75, "final_score": 0.778 }
  ],
  "match_type": "multi_modal"
}
```

`match_type` 取值：
- `multi_modal`：文本+图像双模态融合
- `text_only` / `image_only`：单模态降级
- `fallback`：无可用向量，调用方应降级为 TF-IDF

## 六、融合算法

```
final_score = 0.4 * text_sim + 0.4 * image_sim + 0.2 * location_sim
```

- `text_sim` / `image_sim`：Redis KNN 返回的 COSINE 距离转换为相似度 `1 - D`，裁剪到 `[0,1]`
- `location_sim`：基于 location 字符串相似度（缺失取中性 0.5，相同 1.0，否则按公共前缀占比）
- **降级归一化**：仅单模态可用时，将缺失模态权重按比例并入剩余模态并归一化，保证评分可比
- **跨类型匹配**：失物（lost）在拾物（found）索引中检索，反之亦然

## 七、配置项（环境变量）

| 变量 | 默认值 | 说明 |
|------|--------|------|
| REDIS_HOST | localhost | Redis 地址 |
| REDIS_PORT | 6379 | Redis 端口 |
| REDIS_PASSWORD | | Redis 密码 |
| CLIP_MODEL | openai/clip-vit-base-patch32 | CLIP 模型 |
| TEXT_MODEL | BAAI/bge-small-zh-v1.5 | 文本模型 |
| HNSW_M | 16 | HNSW 邻居数 |
| HNSW_EF_CONSTRUCT | 200 | HNSW 建图队列 |
| MODEL_CACHE_DIR | | 模型缓存目录 |
| TOP_K_DEFAULT | 10 | 默认返回数 |

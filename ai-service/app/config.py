"""
配置模块
================

集中管理 AI 服务的所有可配置项：
1. Redis Stack 连接参数（向量索引与 KNN 检索均依赖 Redis Stack 的 search 模块）
2. 模型名称（CLIP 图像模型 + BGE 中文文本模型）
3. HNSW 索引参数（图近邻搜索，影响召回率与内存占用）
4. 多模态融合权重（与 Java 侧 MatchingProperties 保持一致）

所有参数均支持通过环境变量覆盖，便于容器化部署。
"""

import os

# ======================== Redis Stack 连接配置 ========================
# Redis Stack 必须启用 RediSearch 模块（即 FT.CREATE / FT.SEARCH 可用）
REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
REDIS_DB = int(os.getenv("REDIS_DB", "0"))
REDIS_PASSWORD = os.getenv("REDIS_PASSWORD", "")
# Redis 连接超时（秒），避免服务不可用时长时间阻塞
REDIS_SOCKET_TIMEOUT = float(os.getenv("REDIS_SOCKET_TIMEOUT", "3.0"))


# ======================== 模型配置 ========================
# CLIP 图像模型：输出 512 维图像向量，用于跨模态图像相似度匹配
CLIP_MODEL = os.getenv("CLIP_MODEL", "openai/clip-vit-base-patch32")
# BGE 中文文本模型：输出 512 维文本向量，对中文失物描述语义匹配效果好
TEXT_MODEL = os.getenv("TEXT_MODEL", "BAAI/bge-small-zh-v1.5")

# 向量维度（与上述模型对应，用于建索引时声明 DIM）
CLIP_DIM = 512
TEXT_DIM = 512

# 模型缓存目录（离线/镜像复用场景可指定），None 则使用默认缓存
MODEL_CACHE_DIR = os.getenv("MODEL_CACHE_DIR", None)


# ======================== HNSW 索引参数 ========================
# HNSW（Hierarchical Navigable Small World）是一种近似最近邻图索引算法：
#   - M：每个节点的最大邻居数，越大召回越高、内存越大（典型 16）
#   - EF_CONSTRUCT：建图时候选队列大小，越大建图越慢但质量越高（典型 200）
HNSW_PARAMS = {
    "M": int(os.getenv("HNSW_M", "16")),
    "EF_CONSTRUCT": int(os.getenv("HNSW_EF_CONSTRUCT", "200")),
}

# 索引名前缀，按物品类型分别建索引：item_vector_lost / item_vector_found
# 失物匹配拾物时，在「对方」索引中检索，实现跨类型匹配
INDEX_NAME_PREFIX = os.getenv("INDEX_NAME_PREFIX", "item_vector_")

# Redis 中存储物品向量的 key 前缀（与索引前缀保持一致，便于 IndexDefinition 自动关联）
ITEM_KEY_PREFIX = INDEX_NAME_PREFIX


# ======================== 多模态融合权重 ========================
# 融合评分公式：final_score = w_text * text_sim + w_image * image_sim + w_location * location_sim
# 与 Java 侧 com.campus.lostfound.config.MatchingProperties 保持一致
FUSION_WEIGHTS = {
    "text": 0.4,
    "image": 0.4,
    "location": 0.2,
}

# 匹配结果默认返回数量
TOP_K_DEFAULT = int(os.getenv("TOP_K_DEFAULT", "10"))

# KNN 检索时为融合预留的候选扩大倍数
# 单模态 KNN 只返回部分候选，融合前需扩大召回集，避免漏掉另一模态高分项
KNN_OVER_FETCH_FACTOR = int(os.getenv("KNN_OVER_FETCH_FACTOR", "5"))

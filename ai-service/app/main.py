"""
FastAPI 应用入口
================

提供 4 个接口：

1. GET  /api/ai/health   健康检查（模型加载状态 + Redis 连通性）
2. POST /api/ai/extract  从文本/图片抽取向量
3. POST /api/ai/index    将物品向量写入 Redis Stack 索引
4. POST /api/ai/match    多模态融合匹配（核心）

设计原则：
- 无状态：所有向量数据持久化在 Redis Stack，服务本身不持有业务数据
- 懒加载：模型仅在首次 extract/index/match 时加载
- 优雅降级：模型/Redis 不可用时返回明确错误，不抛 500
"""

import logging

from fastapi import FastAPI

from . import config, matching
from .models import (
    ExtractRequest,
    ExtractResponse,
    HealthResponse,
    IndexRequest,
    IndexResponse,
    MatchRequest,
    MatchResponse,
    MatchResultItem,
)
from .vector_service import vector_service

# 日志配置
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("ai-service.main")

app = FastAPI(
    title="校园失物招领 AI 增强服务",
    description="基于 CLIP + BGE 的多模态向量抽取与匹配微服务（可选增强）",
    version="1.0.0",
)


# ===================================================================
#  1. 健康检查
# ===================================================================
@app.get("/api/ai/health", response_model=HealthResponse, tags=["AI"])
def health() -> HealthResponse:
    """
    健康检查：不触发模型加载，仅探测 Redis 连通性。
    models_loaded 反映当前模型是否已就绪。
    """
    redis_ok = vector_service.check_redis()
    return HealthResponse(
        status="up",
        models_loaded=vector_service.models_loaded,
        redis_connected=redis_ok,
    )


# ===================================================================
#  2. 向量抽取
# ===================================================================
@app.post("/api/ai/extract", response_model=ExtractResponse, tags=["AI"])
def extract(req: ExtractRequest) -> ExtractResponse:
    """
    从文本和/或图片中抽取向量。
    - 模型未就绪/加载失败：返回空向量（dim=0）
    - text 与 image_path 均为空：直接返回空
    """
    if not req.text and not req.image_path:
        return ExtractResponse()

    text_vec, image_vec = vector_service.extract(req.text, req.image_path)
    return ExtractResponse(
        text_vector=text_vec,
        image_vector=image_vec,
        text_dim=len(text_vec),
        image_dim=len(image_vec),
    )


# ===================================================================
#  3. 索引写入
# ===================================================================
@app.post("/api/ai/index", response_model=IndexResponse, tags=["AI"])
def index(req: IndexRequest) -> IndexResponse:
    """
    抽取物品向量并写入 Redis Stack 索引。
    - 模型不可用 / Redis 不可用 / 未抽到任何向量 → indexed=False
    - 但 success=True 表示请求已被正常处理（区别于服务异常）
    """
    if not req.text and not req.image_path:
        # 无文本无图片，无法抽取
        return IndexResponse(success=True, indexed=False)

    ok = vector_service.index_item(
        item_id=req.item_id,
        item_type=req.item_type,
        text=req.text,
        image_path=req.image_path,
        location=req.location,
    )
    return IndexResponse(success=True, indexed=ok)


# ===================================================================
#  4. 多模态匹配
# ===================================================================
@app.post("/api/ai/match", response_model=MatchResponse, tags=["AI"])
def match(req: MatchRequest) -> MatchResponse:
    """
    多模态融合匹配核心逻辑：

    1) 抽取查询向量（文本/图像）；若二者均缺，则回退取回该物品已索引向量
    2) 确定候选物品类型 = 查询类型的对侧（lost↔found 跨类型匹配）
    3) 分别做文本 KNN 与图像 KNN（仅对可用模态）
    4) 融合 final_score = 0.4*text + 0.4*image + 0.2*location
    5) 降序取 top_k 返回

    模型不可用 / Redis 不可用 → 返回空结果，match_type=fallback，
    调用方（Java 侧）据此降级为 TF-IDF。
    """
    # ---- ① 获取查询向量 ----
    text_vec: list = []
    image_vec: list = []
    query_location = req.location

    if req.text:
        text_vec = vector_service.extract_text_vector(req.text)
    if req.image_path:
        image_vec = vector_service.extract_image_vector(req.image_path)

    # 文本/图像均未直接提供或抽取失败 → 回退取回已索引向量
    if not text_vec and not image_vec:
        t, i, loc = vector_service.get_stored_vectors(req.item_id, req.item_type)
        text_vec, image_vec = t, i
        if not query_location and loc:
            query_location = loc

    has_text = bool(text_vec)
    has_image = bool(image_vec)

    # ---- ② 模型或向量均不可用 → 降级 ----
    if not has_text and not has_image:
        logger.warning("匹配降级：无可用向量 item=%s:%s", req.item_type, req.item_id)
        return MatchResponse(results=[], match_type="fallback")

    # ---- ③ 候选类型取对侧：失物找拾物，拾物找失物 ----
    candidate_type = "found" if req.item_type == "lost" else "lost"

    # ---- ④ KNN 检索（扩大召回以利于融合）----
    # 单模态 KNN 只覆盖部分候选，融合前需扩大候选集
    fetch_k = max(req.top_k * config.KNN_OVER_FETCH_FACTOR, req.top_k)

    text_hits: list = []
    image_hits: list = []
    if has_text:
        text_hits = vector_service.knn_search(
            candidate_type, "text_vector", text_vec, fetch_k
        )
    if has_image:
        image_hits = vector_service.knn_search(
            candidate_type, "image_vector", image_vec, fetch_k
        )

    # ---- ⑤ 融合并排序 ----
    ranked = matching.fuse_and_rank(
        text_hits=text_hits,
        image_hits=image_hits,
        query_location=query_location,
        has_text=has_text,
        has_image=has_image,
        top_k=req.top_k,
    )

    match_type = matching.determine_match_type(has_text, has_image)
    results = [MatchResultItem(**item) for item in ranked]

    logger.info(
        "匹配完成 item=%s:%s -> 候选=%s, type=%s, 返回=%d 条",
        req.item_type, req.item_id, candidate_type, match_type, len(results),
    )
    return MatchResponse(results=results, match_type=match_type)


# ===================================================================
#  启动入口（便于直接 python -m 或 uvicorn 运行）
# ===================================================================
if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8001,
        reload=False,
    )

"""
多模态融合匹配模块
==================

融合策略：
    final_score = w_text * text_sim + w_image * image_sim + w_location * location_sim

其中：
- text_sim   / image_sim：来自 Redis KNN 的余弦相似度（已在 vector_service 中由距离转换）
- location_sim：基于 location 字符串的相似度（缺失时取中性值 0.5）

降级处理：
- 仅文本可用：将 image 权重按比例并入 text/location，归一化后融合
- 仅图像可用：同理
- 文本/图像均不可用：仅依赖 location_sim

匹配流程（由 match 接口编排，本模块只负责「融合 + 排序」）：
1. 用查询向量分别在候选索引上做文本 KNN 与图像 KNN
2. 按 item_id 合并两个候选集
3. 逐项融合得到 final_score
4. 按 final_score 降序取 top_k
"""

import logging
from typing import Dict, List, Optional

import numpy as np

from . import config

logger = logging.getLogger("ai-service.matching")

# 融合权重（与 config.FUSION_WEIGHTS 一致，单独取出便于阅读）
W_TEXT = config.FUSION_WEIGHTS["text"]
W_IMAGE = config.FUSION_WEIGHTS["image"]
W_LOCATION = config.FUSION_WEIGHTS["location"]


# ===================================================================
#  相似度计算工具
# ===================================================================
def cosine_similarity(vec_a: List[float], vec_b: List[float]) -> float:
    """
    计算两个向量的余弦相似度，范围 [-1, 1]。
    任一为空或维度不一致返回 0。
    """
    if not vec_a or not vec_b:
        return 0.0
    a = np.asarray(vec_a, dtype=np.float32)
    b = np.asarray(vec_b, dtype=np.float32)
    if a.shape != b.shape:
        return 0.0
    na = float(np.linalg.norm(a))
    nb = float(np.linalg.norm(b))
    if na == 0.0 or nb == 0.0:
        return 0.0
    return float(np.dot(a, b) / (na * nb))


def location_similarity(loc_a: Optional[str], loc_b: Optional[str]) -> float:
    """
    计算地点相似度，范围 [0, 1]。

    策略（可扩展为 GeoHash 前缀匹配）：
    - 任一缺失 → 0.5（中性，不偏置排名）
    - 完全相同 → 1.0
    - 否则按公共前缀长度占比估算（适合 GeoHash / 地名分级）
    """
    if not loc_a or not loc_b:
        return 0.5
    if loc_a == loc_b:
        return 1.0
    # 公共前缀长度
    common = 0
    for ca, cb in zip(loc_a, loc_b):
        if ca == cb:
            common += 1
        else:
            break
    max_len = max(len(loc_a), len(loc_b)) or 1
    return common / max_len


# ===================================================================
#  候选合并
# ===================================================================
def _merge_candidates(
    text_hits: List[Dict], image_hits: List[Dict]
) -> Dict[int, Dict]:
    """
    按 item_id 合并文本 KNN 与图像 KNN 的结果。

    返回：{item_id: {"item_type":..., "text_score":float|None,
                     "image_score":float|None, "location":str|None}}
    """
    merged: Dict[int, Dict] = {}
    for hit in text_hits:
        item_id = hit["item_id"]
        merged[item_id] = {
            "item_id": item_id,
            "item_type": hit.get("item_type"),
            "text_score": hit.get("score"),
            "image_score": None,
            "location": hit.get("location"),
        }
    for hit in image_hits:
        item_id = hit["item_id"]
        if item_id not in merged:
            merged[item_id] = {
                "item_id": item_id,
                "item_type": hit.get("item_type"),
                "text_score": None,
                "image_score": hit.get("score"),
                "location": hit.get("location"),
            }
        else:
            # 已在文本结果中存在，补齐图像分
            merged[item_id]["image_score"] = hit.get("score")
            # 图像命中可能携带 location，补齐缺失情况
            if not merged[item_id].get("location"):
                merged[item_id]["location"] = hit.get("location")
    return merged


# ===================================================================
#  融合打分
# ===================================================================
def _fuse(
    text_sim: Optional[float],
    image_sim: Optional[float],
    loc_sim: float,
    has_text: bool,
    has_image: bool,
) -> float:
    """
    根据查询侧可用模态，自适应融合打分。

    - 文本+图像均在：使用完整权重（三者权重和=1.0）
    - 仅文本：把 image 权重并入剩余模态并归一化
    - 仅图像：同理
    - 都没有：退化为 location_sim
    """
    t = text_sim if (has_text and text_sim is not None) else None
    i = image_sim if (has_image and image_sim is not None) else None

    if t is not None and i is not None:
        return W_TEXT * t + W_IMAGE * i + W_LOCATION * loc_sim

    if t is not None:
        # 仅文本：权重归一化到 (W_TEXT + W_LOCATION)
        w_sum = W_TEXT + W_LOCATION
        return (W_TEXT * t + W_LOCATION * loc_sim) / w_sum if w_sum > 0 else t

    if i is not None:
        w_sum = W_IMAGE + W_LOCATION
        return (W_IMAGE * i + W_LOCATION * loc_sim) / w_sum if w_sum > 0 else i

    # 文本/图像均无 → 仅靠 location
    return loc_sim


# ===================================================================
#  对外入口：融合并排序
# ===================================================================
def fuse_and_rank(
    text_hits: List[Dict],
    image_hits: List[Dict],
    query_location: Optional[str],
    has_text: bool,
    has_image: bool,
    top_k: int,
) -> List[Dict]:
    """
    合并 KNN 结果 → 融合打分 → 按 final_score 降序取 top_k。

    :return: [{"item_id", "item_type", "text_score", "image_score", "final_score"}]
    """
    merged = _merge_candidates(text_hits, image_hits)
    results: List[Dict] = []
    for cand in merged.values():
        text_sim = cand.get("text_score")
        image_sim = cand.get("image_score")
        loc_sim = location_similarity(query_location, cand.get("location"))
        final = _fuse(text_sim, image_sim, loc_sim, has_text, has_image)
        results.append({
            "item_id": cand["item_id"],
            "item_type": cand.get("item_type") or "",
            # 对外字段：缺失模态记 0，便于前端展示
            "text_score": round(float(text_sim), 6) if text_sim is not None else 0.0,
            "image_score": round(float(image_sim), 6) if image_sim is not None else 0.0,
            "final_score": round(float(final), 6),
        })

    # 按最终评分降序
    results.sort(key=lambda x: x["final_score"], reverse=True)
    return results[:top_k]


def determine_match_type(has_text: bool, has_image: bool) -> str:
    """根据查询侧可用模态判定匹配类型标识"""
    if has_text and has_image:
        return "multi_modal"
    if has_image:
        return "image_only"
    if has_text:
        return "text_only"
    return "fallback"

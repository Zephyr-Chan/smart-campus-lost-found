"""
Pydantic 数据模型
=================

定义四个对外接口的请求 / 响应结构，统一 JSON 契约。

字段说明：
- text / image_path：多模态输入，至少提供一个即可
- item_type：取值 "lost"（丢失）或 "found"（拾到），与 Java 侧常量一致
- location（可选扩展）：地点字符串或 GeoHash，用于计算 location_sim；
  未提供时使用中性默认值，不影响主流程
"""

from typing import List, Optional

from pydantic import BaseModel, Field


# ======================== 健康检查 ========================
class HealthResponse(BaseModel):
    """健康检查响应"""
    status: str = "up"
    models_loaded: bool = False
    redis_connected: bool = False


# ======================== 向量抽取 ========================
class ExtractRequest(BaseModel):
    """向量抽取请求：从文本和/或图片中提取向量"""
    text: Optional[str] = Field(None, description="待提取向量的文本描述")
    image_path: Optional[str] = Field(None, description="图片在服务器上的绝对路径")

    class Config:
        json_schema_extra = {
            "example": {"text": "黑色钱包，有拉链", "image_path": "/uploads/1.jpg"}
        }


class ExtractResponse(BaseModel):
    """向量抽取响应"""
    text_vector: List[float] = Field(default_factory=list, description="文本向量")
    image_vector: List[float] = Field(default_factory=list, description="图像向量")
    text_dim: int = Field(0, description="文本向量维度（0 表示未抽取）")
    image_dim: int = Field(0, description="图像向量维度（0 表示未抽取）")


# ======================== 索引写入 ========================
class IndexRequest(BaseModel):
    """索引写入请求：将物品向量写入 Redis Stack"""
    item_id: int = Field(..., description="物品 ID")
    item_type: str = Field(..., description="物品类型：lost / found")
    text: Optional[str] = Field(None, description="物品文本描述")
    image_path: Optional[str] = Field(None, description="物品图片路径")
    # 可选扩展字段：携带后即可启用真实的 location_sim 计算
    location: Optional[str] = Field(None, description="物品地点（地名或 GeoHash）")


class IndexResponse(BaseModel):
    """索引写入响应"""
    success: bool = Field(..., description="请求是否成功处理（即使未写入也返回 true）")
    indexed: bool = Field(..., description="是否真正写入向量索引")


# ======================== 多模态匹配 ========================
class MatchRequest(BaseModel):
    """多模态匹配请求"""
    item_id: int = Field(..., description="发起匹配的物品 ID（用于日志/取回向量）")
    item_type: str = Field(..., description="发起物品类型：lost / found")
    text: Optional[str] = Field(None, description="查询文本")
    image_path: Optional[str] = Field(None, description="查询图片路径")
    top_k: int = Field(10, ge=1, le=100, description="返回结果数量")
    # 可选扩展字段
    location: Optional[str] = Field(None, description="查询地点（地名或 GeoHash）")


class MatchResultItem(BaseModel):
    """单条匹配结果"""
    item_id: int
    item_type: str
    text_score: float = Field(..., description="文本相似度 [0,1]，无则 0")
    image_score: float = Field(..., description="图像相似度 [0,1]，无则 0")
    final_score: float = Field(..., description="融合后最终评分")


class MatchResponse(BaseModel):
    """多模态匹配响应"""
    results: List[MatchResultItem] = Field(default_factory=list)
    match_type: str = Field(
        ..., description="匹配类型：multi_modal / text_only / image_only / fallback"
    )

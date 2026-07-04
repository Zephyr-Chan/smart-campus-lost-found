"""
向量服务模块
============

职责：
1. 懒加载（lazy-load）CLIP 图像模型与 BGE 文本模型，避免冷启动慢
2. 从文本 / 图片抽取归一化向量
3. 在 Redis Stack 中按物品类型创建 HNSW 向量索引
4. 写入物品向量、并支持 KNN 近邻检索

设计要点：
- 所有模型加载均在「首次请求」时触发，启动瞬间即可对外提供 health 接口
- 模型加载失败时不会抛出异常中断服务，而是置位错误标记，后续请求返回空向量
- Redis 不可用 / search 模块缺失时同样优雅降级
- 向量以 float32 字节流存入 Redis Hash，配合 HNSW 索引实现毫秒级 KNN
"""

import logging
from typing import Dict, List, Optional, Tuple

import numpy as np

from . import config

logger = logging.getLogger("ai-service.vector_service")


class VectorService:
    """向量抽取与 Redis Stack 索引服务（单例，进程内共享）"""

    def __init__(self) -> None:
        # ---- 模型句柄（懒加载，初始为 None）----
        self._text_model = None        # sentence-transformers 模型
        self._clip_model = None        # transformers CLIPModel
        self._clip_processor = None    # transformers CLIPProcessor

        # ---- 模型状态 ----
        self._models_loaded = False
        self._load_error: Optional[str] = None
        self._loading = False          # 防止并发重复加载

        # ---- Redis 客户端 ----
        self._redis = None

        # ---- 已确认创建的索引集合，避免重复 FT.CREATE ----
        self._ready_indexes: set = set()

    # ===================================================================
    #  对外状态查询
    # ===================================================================
    @property
    def models_loaded(self) -> bool:
        """模型是否已成功加载"""
        return self._models_loaded

    @property
    def load_error(self) -> Optional[str]:
        """模型加载失败时的错误信息"""
        return self._load_error

    # ===================================================================
    #  模型懒加载
    # ===================================================================
    def _ensure_models(self) -> bool:
        """
        首次调用时加载模型；后续调用直接返回状态。

        加载失败会被捕获并记录，不影响服务存活。
        """
        if self._models_loaded:
            return True
        if self._loading:
            # 正在加载中（其它线程），本轮直接放弃
            return False
        self._loading = True
        try:
            # 延迟导入重型依赖，仅在真正需要时加载到内存
            import torch  # noqa: F401  确保torch可用
            from sentence_transformers import SentenceTransformer
            from transformers import CLIPModel, CLIPProcessor

            logger.info("开始加载模型：text=%s, clip=%s", config.TEXT_MODEL, config.CLIP_MODEL)
            self._text_model = SentenceTransformer(
                config.TEXT_MODEL, cache_folder=config.MODEL_CACHE_DIR
            )
            self._clip_processor = CLIPProcessor.from_pretrained(
                config.CLIP_MODEL, cache_dir=config.MODEL_CACHE_DIR
            )
            self._clip_model = CLIPModel.from_pretrained(
                config.CLIP_MODEL, cache_dir=config.MODEL_CACHE_DIR
            )
            # 推理模式，关闭梯度计算以节省显存
            self._clip_model.eval()
            self._models_loaded = True
            self._load_error = None
            logger.info("模型加载完成")
        except Exception as e:  # noqa: BLE001  捕获所有加载异常以保障降级
            self._models_loaded = False
            self._load_error = str(e)
            logger.exception("模型加载失败，服务将以降级模式运行: %s", e)
        finally:
            self._loading = False
        return self._models_loaded

    # ===================================================================
    #  向量抽取
    # ===================================================================
    def extract_text_vector(self, text: str) -> List[float]:
        """
        使用 BGE 模型抽取文本向量并归一化。
        模型不可用时返回空列表。
        """
        if not text or not text.strip():
            return []
        if not self._ensure_models():
            return []
        try:
            # normalize_embeddings=True 后，余弦相似度 = 点积，便于检索
            vec = self._text_model.encode(
                text, normalize_embeddings=True, convert_to_numpy=True
            )
            return np.asarray(vec, dtype=np.float32).tolist()
        except Exception as e:  # noqa: BLE001
            logger.exception("文本向量抽取失败: %s", e)
            return []

    def extract_image_vector(self, image_path: str) -> List[float]:
        """
        使用 CLIP 模型抽取图像向量并归一化。
        模型不可用 / 图片读取失败时返回空列表。
        """
        if not image_path:
            return []
        if not self._ensure_models():
            return []
        try:
            from PIL import Image
            import torch

            # 转为 RGB，避免 RGBA / 灰度图导致 CLIP 输入维度不一致
            image = Image.open(image_path).convert("RGB")
            inputs = self._clip_processor(images=image, return_tensors="pt")
            with torch.no_grad():
                # get_image_features 返回 (1, 512) 的图像嵌入
                features = self._clip_model.get_image_features(**inputs)
            vec = features[0].cpu().numpy().astype(np.float32)
            # L2 归一化，使余弦相似度等价于内积
            norm = np.linalg.norm(vec)
            if norm > 0:
                vec = vec / norm
            return vec.tolist()
        except Exception as e:  # noqa: BLE001
            logger.exception("图像向量抽取失败 path=%s: %s", image_path, e)
            return []

    def extract(
        self, text: Optional[str] = None, image_path: Optional[str] = None
    ) -> Tuple[List[float], List[float]]:
        """同时抽取文本与图像向量（任一可缺省）"""
        text_vec = self.extract_text_vector(text) if text else []
        image_vec = self.extract_image_vector(image_path) if image_path else []
        return text_vec, image_vec

    # ===================================================================
    #  Redis 连接
    # ===================================================================
    def get_redis(self):
        """获取（惰性创建）Redis 客户端"""
        if self._redis is None:
            try:
                import redis

                self._redis = redis.Redis(
                    host=config.REDIS_HOST,
                    port=config.REDIS_PORT,
                    db=config.REDIS_DB,
                    password=config.REDIS_PASSWORD or None,
                    socket_timeout=config.REDIS_SOCKET_TIMEOUT,
                    socket_connect_timeout=config.REDIS_SOCKET_TIMEOUT,
                    decode_responses=False,  # 向量字节流需保持 bytes
                )
            except Exception as e:  # noqa: BLE001
                logger.exception("Redis 客户端创建失败: %s", e)
                self._redis = None
        return self._redis

    def check_redis(self) -> bool:
        """检测 Redis 是否可连通（供 health 接口使用）"""
        try:
            r = self.get_redis()
            if r is None:
                return False
            return r.ping()
        except Exception:  # noqa: BLE001
            return False

    # ===================================================================
    #  索引管理
    # ===================================================================
    def _index_name(self, item_type: str) -> str:
        """根据物品类型生成索引名，如 item_vector_lost"""
        return f"{config.INDEX_NAME_PREFIX}{item_type}"

    def _item_key(self, item_type: str, item_id: int) -> str:
        """物品向量在 Redis 中的 key，如 item_vector_lost:123"""
        return f"{config.ITEM_KEY_PREFIX}{item_type}:{item_id}"

    def _ensure_index(self, item_type: str) -> bool:
        """
        确保指定物品类型的 HNSW 索引存在，不存在则创建。
        索引包含 text_vector 与 image_vector 两个向量字段，
        缺少图片的物品不写入 image_vector，KNN 时自动跳过。
        """
        if item_type in self._ready_indexes:
            return True
        r = self.get_redis()
        if r is None:
            return False
        index_name = self._index_name(item_type)
        try:
            # 延迟导入 search 模块相关类
            from redis.commands.search.field import (
                NumericField,
                TagField,
                TextField,
                VectorField,
            )
            from redis.commands.search.indexDefinition import (
                IndexDefinition,
                IndexType,
            )

            # IndexDefinition 通过 prefix 自动把该前缀的 Hash 关联到此索引
            prefix = f"{config.ITEM_KEY_PREFIX}{item_type}:"
            definition = IndexDefinition(prefix=[prefix], index_type=IndexType.HASH)

            # HNSW 向量字段：FLOAT32、COSINE 距离
            hnsw = {
                "TYPE": "FLOAT32",
                "DIM": config.CLIP_DIM,  # 文本与图像均为 512 维
                "DISTANCE_METRIC": "COSINE",
                "M": config.HNSW_PARAMS["M"],
                "EF_CONSTRUCTION": config.HNSW_PARAMS["EF_CONSTRUCT"],
            }
            schema = (
                NumericField("item_id", sortable=True),
                TagField("item_type"),
                TextField("location"),
                VectorField("text_vector", "HNSW", hnsw),
                VectorField("image_vector", "HNSW", dict(hnsw)),
            )
            r.ft(index_name).create_index(schema, definition=definition)
            self._ready_indexes.add(item_type)
            logger.info("已创建 HNSW 索引: %s", index_name)
            return True
        except Exception as e:  # noqa: BLE001
            # 索引已存在会抛异常，这里判定后加入 ready 集合
            msg = str(e)
            if "Index already exists" in msg or "already exists" in msg:
                self._ready_indexes.add(item_type)
                logger.info("索引已存在，复用: %s", index_name)
                return True
            logger.warning("创建索引 %s 失败（Redis Stack search 模块可能未启用）: %s",
                           index_name, msg)
            return False

    # ===================================================================
    #  写入与读取
    # ===================================================================
    def index_item(
        self,
        item_id: int,
        item_type: str,
        text: Optional[str] = None,
        image_path: Optional[str] = None,
        location: Optional[str] = None,
    ) -> bool:
        """
        抽取向量并写入 Redis Stack。
        至少要能抽到一种向量才算写入成功；模型不可用则返回 False。
        """
        if not self._ensure_models():
            return False
        if not self._ensure_index(item_type):
            return False

        text_vec = self.extract_text_vector(text) if text else []
        image_vec = self.extract_image_vector(image_path) if image_path else []

        # 文本与图像均缺失 → 无法建索引
        if not text_vec and not image_vec:
            logger.warning("物品 %s:%s 未提取到任何向量，跳过索引", item_type, item_id)
            return False

        r = self.get_redis()
        if r is None:
            return False

        try:
            mapping: Dict = {
                "item_id": item_id,
                "item_type": item_type,
            }
            if location:
                mapping["location"] = location
            if text_vec:
                mapping["text_vector"] = np.asarray(text_vec, dtype=np.float32).tobytes()
            # 注意：image_vec 缺失时不写入该字段，KNN 检索将自动忽略此文档
            if image_vec:
                mapping["image_vector"] = np.asarray(image_vec, dtype=np.float32).tobytes()

            key = self._item_key(item_type, item_id)
            r.hset(key, mapping=mapping)
            logger.info("已索引物品 %s:%s (text=%s, image=%s)",
                        item_type, item_id, bool(text_vec), bool(image_vec))
            return True
        except Exception as e:  # noqa: BLE001
            logger.exception("写入索引失败 %s:%s: %s", item_type, item_id, e)
            return False

    def get_stored_vectors(
        self, item_id: int, item_type: str
    ) -> Tuple[List[float], List[float], Optional[str]]:
        """
        从 Redis 读取已索引物品的向量与 location。
        用于 match 接口在未提供 text/image 时的回退取回。
        """
        r = self.get_redis()
        if r is None:
            return [], [], None
        try:
            key = self._item_key(item_type, item_id)
            data = r.hgetall(key)
            if not data:
                return [], [], None

            def _to_list(raw: Optional[bytes]) -> List[float]:
                if not raw:
                    return []
                return np.frombuffer(raw, dtype=np.float32).tolist()

            text_vec = _to_list(data.get(b"text_vector"))
            image_vec = _to_list(data.get(b"image_vector"))
            location = data.get(b"location")
            location = location.decode("utf-8") if isinstance(location, bytes) else None
            return text_vec, image_vec, location
        except Exception as e:  # noqa: BLE001
            logger.exception("读取已存向量失败 %s:%s: %s", item_type, item_id, e)
            return [], [], None

    # ===================================================================
    #  KNN 近邻检索
    # ===================================================================
    def knn_search(
        self,
        item_type: str,
        field: str,
        query_vector: List[float],
        k: int,
    ) -> List[Dict]:
        """
        在指定物品类型索引上，对指定向量字段做 KNN 检索。

        :param item_type: 候选物品类型（通常是查询物品的「对侧」类型）
        :param field: "text_vector" 或 "image_vector"
        :param query_vector: 查询向量
        :param k: 返回数量
        :return: [{"item_id", "item_type", "score", "location"}, ...]
                 score 已从 Redis 距离转换为 [0,1] 相似度
        """
        if not query_vector:
            return []
        if not self._ensure_index(item_type):
            return []
        r = self.get_redis()
        if r is None:
            return []
        try:
            from redis.commands.search.query import Query

            index_name = self._index_name(item_type)
            # KNN 查询语法：*=>[KNN {k} @{field} $vec AS score]
            # Redis 返回的 score 为 COSINE 距离 D ∈ [0,2]，相似度 = 1 - D
            q = (
                Query(f"*=>[KNN {k} @{field} $vec AS score]")
                .add_param("vec", np.asarray(query_vector, dtype=np.float32).tobytes())
                .return_fields("item_id", "item_type", "location", "score")
                .paging(0, k)
            )
            res = r.ft(index_name).search(q)
            results: List[Dict] = []
            for doc in res.docs:
                try:
                    item_id = int(getattr(doc, "item_id", 0))
                except (TypeError, ValueError):
                    continue
                if item_id <= 0:
                    continue
                # 距离 → 相似度，并裁剪到 [0,1]
                try:
                    distance = float(getattr(doc, "score", 1.0))
                except (TypeError, ValueError):
                    distance = 1.0
                similarity = max(0.0, min(1.0, 1.0 - distance))
                item_type_val = getattr(doc, "item_type", item_type)
                if isinstance(item_type_val, bytes):
                    item_type_val = item_type_val.decode("utf-8")
                loc = getattr(doc, "location", None)
                if isinstance(loc, bytes):
                    loc = loc.decode("utf-8")
                results.append({
                    "item_id": item_id,
                    "item_type": item_type_val or item_type,
                    "score": similarity,
                    "location": loc or None,
                })
            return results
        except Exception as e:  # noqa: BLE001
            logger.exception("KNN 检索失败 %s.%s: %s", item_type, field, e)
            return []


# 进程级单例，供 main.py 直接导入使用
vector_service = VectorService()

package com.campus.lostfound.common.constant;

/**
 * 系统常量
 */
public class Constants {

    private Constants() {
    }

    /** 匹配结果缓存前缀 */
    public static final String MATCH_PREFIX = "match:lost:";

    /** 匹配结果缓存TTL（分钟） */
    public static final long MATCH_TTL = 30;

    /** Token缓存前缀 */
    public static final String TOKEN_PREFIX = "user:token:";

    /** 管理员角色 */
    public static final int ROLE_ADMIN = 1;

    /** 物品状态：待处理 */
    public static final int STATUS_PENDING = 0;

    /** 物品状态：已匹配 */
    public static final int STATUS_MATCHED = 1;

    /** 物品状态：已关闭 */
    public static final int STATUS_CLOSED = 2;

    /** 认领状态：待审核 */
    public static final int CLAIM_PENDING = 0;

    /** 认领状态：已通过 */
    public static final int CLAIM_APPROVED = 1;

    /** 认领状态：已拒绝 */
    public static final int CLAIM_REJECTED = 2;

    /** 认领状态：已完成 */
    public static final int CLAIM_COMPLETED = 3;

    // ===== V2 升级常量 =====

    /** 物品类型：丢失 */
    public static final String ITEM_TYPE_LOST = "lost";

    /** 物品类型：拾到 */
    public static final String ITEM_TYPE_FOUND = "found";

    /** 消息类型：匹配 */
    public static final String MSG_TYPE_MATCH = "match";

    /** 消息类型：认领 */
    public static final String MSG_TYPE_CLAIM = "claim";

    /** 消息类型：积分 */
    public static final String MSG_TYPE_CREDIT = "credit";

    /** 消息类型：系统 */
    public static final String MSG_TYPE_SYSTEM = "system";

    /** 评论状态：正常 */
    public static final int COMMENT_NORMAL = 1;

    /** 评论状态：删除 */
    public static final int COMMENT_DELETED = 0;

    /** 信用积分变动原因：发布拾到 */
    public static final String CREDIT_PUBLISH_FOUND = "publish_found";

    /** 信用积分变动原因：认领成功 */
    public static final String CREDIT_CLAIM_SUCCESS = "claim_success";

    /** 信用积分变动原因：恶意行为 */
    public static final String CREDIT_MALICIOUS = "malicious";

    /** 推荐结果缓存前缀 */
    public static final String RECOMMEND_PREFIX = "recommend:user:";

    /** 推荐结果缓存TTL（分钟） */
    public static final long RECOMMEND_TTL = 30;

    /** 排行榜缓存前缀 */
    public static final String RANK_PREFIX = "rank:";

    /** 排行榜缓存TTL（分钟） */
    public static final long RANK_TTL = 10;

    /** 物品相似度矩阵缓存前缀 */
    public static final String SIMILARITY_PREFIX = "similarity:";

    /** 未读消息计数缓存前缀 */
    public static final String UNREAD_PREFIX = "message:unread:";

    /** GeoHash 默认精度（7位约 153m x 153m） */
    public static final int GEOHASH_PRECISION = 7;

    /** 向量索引名前缀 */
    public static final String VECTOR_INDEX_PREFIX = "item_vector_";

    /** AI 服务降级标记缓存前缀 */
    public static final String AI_DEGRADE_PREFIX = "ai:degrade:";

    /** AI 服务降级检查间隔（秒） */
    public static final long AI_DEGRADE_TTL = 60;
}

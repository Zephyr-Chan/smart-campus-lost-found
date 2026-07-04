package com.campus.lostfound.service;

/**
 * 物品向量同步服务接口
 * 负责将物品信息同步到 AI 服务的向量索引，并维护本地 item_vector 表的状态记录
 */
public interface VectorSyncService {

    /**
     * 同步物品向量（文本 + 图像）到 AI 服务并更新本地向量状态
     *
     * @param itemId    物品ID
     * @param itemType  物品类型（lost/found）
     * @param text      描述文本
     * @param imagePath 图片路径（可为 null）
     */
    void syncItemVector(Long itemId, String itemType, String text, String imagePath);

    /**
     * 从 AI 服务移除物品向量索引，并删除本地向量记录
     *
     * @param itemId   物品ID
     * @param itemType 物品类型（lost/found）
     */
    void removeItemVector(Long itemId, String itemType);
}

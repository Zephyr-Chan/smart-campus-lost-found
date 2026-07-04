package com.campus.lostfound.service;

import com.campus.lostfound.common.result.Result;

import java.util.List;
import java.util.Map;

/**
 * 推荐服务接口
 * 基于 Item-CF 协同过滤算法实现个性化推荐
 */
public interface RecommendService {

    /**
     * 为登录用户生成个性化推荐
     */
    Result<List<Map<String, Object>>> recommendForUser(Long userId, int limit);

    /**
     * 为游客生成热门推荐（冷启动）
     */
    Result<List<Map<String, Object>>> recommendForGuest(int limit);

    /**
     * 获取相似物品推荐
     */
    Result<List<Map<String, Object>>> similarItems(Long itemId, String itemType, int limit);

    /**
     * 记录用户浏览历史（用于协同过滤）
     */
    void recordViewHistory(Long userId, Long itemId, String itemType);
}

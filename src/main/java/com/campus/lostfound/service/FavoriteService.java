package com.campus.lostfound.service;

import com.campus.lostfound.common.result.Result;

import java.util.List;
import java.util.Map;

/**
 * 收藏服务接口
 */
public interface FavoriteService {

    /**
     * 切换收藏状态（已收藏则取消，未收藏则添加）
     *
     * @param userId   用户ID
     * @param itemId   物品ID
     * @param itemType 物品类型（lost/found）
     * @return true-已收藏，false-已取消收藏
     */
    Result<Boolean> toggleFavorite(Long userId, Long itemId, String itemType);

    /**
     * 检查当前用户是否已收藏某物品
     *
     * @param userId   用户ID
     * @param itemId   物品ID
     * @param itemType 物品类型（lost/found）
     * @return true-已收藏
     */
    Result<Boolean> checkFavorite(Long userId, Long itemId, String itemType);

    /**
     * 分页查询我的收藏列表（关联物品信息）
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页数量
     * @return 收藏记录列表（每条包含物品标题、分类、图片、状态等）
     */
    Result<List<Map<String, Object>>> myFavorites(Long userId, int page, int size);
}

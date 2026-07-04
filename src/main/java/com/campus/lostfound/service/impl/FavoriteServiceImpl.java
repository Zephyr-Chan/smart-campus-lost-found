package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.result.ResultCode;
import com.campus.lostfound.entity.Favorite;
import com.campus.lostfound.entity.FoundItem;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.mapper.FavoriteMapper;
import com.campus.lostfound.mapper.FoundItemMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.service.FavoriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收藏服务实现类
 */
@Slf4j
@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private LostItemMapper lostItemMapper;

    @Autowired
    private FoundItemMapper foundItemMapper;

    @Override
    public Result<Boolean> toggleFavorite(Long userId, Long itemId, String itemType) {
        if (userId == null || itemId == null || !StringUtils.hasText(itemType)) {
            return Result.fail(ResultCode.PARAM_ERROR);
        }

        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("item_id", itemId).eq("item_type", itemType);
        Favorite existing = favoriteMapper.selectOne(wrapper);

        if (existing != null) {
            // 已收藏 -> 取消收藏
            favoriteMapper.deleteById(existing.getId());
            log.info("用户{}取消收藏 itemId={}, itemType={}", userId, itemId, itemType);
            return Result.success(false);
        }

        // 未收藏 -> 添加收藏
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setItemId(itemId);
        favorite.setItemType(itemType);
        favoriteMapper.insert(favorite);
        log.info("用户{}添加收藏 itemId={}, itemType={}", userId, itemId, itemType);
        return Result.success(true);
    }

    @Override
    public Result<Boolean> checkFavorite(Long userId, Long itemId, String itemType) {
        if (userId == null || itemId == null || !StringUtils.hasText(itemType)) {
            return Result.success(false);
        }
        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("item_id", itemId).eq("item_type", itemType);
        Long count = favoriteMapper.selectCount(wrapper);
        return Result.success(count != null && count > 0);
    }

    @Override
    public Result<List<Map<String, Object>>> myFavorites(Long userId, int page, int size) {
        if (userId == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        if (page < 1) {
            page = 1;
        }
        if (size < 1 || size > 100) {
            size = 10;
        }

        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("create_time");
        Page<Favorite> pageParam = new Page<>(page, size);
        Page<Favorite> result = favoriteMapper.selectPage(pageParam, wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Favorite favorite : result.getRecords()) {
            Map<String, Object> vo = new HashMap<>(8);
            vo.put("favoriteId", favorite.getId());
            vo.put("itemId", favorite.getItemId());
            vo.put("itemType", favorite.getItemType());
            vo.put("favoriteTime", favorite.getCreateTime());
            // 根据物品类型关联物品信息
            fillItemInfo(vo, favorite.getItemId(), favorite.getItemType());
            records.add(vo);
        }
        return Result.success(records);
    }

    /**
     * 根据物品类型填充物品基础信息（title、category、images、status、location、createTime）
     */
    private void fillItemInfo(Map<String, Object> vo, Long itemId, String itemType) {
        if (itemId == null || itemType == null) {
            return;
        }
        if (Constants.ITEM_TYPE_LOST.equalsIgnoreCase(itemType)) {
            LostItem item = lostItemMapper.selectById(itemId);
            if (item != null) {
                vo.put("title", item.getTitle());
                vo.put("category", item.getCategory());
                vo.put("images", item.getImages());
                vo.put("status", item.getStatus());
                vo.put("location", item.getLocation());
                vo.put("createTime", item.getCreateTime());
            }
        } else if (Constants.ITEM_TYPE_FOUND.equalsIgnoreCase(itemType)) {
            FoundItem item = foundItemMapper.selectById(itemId);
            if (item != null) {
                vo.put("title", item.getTitle());
                vo.put("category", item.getCategory());
                vo.put("images", item.getImages());
                vo.put("status", item.getStatus());
                vo.put("location", item.getLocation());
                vo.put("createTime", item.getCreateTime());
            }
        }
    }
}

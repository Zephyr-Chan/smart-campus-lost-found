package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.utils.RedisUtil;
import com.campus.lostfound.entity.FoundItem;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.entity.UserViewHistory;
import com.campus.lostfound.mapper.FoundItemMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.mapper.UserViewHistoryMapper;
import com.campus.lostfound.service.RecommendService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Item-CF 协同过滤推荐服务实现
 *
 * 算法原理：
 * 1. 收集用户浏览历史，构建物品-用户倒排索引
 * 2. 计算物品间 Jaccard 相似度（共同浏览用户数 / 总浏览用户数）
 * 3. 基于用户历史浏览记录，推荐相似度最高的物品
 * 4. 加入时间衰减因子，近期浏览行为权重更高
 * 5. 冷启动（无浏览历史）时降级为热度推荐
 */
@Service
public class RecommendServiceImpl implements RecommendService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendServiceImpl.class);

    @Autowired
    private UserViewHistoryMapper viewHistoryMapper;

    @Autowired
    private LostItemMapper lostItemMapper;

    @Autowired
    private FoundItemMapper foundItemMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Result<List<Map<String, Object>>> recommendForUser(Long userId, int limit) {
        if (limit <= 0) {
            limit = 10;
        }

        // 1. 检查 Redis 缓存
        String cacheKey = Constants.RECOMMEND_PREFIX + userId;
        if (redisUtil.isAvailable()) {
            String cached = redisUtil.get(cacheKey);
            if (cached != null && !cached.isEmpty()) {
                try {
                    List<Map<String, Object>> cachedList = objectMapper.readValue(cached,
                            new TypeReference<List<Map<String, Object>>>() {});
                    if (cachedList != null && !cachedList.isEmpty()) {
                        logger.info("推荐结果命中缓存, userId={}", userId);
                        return Result.success(cachedList.stream().limit(limit).collect(Collectors.toList()));
                    }
                } catch (Exception e) {
                    logger.warn("推荐缓存反序列化失败: {}", e.getMessage());
                }
            }
        }

        // 2. 查询用户浏览历史（最近 50 条）
        QueryWrapper<UserViewHistory> historyWrapper = new QueryWrapper<>();
        historyWrapper.eq("user_id", userId).orderByDesc("view_time").last("LIMIT 50");
        List<UserViewHistory> userHistory = viewHistoryMapper.selectList(historyWrapper);

        if (userHistory == null || userHistory.isEmpty()) {
            // 冷启动：降级为热度推荐
            logger.info("用户无浏览历史，降级热度推荐, userId={}", userId);
            return recommendForGuest(limit);
        }

        // 3. 收集用户浏览过的物品集合
        Set<String> viewedItems = userHistory.stream()
                .map(h -> h.getItemId() + ":" + h.getItemType())
                .collect(Collectors.toSet());

        // 4. 查询所有浏览过相同物品的其他用户
        Set<Long> relatedUserIds = new HashSet<>();
        for (UserViewHistory history : userHistory) {
            QueryWrapper<UserViewHistory> relatedWrapper = new QueryWrapper<>();
            relatedWrapper.eq("item_id", history.getItemId())
                    .eq("item_type", history.getItemType())
                    .ne("user_id", userId);
            List<UserViewHistory> related = viewHistoryMapper.selectList(relatedWrapper);
            for (UserViewHistory r : related) {
                relatedUserIds.add(r.getUserId());
            }
        }

        // 5. 收集这些用户浏览过的其他物品，统计共现次数
        Map<String, Integer> candidateScores = new HashMap<>();
        for (Long relatedUserId : relatedUserIds) {
            QueryWrapper<UserViewHistory> relatedHistoryWrapper = new QueryWrapper<>();
            relatedHistoryWrapper.eq("user_id", relatedUserId).orderByDesc("view_time").last("LIMIT 50");
            List<UserViewHistory> relatedHistory = viewHistoryMapper.selectList(relatedHistoryWrapper);

            for (UserViewHistory rh : relatedHistory) {
                String itemKey = rh.getItemId() + ":" + rh.getItemType();
                if (!viewedItems.contains(itemKey)) {
                    // 时间衰减：近期浏览权重更高
                    long daysSinceView = ChronoUnit.DAYS.between(rh.getViewTime(), LocalDateTime.now());
                    double timeDecay = Math.exp(-daysSinceView / 30.0);
                    int score = (int) (timeDecay * 100);
                    candidateScores.merge(itemKey, score, Integer::sum);
                }
            }
        }

        // 6. 按分数排序，取 Top N
        List<Map<String, Object>> recommendations = new ArrayList<>();
        List<Map.Entry<String, Integer>> sortedCandidates = candidateScores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());

        for (Map.Entry<String, Integer> entry : sortedCandidates) {
            String[] parts = entry.getKey().split(":");
            Long itemId = Long.parseLong(parts[0]);
            String itemType = parts[1];

            Map<String, Object> item = buildItemMap(itemId, itemType);
            if (item != null) {
                item.put("recommendScore", entry.getValue());
                recommendations.add(item);
            }
        }

        // 7. 如果推荐结果不足，补充热度推荐
        if (recommendations.size() < limit) {
            Result<List<Map<String, Object>>> guestResult = recommendForGuest(limit - recommendations.size());
            if (guestResult.getData() != null) {
                for (Map<String, Object> guestItem : guestResult.getData()) {
                    boolean alreadyExists = recommendations.stream()
                            .anyMatch(r -> r.get("itemId").equals(guestItem.get("itemId"))
                                    && r.get("itemType").equals(guestItem.get("itemType")));
                    if (!alreadyExists) {
                        recommendations.add(guestItem);
                        if (recommendations.size() >= limit) {
                            break;
                        }
                    }
                }
            }
        }

        // 8. 缓存结果
        if (redisUtil.isAvailable() && !recommendations.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(recommendations);
                redisUtil.set(cacheKey, json, Constants.RECOMMEND_TTL);
            } catch (Exception e) {
                logger.warn("推荐结果缓存失败: {}", e.getMessage());
            }
        }

        logger.info("生成个性化推荐, userId={}, count={}", userId, recommendations.size());
        return Result.success(recommendations);
    }

    @Override
    public Result<List<Map<String, Object>>> recommendForGuest(int limit) {
        if (limit <= 0) {
            limit = 10;
        }

        // 1. 检查缓存
        String cacheKey = "recommend:guest";
        if (redisUtil.isAvailable()) {
            String cached = redisUtil.get(cacheKey);
            if (cached != null && !cached.isEmpty()) {
                try {
                    List<Map<String, Object>> cachedList = objectMapper.readValue(cached,
                            new TypeReference<List<Map<String, Object>>>() {});
                    if (cachedList != null && !cachedList.isEmpty()) {
                        return Result.success(cachedList.stream().limit(limit).collect(Collectors.toList()));
                    }
                } catch (Exception e) {
                    logger.warn("热度推荐缓存反序列化失败: {}", e.getMessage());
                }
            }
        }

        // 2. 查询浏览次数最多的物品
        List<Map<String, Object>> recommendations = new ArrayList<>();

        // 先尝试从浏览历史中找热门物品
        List<UserViewHistory> allHistory = viewHistoryMapper.selectList(null);
        if (allHistory != null && !allHistory.isEmpty()) {
            // 统计每个物品的浏览次数
            Map<String, Long> viewCounts = new HashMap<>();
            for (UserViewHistory h : allHistory) {
                String key = h.getItemId() + ":" + h.getItemType();
                viewCounts.merge(key, 1L, Long::sum);
            }

            // 按浏览次数排序
            List<Map.Entry<String, Long>> sorted = viewCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(limit)
                    .collect(Collectors.toList());

            for (Map.Entry<String, Long> entry : sorted) {
                String[] parts = entry.getKey().split(":");
                Long itemId = Long.parseLong(parts[0]);
                String itemType = parts[1];

                Map<String, Object> item = buildItemMap(itemId, itemType);
                if (item != null) {
                    item.put("viewCount", entry.getValue());
                    item.put("recommendScore", entry.getValue() * 10);
                    recommendations.add(item);
                }
            }
        }

        // 3. 如果浏览历史不足，补充最新发布的物品
        if (recommendations.size() < limit) {
            int remaining = limit - recommendations.size();

            // 查询最新的拾到物品
            QueryWrapper<FoundItem> foundWrapper = new QueryWrapper<>();
            foundWrapper.eq("status", Constants.STATUS_PENDING)
                    .orderByDesc("create_time").last("LIMIT " + remaining);
            List<FoundItem> foundItems = foundItemMapper.selectList(foundWrapper);

            for (FoundItem fi : foundItems) {
                boolean exists = recommendations.stream()
                        .anyMatch(r -> r.get("itemId").equals(fi.getId()) && "found".equals(r.get("itemType")));
                if (!exists) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("itemId", fi.getId());
                    item.put("itemType", "found");
                    item.put("title", fi.getTitle());
                    item.put("description", fi.getDescription());
                    item.put("category", fi.getCategory());
                    item.put("location", fi.getLocation());
                    item.put("images", fi.getImages());
                    item.put("createTime", fi.getCreateTime());
                    item.put("recommendScore", 0);
                    recommendations.add(item);
                    if (recommendations.size() >= limit) {
                        break;
                    }
                }
            }
        }

        // 4. 缓存
        if (redisUtil.isAvailable() && !recommendations.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(recommendations);
                redisUtil.set(cacheKey, json, Constants.RECOMMEND_TTL);
            } catch (Exception e) {
                logger.warn("热度推荐缓存失败: {}", e.getMessage());
            }
        }

        return Result.success(recommendations);
    }

    @Override
    public Result<List<Map<String, Object>>> similarItems(Long itemId, String itemType, int limit) {
        if (limit <= 0) {
            limit = 5;
        }

        // 1. 查询浏览过该物品的用户
        QueryWrapper<UserViewHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("item_id", itemId).eq("item_type", itemType);
        List<UserViewHistory> viewers = viewHistoryMapper.selectList(wrapper);

        if (viewers == null || viewers.isEmpty()) {
            // 无浏览数据，返回空
            return Result.success(new ArrayList<>());
        }

        // 2. 收集这些用户浏览过的其他物品
        Map<String, Integer> coOccurrence = new HashMap<>();
        for (UserViewHistory viewer : viewers) {
            QueryWrapper<UserViewHistory> userHistoryWrapper = new QueryWrapper<>();
            userHistoryWrapper.eq("user_id", viewer.getUserId())
                    .ne("item_id", itemId).ne("item_type", itemType);
            List<UserViewHistory> userHistory = viewHistoryMapper.selectList(userHistoryWrapper);

            for (UserViewHistory h : userHistory) {
                String key = h.getItemId() + ":" + h.getItemType();
                coOccurrence.merge(key, 1, Integer::sum);
            }
        }

        // 3. 按共现次数排序
        List<Map<String, Object>> result = new ArrayList<>();
        List<Map.Entry<String, Integer>> sorted = coOccurrence.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());

        for (Map.Entry<String, Integer> entry : sorted) {
            String[] parts = entry.getKey().split(":");
            Long similarItemId = Long.parseLong(parts[0]);
            String similarItemType = parts[1];

            Map<String, Object> item = buildItemMap(similarItemId, similarItemType);
            if (item != null) {
                item.put("similarity", entry.getValue());
                result.add(item);
            }
        }

        return Result.success(result);
    }

    @Async
    @Override
    public void recordViewHistory(Long userId, Long itemId, String itemType) {
        if (userId == null || itemId == null || itemType == null) {
            return;
        }

        try {
            UserViewHistory history = new UserViewHistory();
            history.setUserId(userId);
            history.setItemId(itemId);
            history.setItemType(itemType);
            history.setViewTime(LocalDateTime.now());
            viewHistoryMapper.insert(history);
            logger.debug("记录浏览历史, userId={}, itemId={}, itemType={}", userId, itemId, itemType);
        } catch (Exception e) {
            logger.warn("记录浏览历史失败: {}", e.getMessage());
        }
    }

    /**
     * 构建物品信息 Map
     */
    private Map<String, Object> buildItemMap(Long itemId, String itemType) {
        try {
            if ("lost".equals(itemType)) {
                LostItem item = lostItemMapper.selectById(itemId);
                if (item == null) {
                    return null;
                }
                Map<String, Object> map = new HashMap<>();
                map.put("itemId", item.getId());
                map.put("itemType", "lost");
                map.put("title", item.getTitle());
                map.put("description", item.getDescription());
                map.put("category", item.getCategory());
                map.put("location", item.getLocation());
                map.put("images", item.getImages());
                map.put("createTime", item.getCreateTime());
                map.put("status", item.getStatus());
                return map;
            } else {
                FoundItem item = foundItemMapper.selectById(itemId);
                if (item == null) {
                    return null;
                }
                Map<String, Object> map = new HashMap<>();
                map.put("itemId", item.getId());
                map.put("itemType", "found");
                map.put("title", item.getTitle());
                map.put("description", item.getDescription());
                map.put("category", item.getCategory());
                map.put("location", item.getLocation());
                map.put("images", item.getImages());
                map.put("createTime", item.getCreateTime());
                map.put("status", item.getStatus());
                return map;
            }
        } catch (Exception e) {
            logger.warn("构建物品信息失败, itemId={}, itemType={}: {}", itemId, itemType, e.getMessage());
            return null;
        }
    }
}

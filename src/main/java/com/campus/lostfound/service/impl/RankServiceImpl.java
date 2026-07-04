package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.utils.RedisUtil;
import com.campus.lostfound.entity.Claim;
import com.campus.lostfound.entity.FoundItem;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.FoundItemMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.service.RankService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 贡献排行榜服务实现类
 */
@Slf4j
@Service
public class RankServiceImpl implements RankService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LostItemMapper lostItemMapper;

    @Autowired
    private FoundItemMapper foundItemMapper;

    @Autowired
    private ClaimMapper claimMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Result<List<Map<String, Object>>> getRankList(String type, int limit) {
        // 参数兜底
        if (type == null || type.isEmpty()) {
            type = "credit";
        }
        if (limit <= 0 || limit > 100) {
            limit = 20;
        }

        // 1. 优先读取Redis缓存
        String cacheKey = Constants.RANK_PREFIX + type;
        String cached = redisUtil.get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            try {
                List<Map<String, Object>> cachedList = objectMapper.readValue(cached,
                        new TypeReference<List<Map<String, Object>>>() {
                        });
                if (cachedList != null) {
                    return Result.success(cachedList);
                }
            } catch (Exception e) {
                log.warn("排行榜缓存反序列化失败, key={}, 错误: {}", cacheKey, e.getMessage());
            }
        }

        // 2. 根据类型查询榜单
        List<Map<String, Object>> resultList;
        switch (type) {
            case "credit":
                resultList = buildCreditRank(limit);
                break;
            case "recovery":
                resultList = buildRecoveryRank(limit);
                break;
            case "contribution":
                resultList = buildContributionRank(limit);
                break;
            default:
                resultList = buildCreditRank(limit);
                break;
        }

        // 3. 写入Redis缓存
        try {
            String json = objectMapper.writeValueAsString(resultList);
            redisUtil.set(cacheKey, json, Constants.RANK_TTL);
        } catch (Exception e) {
            log.warn("排行榜缓存写入失败, key={}, 错误: {}", cacheKey, e.getMessage());
        }

        return Result.success(resultList);
    }

    /**
     * 积分榜：按信用积分降序
     */
    private List<Map<String, Object>> buildCreditRank(int limit) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("credit_score").last("LIMIT " + limit);
        List<User> users = userMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>(users.size());
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            Map<String, Object> item = new HashMap<>(8);
            item.put("rank", i + 1);
            item.put("userId", u.getId());
            item.put("username", u.getUsername());
            item.put("realName", u.getRealName());
            item.put("avatar", u.getAvatar());
            item.put("score", u.getCreditScore());
            result.add(item);
        }
        return result;
    }

    /**
     * 找回率榜：找回率 = 已通过/完成的认领数 / 该用户发布的丢失物品数
     */
    private List<Map<String, Object>> buildRecoveryRank(int limit) {
        // 每个用户发布的丢失物品总数
        QueryWrapper<LostItem> lostWrapper = new QueryWrapper<>();
        lostWrapper.select("user_id AS userId", "count(*) AS cnt").groupBy("user_id");
        List<Map<String, Object>> lostCounts = lostItemMapper.selectMaps(lostWrapper);
        Map<Long, Long> lostMap = new HashMap<>(lostCounts.size() * 2);
        for (Map<String, Object> m : lostCounts) {
            Long uid = toLong(m.get("userId"));
            Long total = toLong(m.get("cnt"));
            if (uid != null) {
                lostMap.put(uid, total);
            }
        }

        // 每个用户已找回（认领通过/完成）的数量
        QueryWrapper<Claim> claimWrapper = new QueryWrapper<>();
        claimWrapper.select("claimant_id AS claimantId", "count(*) AS cnt")
                .in("status", Constants.CLAIM_APPROVED, Constants.CLAIM_COMPLETED)
                .groupBy("claimant_id");
        List<Map<String, Object>> recoveredCounts = claimMapper.selectMaps(claimWrapper);
        Map<Long, Long> recoveredMap = new HashMap<>(recoveredCounts.size() * 2);
        for (Map<String, Object> m : recoveredCounts) {
            Long uid = toLong(m.get("claimantId"));
            Long recovered = toLong(m.get("cnt"));
            if (uid != null) {
                recoveredMap.put(uid, recovered);
            }
        }

        // 合并计算找回率（仅含发布过丢失物品的用户）
        Set<Long> userIds = new HashSet<>(lostMap.keySet());
        List<Map<String, Object>> tempList = new ArrayList<>(userIds.size());
        for (Long uid : userIds) {
            long total = lostMap.getOrDefault(uid, 0L);
            if (total == 0) {
                continue;
            }
            long recovered = recoveredMap.getOrDefault(uid, 0L);
            double rate = (double) recovered / total * 100;
            Map<String, Object> item = new HashMap<>(8);
            item.put("userId", uid);
            item.put("totalLost", total);
            item.put("recovered", recovered);
            item.put("recoveryRate", Math.round(rate * 100) / 100.0);
            tempList.add(item);
        }

        // 按找回率降序排序
        tempList.sort((a, b) -> {
            double ra = ((Number) a.get("recoveryRate")).doubleValue();
            double rb = ((Number) b.get("recoveryRate")).doubleValue();
            return Double.compare(rb, ra);
        });

        // 截取limit条
        List<Map<String, Object>> result = tempList.size() > limit
                ? new ArrayList<>(tempList.subList(0, limit))
                : tempList;

        // 填充用户信息并设置排名
        fillUserInfo(result);
        for (int i = 0; i < result.size(); i++) {
            result.get(i).put("rank", i + 1);
        }
        return result;
    }

    /**
     * 贡献榜：贡献值 = 发布的拾到物品数 + 作为被认领人且认领通过/完成的数量
     */
    private List<Map<String, Object>> buildContributionRank(int limit) {
        // 每个用户发布的拾到物品数
        QueryWrapper<FoundItem> foundWrapper = new QueryWrapper<>();
        foundWrapper.select("user_id AS userId", "count(*) AS cnt").groupBy("user_id");
        List<Map<String, Object>> foundCounts = foundItemMapper.selectMaps(foundWrapper);
        Map<Long, Long> foundMap = new HashMap<>(foundCounts.size() * 2);
        for (Map<String, Object> m : foundCounts) {
            Long uid = toLong(m.get("userId"));
            Long cnt = toLong(m.get("cnt"));
            if (uid != null) {
                foundMap.put(uid, cnt);
            }
        }

        // 每个用户作为被认领人（拾物发布者）且认领通过/完成的数量
        QueryWrapper<Claim> claimWrapper = new QueryWrapper<>();
        claimWrapper.select("respondent_id AS respondentId", "count(*) AS cnt")
                .in("status", Constants.CLAIM_APPROVED, Constants.CLAIM_COMPLETED)
                .groupBy("respondent_id");
        List<Map<String, Object>> approvedCounts = claimMapper.selectMaps(claimWrapper);
        Map<Long, Long> approvedMap = new HashMap<>(approvedCounts.size() * 2);
        for (Map<String, Object> m : approvedCounts) {
            Long uid = toLong(m.get("respondentId"));
            Long cnt = toLong(m.get("cnt"));
            if (uid != null) {
                approvedMap.put(uid, cnt);
            }
        }

        // 合并计算贡献值
        Set<Long> userIds = new HashSet<>(foundMap.keySet());
        userIds.addAll(approvedMap.keySet());
        List<Map<String, Object>> tempList = new ArrayList<>(userIds.size());
        for (Long uid : userIds) {
            long foundCount = foundMap.getOrDefault(uid, 0L);
            long approvedCount = approvedMap.getOrDefault(uid, 0L);
            long contribution = foundCount + approvedCount;
            if (contribution <= 0) {
                continue;
            }
            Map<String, Object> item = new HashMap<>(8);
            item.put("userId", uid);
            item.put("foundCount", foundCount);
            item.put("approvedCount", approvedCount);
            item.put("contribution", contribution);
            tempList.add(item);
        }

        // 按贡献值降序排序
        tempList.sort((a, b) -> {
            long ca = ((Number) a.get("contribution")).longValue();
            long cb = ((Number) b.get("contribution")).longValue();
            return Long.compare(cb, ca);
        });

        // 截取limit条
        List<Map<String, Object>> result = tempList.size() > limit
                ? new ArrayList<>(tempList.subList(0, limit))
                : tempList;

        // 填充用户信息并设置排名
        fillUserInfo(result);
        for (int i = 0; i < result.size(); i++) {
            result.get(i).put("rank", i + 1);
        }
        return result;
    }

    /**
     * 批量填充用户基本信息（username/realName/avatar）
     */
    private void fillUserInfo(List<Map<String, Object>> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> idList = list.stream()
                .map(m -> toLong(m.get("userId")))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        if (idList.isEmpty()) {
            return;
        }
        QueryWrapper<User> userWrapper = new QueryWrapper<>();
        userWrapper.in("id", idList);
        List<User> users = userMapper.selectList(userWrapper);
        Map<Long, User> userMap = new HashMap<>(users.size() * 2);
        for (User u : users) {
            userMap.put(u.getId(), u);
        }
        for (Map<String, Object> item : list) {
            Long uid = toLong(item.get("userId"));
            User u = uid == null ? null : userMap.get(uid);
            if (u != null) {
                item.put("username", u.getUsername());
                item.put("realName", u.getRealName());
                item.put("avatar", u.getAvatar());
            } else {
                item.put("username", "未知用户");
                item.put("realName", "");
                item.put("avatar", "");
            }
        }
    }

    /**
     * 安全地将数值对象转为Long
     */
    private Long toLong(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

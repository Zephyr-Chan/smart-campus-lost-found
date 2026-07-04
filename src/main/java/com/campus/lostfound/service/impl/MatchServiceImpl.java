package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.lostfound.client.AiMatchingClient;
import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.exception.BizException;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.result.ResultCode;
import com.campus.lostfound.common.utils.CosineSimilarityUtil;
import com.campus.lostfound.common.utils.GeohashUtil;
import com.campus.lostfound.common.utils.RedisUtil;
import com.campus.lostfound.common.utils.TfidfUtil;
import com.campus.lostfound.config.MatchingProperties;
import com.campus.lostfound.entity.FoundItem;
import com.campus.lostfound.entity.ItemLocation;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.mapper.FoundItemMapper;
import com.campus.lostfound.mapper.ItemLocationMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.service.MatchService;
import com.campus.lostfound.vo.MatchResultVO;
import com.campus.lostfound.websocket.WebSocketServer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能匹配服务实现类（核心）
 * <p>
 * 优先调用 Python AI 微服务进行多模态匹配（文本 + 图像 + 位置融合评分），
 * 当 AI 服务不可用时自动降级到 TF-IDF + 余弦相似度匹配，
 * 保证匹配能力在 AI 服务故障期间依然可用。
 * </p>
 */
@Service
public class MatchServiceImpl implements MatchService {

    private static final Logger logger = LoggerFactory.getLogger(MatchServiceImpl.class);

    /** 匹配类型：多模态（AI） */
    private static final String MATCH_TYPE_MULTIMODAL = "multimodal";

    /** 匹配类型：TF-IDF 降级 */
    private static final String MATCH_TYPE_TFIDF = "tfidf";

    /** 默认返回的匹配结果数量 */
    private static final int DEFAULT_TOP_K = 10;

    @Autowired
    private LostItemMapper lostItemMapper;

    @Autowired
    private FoundItemMapper foundItemMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ItemLocationMapper itemLocationMapper;

    @Autowired
    private TfidfUtil tfidfUtil;

    @Autowired
    private CosineSimilarityUtil cosineSimilarityUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AiMatchingClient aiMatchingClient;

    @Autowired
    private MatchingProperties matchingProperties;

    @Override
    public Result<List<MatchResultVO>> findMatches(Long lostItemId, Long currentUserId) {
        // 1. 查询丢失物品（所有登录用户均可查看匹配结果）
        LostItem lostItem = lostItemMapper.selectById(lostItemId);
        if (lostItem == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "丢失物品不存在");
        }

        // 2. 检测 AI 服务可用性，决定匹配策略
        boolean aiAvailable = aiMatchingClient.healthCheck();
        String matchType = aiAvailable ? MATCH_TYPE_MULTIMODAL : MATCH_TYPE_TFIDF;
        logger.info("匹配策略决策, lostItemId={}, aiAvailable={}, matchType={}", lostItemId, aiAvailable, matchType);

        // 3. 查询 Redis 缓存（key 包含匹配类型，避免不同策略结果串用）
        String cacheKey = Constants.MATCH_PREFIX + lostItemId + ":" + matchType;
        try {
            if (redisUtil.isAvailable()) {
                String cached = redisUtil.get(cacheKey);
                if (cached != null && !cached.isEmpty()) {
                    List<MatchResultVO> cachedList = objectMapper.readValue(cached,
                            new TypeReference<List<MatchResultVO>>() {});
                    logger.info("匹配结果命中缓存, lostItemId={}, matchType={}", lostItemId, matchType);
                    return Result.success(cachedList);
                }
            }
        } catch (Exception e) {
            logger.warn("读取Redis缓存失败，将重新计算匹配结果: {}", e.getMessage());
        }

        // 4. 执行匹配：AI 可用走多模态，否则降级 TF-IDF
        Result<List<MatchResultVO>> result;
        if (aiAvailable) {
            result = findMatchesByAi(lostItem);
            // AI 调用失败或无结果时，进一步降级到 TF-IDF
            if (result.getData() == null || result.getData().isEmpty()) {
                logger.warn("AI多模态匹配无结果，尝试TF-IDF降级, lostItemId={}", lostItemId);
                Result<List<MatchResultVO>> tfidfResult = findMatchesByTfidf(lostItemId, currentUserId);
                if (tfidfResult.getData() != null && !tfidfResult.getData().isEmpty()) {
                    result = tfidfResult;
                    matchType = MATCH_TYPE_TFIDF;
                }
            }
        } else {
            logger.info("AI服务不可用，使用TF-IDF匹配, lostItemId={}", lostItemId);
            result = findMatchesByTfidf(lostItemId, currentUserId);
        }

        // 5. 缓存结果
        cacheResults(cacheKey, result.getData());

        // 6. WebSocket 高匹配度通知
        notifyIfHighMatch(lostItem, result.getData(), lostItemId);

        return result;
    }

    /**
     * AI 多模态匹配：调用 Python AI 服务获取文本/图像融合匹配结果，并补充本地位置评分
     */
    private Result<List<MatchResultVO>> findMatchesByAi(LostItem lostItem) {
        List<MatchResultVO> results = new ArrayList<>();
        try {
            String imagePath = getFirstImage(lostItem.getImages());
            List<Map<String, Object>> matches = aiMatchingClient.matchItems(
                    lostItem.getId(), Constants.ITEM_TYPE_LOST,
                    lostItem.getDescription(), imagePath, DEFAULT_TOP_K);

            if (matches == null || matches.isEmpty()) {
                return Result.success(results);
            }

            // 查询丢失物品位置（用于位置评分，循环外只查一次）
            ItemLocation lostLocation = queryLocation(lostItem.getId(), Constants.ITEM_TYPE_LOST);
            double[] lostCoord = getCoordinates(lostLocation);

            for (Map<String, Object> m : matches) {
                Long foundItemId = getLong(m, "foundItemId", "item_id", "itemId");
                if (foundItemId == null) {
                    continue;
                }
                FoundItem fi = foundItemMapper.selectById(foundItemId);
                if (fi == null) {
                    continue;
                }

                double textScore = getDouble(m, "textScore", "text_sim", "text_score");
                double imageScore = getDouble(m, "imageScore", "image_sim", "image_score");

                // 位置评分：若双方均有位置，通过 GeohashUtil 计算 haversine 距离并转换为相似度
                Double distance = null;
                double locationScore = 0.0;
                ItemLocation foundLocation = queryLocation(foundItemId, Constants.ITEM_TYPE_FOUND);
                double[] foundCoord = getCoordinates(foundLocation);
                if (lostCoord != null && foundCoord != null) {
                    distance = GeohashUtil.haversineDistance(
                            lostCoord[0], lostCoord[1], foundCoord[0], foundCoord[1]);
                    locationScore = GeohashUtil.distanceToScore(distance);
                }

                // 融合评分（文本 + 图像 + 位置），权重来自 MatchingProperties
                double finalScore = matchingProperties.fuse(textScore, imageScore, locationScore);

                MatchResultVO vo = buildBaseVO(fi);
                vo.setTextScore(textScore);
                vo.setImageScore(imageScore);
                vo.setLocationScore(locationScore);
                vo.setFinalScore(finalScore);
                vo.setMatchScore(finalScore);
                vo.setDistance(distance);
                vo.setMatchType(MATCH_TYPE_MULTIMODAL);
                results.add(vo);
            }

            // 按融合评分降序，取 Top10
            results.sort((a, b) -> Double.compare(
                    b.getFinalScore() == null ? 0 : b.getFinalScore(),
                    a.getFinalScore() == null ? 0 : a.getFinalScore()));
            if (results.size() > DEFAULT_TOP_K) {
                results = new ArrayList<>(results.subList(0, DEFAULT_TOP_K));
            }
        } catch (Exception e) {
            logger.warn("AI多模态匹配异常, lostItemId={}: {}", lostItem.getId(), e.getMessage());
        }
        return Result.success(results);
    }

    /**
     * TF-IDF + 余弦相似度匹配（原有逻辑，作为 AI 不可用时的降级方案）
     */
    private Result<List<MatchResultVO>> findMatchesByTfidf(Long lostItemId, Long currentUserId) {
        LostItem lostItem = lostItemMapper.selectById(lostItemId);
        if (lostItem == null) {
            return Result.success(new ArrayList<>());
        }

        // 查询所有待匹配的 found_item
        QueryWrapper<FoundItem> wrapper = new QueryWrapper<>();
        wrapper.eq("status", Constants.STATUS_PENDING);
        List<FoundItem> foundItems = foundItemMapper.selectList(wrapper);

        List<MatchResultVO> results = new ArrayList<>();
        if (foundItems == null || foundItems.isEmpty()) {
            return Result.success(results);
        }

        // 对 lost_item 的 description 分词
        List<String> lostTokens = tfidfUtil.tokenize(lostItem.getDescription());

        // 收集所有 found_item 分词，构建语料库计算 IDF（复用分词结果避免重复 tokenize）
        List<List<String>> allDocuments = new ArrayList<>();
        List<List<String>> foundTokensList = new ArrayList<>();
        for (FoundItem fi : foundItems) {
            List<String> tokens = tfidfUtil.tokenize(fi.getDescription());
            foundTokensList.add(tokens);
            allDocuments.add(tokens);
        }
        allDocuments.add(lostTokens);
        Map<String, Double> idfMap = tfidfUtil.computeIDF(allDocuments);
        Map<String, Double> lostVector = tfidfUtil.buildTfIdfVector(lostTokens, idfMap);

        // 遍历每个 found_item：构建 TF-IDF 向量，计算余弦相似度
        for (int i = 0; i < foundItems.size(); i++) {
            FoundItem fi = foundItems.get(i);
            List<String> foundTokens = foundTokensList.get(i);
            Map<String, Double> foundVector = tfidfUtil.buildTfIdfVector(foundTokens, idfMap);
            double score = cosineSimilarityUtil.calculate(lostVector, foundVector);

            MatchResultVO vo = buildBaseVO(fi);
            vo.setMatchScore(score);
            vo.setTextScore(score);
            vo.setFinalScore(score);
            vo.setMatchType(MATCH_TYPE_TFIDF);
            results.add(vo);
        }

        // 按匹配分数降序，取 Top10
        results.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
        if (results.size() > DEFAULT_TOP_K) {
            results = new ArrayList<>(results.subList(0, DEFAULT_TOP_K));
        }
        return Result.success(results);
    }

    /**
     * 构建匹配结果 VO 的基础展示字段（标题/描述/分类/位置/时间/图片/拾取者）
     */
    private MatchResultVO buildBaseVO(FoundItem fi) {
        MatchResultVO vo = new MatchResultVO();
        vo.setFoundItemId(fi.getId());
        vo.setTitle(fi.getTitle());
        vo.setDescription(fi.getDescription());
        vo.setCategory(fi.getCategory());
        vo.setLocation(fi.getLocation());
        vo.setEventTime(fi.getEventTime());
        vo.setImages(fi.getImages());
        User finder = userMapper.selectById(fi.getUserId());
        if (finder != null) {
            vo.setFinderName(finder.getRealName() != null ? finder.getRealName() : finder.getUsername());
        }
        return vo;
    }

    /**
     * 查询物品位置记录
     */
    private ItemLocation queryLocation(Long itemId, String itemType) {
        try {
            QueryWrapper<ItemLocation> wrapper = new QueryWrapper<>();
            wrapper.eq("item_id", itemId).eq("item_type", itemType);
            return itemLocationMapper.selectOne(wrapper);
        } catch (Exception e) {
            logger.warn("查询物品位置失败, itemId={}, itemType={}: {}", itemId, itemType, e.getMessage());
            return null;
        }
    }

    /**
     * 从位置记录中提取经纬度坐标：优先使用经纬度字段，其次解码 geohash
     *
     * @return double[]{latitude, longitude}，无可用数据返回 null
     */
    private double[] getCoordinates(ItemLocation loc) {
        if (loc == null) {
            return null;
        }
        if (loc.getLongitude() != null && loc.getLatitude() != null) {
            return new double[]{loc.getLatitude().doubleValue(), loc.getLongitude().doubleValue()};
        }
        if (loc.getGeohash() != null && !loc.getGeohash().isEmpty()) {
            try {
                return GeohashUtil.decode(loc.getGeohash());
            } catch (Exception e) {
                logger.warn("Geohash解码失败, geohash={}: {}", loc.getGeohash(), e.getMessage());
            }
        }
        return null;
    }

    /**
     * 缓存匹配结果到 Redis
     */
    private void cacheResults(String cacheKey, List<MatchResultVO> data) {
        try {
            if (!redisUtil.isAvailable() || data == null || data.isEmpty()) {
                return;
            }
            String json = objectMapper.writeValueAsString(data);
            redisUtil.set(cacheKey, json, Constants.MATCH_TTL);
            logger.info("匹配结果已缓存, cacheKey={}, TTL={}min", cacheKey, Constants.MATCH_TTL);
        } catch (Exception e) {
            logger.warn("写入Redis缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 当 Top1 匹配分数超过阈值时，通过 WebSocket 推送通知给丢失物品发布者
     */
    private void notifyIfHighMatch(LostItem lostItem, List<MatchResultVO> results, Long lostItemId) {
        if (results == null || results.isEmpty()) {
            return;
        }
        MatchResultVO top = results.get(0);
        Double score = top.getFinalScore() != null ? top.getFinalScore() : top.getMatchScore();
        if (score == null || score <= matchingProperties.getThreshold()) {
            return;
        }
        try {
            Map<String, Object> notifyMsg = new HashMap<>();
            notifyMsg.put("type", Constants.MSG_TYPE_MATCH);
            notifyMsg.put("title", top.getTitle());
            notifyMsg.put("score", String.format("%.4f", score));
            notifyMsg.put("matchType", top.getMatchType());
            notifyMsg.put("itemId", lostItemId);
            WebSocketServer.sendMessage(lostItem.getUserId(), objectMapper.writeValueAsString(notifyMsg));
            logger.info("匹配通知已推送, userId={}, score={}, matchType={}",
                    lostItem.getUserId(), score, top.getMatchType());
        } catch (Exception e) {
            logger.warn("WebSocket通知发送失败: {}", e.getMessage());
        }
    }

    /**
     * 从 Map 中按多个候选 key 读取 double 值（兼容不同命名风格）
     */
    private double getDouble(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            if (map.containsKey(k)) {
                Object v = map.get(k);
                if (v instanceof Number) {
                    return ((Number) v).doubleValue();
                }
            }
        }
        return 0.0;
    }

    /**
     * 从 Map 中按多个候选 key 读取 Long 值（兼容数字/字符串两种返回形式）
     */
    private Long getLong(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            if (map.containsKey(k)) {
                Object v = map.get(k);
                if (v instanceof Number) {
                    return ((Number) v).longValue();
                }
                if (v instanceof String) {
                    try {
                        return Long.parseLong((String) v);
                    } catch (NumberFormatException ignored) {
                        // 忽略解析失败，继续尝试下一个 key
                    }
                }
            }
        }
        return null;
    }

    /**
     * 从逗号分隔的图片字符串中取第一张图片路径
     */
    private String getFirstImage(String images) {
        if (images == null || images.trim().isEmpty()) {
            return null;
        }
        String[] parts = images.split(",");
        return parts.length > 0 ? parts[0].trim() : null;
    }
}

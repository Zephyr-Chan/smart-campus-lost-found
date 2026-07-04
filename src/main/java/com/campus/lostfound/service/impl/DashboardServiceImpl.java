package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.entity.Claim;
import com.campus.lostfound.entity.FoundItem;
import com.campus.lostfound.entity.ItemLocation;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.FoundItemMapper;
import com.campus.lostfound.mapper.ItemLocationMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.service.DashboardService;
import com.campus.lostfound.vo.DashboardVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据看板服务实现类
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardServiceImpl.class);

    @Autowired
    private LostItemMapper lostItemMapper;

    @Autowired
    private FoundItemMapper foundItemMapper;

    @Autowired
    private ClaimMapper claimMapper;

    @Autowired
    private ItemLocationMapper itemLocationMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Result<DashboardVO> getOverview() {
        DashboardVO vo = new DashboardVO();

        // totalLost = 全部丢失物品数量（含已关闭，因为找回后会被设为已关闭）
        Long totalLost = lostItemMapper.selectCount(null);
        vo.setTotalLost(totalLost);

        // totalFound = 全部拾到物品数量
        Long totalFound = foundItemMapper.selectCount(null);
        vo.setTotalFound(totalFound);

        // totalRecovered = claim count where status = 3（已完成）
        QueryWrapper<Claim> claimWrapper = new QueryWrapper<>();
        claimWrapper.eq("status", Constants.CLAIM_COMPLETED);
        Long totalRecovered = claimMapper.selectCount(claimWrapper);
        vo.setTotalRecovered(totalRecovered);

        // totalPending = lost_item(status=0) + found_item(status=0)
        QueryWrapper<LostItem> lostPending = new QueryWrapper<>();
        lostPending.eq("status", Constants.STATUS_PENDING);
        Long lostPendingCount = lostItemMapper.selectCount(lostPending);

        QueryWrapper<FoundItem> foundPending = new QueryWrapper<>();
        foundPending.eq("status", Constants.STATUS_PENDING);
        Long foundPendingCount = foundItemMapper.selectCount(foundPending);
        vo.setTotalPending(lostPendingCount + foundPendingCount);

        // H4修复：recoveryRate = totalRecovered / totalLost * 100（totalLost现为全部丢失物品）
        double recoveryRate = totalLost > 0
                ? (double) totalRecovered / totalLost * 100
                : 0.0;
        BigDecimal bd = new BigDecimal(recoveryRate).setScale(2, RoundingMode.HALF_UP);
        vo.setRecoveryRate(bd.doubleValue());

        // categoryStats: 按category分组统计lost_item数量
        QueryWrapper<LostItem> catWrapper = new QueryWrapper<>();
        catWrapper.select("category", "count(*) AS count").groupBy("category");
        List<Map<String, Object>> categoryMaps = lostItemMapper.selectMaps(catWrapper);
        List<DashboardVO.CategoryStat> categoryStats = new ArrayList<>();
        for (Map<String, Object> map : categoryMaps) {
            String category = map.get("category") != null ? map.get("category").toString() : null;
            Long count = map.get("count") != null ? ((Number) map.get("count")).longValue() : 0L;
            categoryStats.add(new DashboardVO.CategoryStat(category, count));
        }
        vo.setCategoryStats(categoryStats);

        // locationStats: 按location分组统计lost_item数量（取Top10）
        QueryWrapper<LostItem> locWrapper = new QueryWrapper<>();
        locWrapper.select("location", "count(*) AS count")
                .groupBy("location")
                .orderByDesc("count")
                .last("LIMIT 10");
        List<Map<String, Object>> locationMaps = lostItemMapper.selectMaps(locWrapper);
        List<DashboardVO.LocationStat> locationStats = new ArrayList<>();
        for (Map<String, Object> map : locationMaps) {
            String location = map.get("location") != null ? map.get("location").toString() : null;
            Long count = map.get("count") != null ? ((Number) map.get("count")).longValue() : 0L;
            locationStats.add(new DashboardVO.LocationStat(location, count));
        }
        vo.setLocationStats(locationStats);

        return Result.success(vo);
    }

    @Override
    public Result<Map<String, Object>> getTrendData(int days) {
        if (days <= 0) {
            days = 30;
        }

        // 查询最近N天丢失物品每日数量
        QueryWrapper<LostItem> lostWrapper = new QueryWrapper<>();
        lostWrapper.select("DATE(create_time) AS date", "count(*) AS count")
                .apply("DATE(create_time) >= DATE_SUB(CURDATE(), INTERVAL {0} DAY)", days)
                .groupBy("DATE(create_time)")
                .orderByAsc("DATE(create_time)");
        List<Map<String, Object>> lostMaps = lostItemMapper.selectMaps(lostWrapper);

        // 查询最近N天拾到物品每日数量
        QueryWrapper<FoundItem> foundWrapper = new QueryWrapper<>();
        foundWrapper.select("DATE(create_time) AS date", "count(*) AS count")
                .apply("DATE(create_time) >= DATE_SUB(CURDATE(), INTERVAL {0} DAY)", days)
                .groupBy("DATE(create_time)")
                .orderByAsc("DATE(create_time)");
        List<Map<String, Object>> foundMaps = foundItemMapper.selectMaps(foundWrapper);

        // 构建日期范围内的完整数据（包含无数据的日期，填充0）
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1L);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 将查询结果转为 Map<日期字符串, 数量>
        Map<String, Long> lostMap = new LinkedHashMap<>();
        for (Map<String, Object> m : lostMaps) {
            String date = m.get("date") != null ? m.get("date").toString() : "";
            long count = m.get("count") != null ? ((Number) m.get("count")).longValue() : 0L;
            lostMap.put(date, count);
        }

        Map<String, Long> foundMap = new LinkedHashMap<>();
        for (Map<String, Object> m : foundMaps) {
            String date = m.get("date") != null ? m.get("date").toString() : "";
            long count = m.get("count") != null ? ((Number) m.get("count")).longValue() : 0L;
            foundMap.put(date, count);
        }

        // 遍历日期范围，组装返回数据
        List<String> dateList = new ArrayList<>();
        List<Long> lostCounts = new ArrayList<>();
        List<Long> foundCounts = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            String dateStr = date.format(formatter);
            dateList.add(dateStr);
            lostCounts.add(lostMap.getOrDefault(dateStr, 0L));
            foundCounts.add(foundMap.getOrDefault(dateStr, 0L));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dates", dateList);
        result.put("lostCounts", lostCounts);
        result.put("foundCounts", foundCounts);

        logger.info("趋势数据查询完成，天数={}，日期数={}", days, dateList.size());
        return Result.success(result);
    }

    @Override
    public Result<List<Map<String, Object>>> getHeatmapData() {
        // 查询item_location表，按geohash前5位分组统计物品数量及经纬度中心点
        QueryWrapper<ItemLocation> wrapper = new QueryWrapper<>();
        wrapper.select("SUBSTRING(geohash, 1, 5) AS geohash_prefix",
                        "count(*) AS count",
                        "AVG(longitude) AS lng",
                        "AVG(latitude) AS lat")
                .isNotNull("geohash")
                .ne("geohash", "")
                .groupBy("SUBSTRING(geohash, 1, 5)")
                .orderByDesc("count");
        List<Map<String, Object>> maps = itemLocationMapper.selectMaps(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> m : maps) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("geohash", m.get("geohash_prefix") != null ? m.get("geohash_prefix").toString() : "");
            item.put("count", m.get("count") != null ? ((Number) m.get("count")).longValue() : 0L);
            if (m.get("lng") != null) {
                item.put("lng", new BigDecimal(m.get("lng").toString()).setScale(6, RoundingMode.HALF_UP).doubleValue());
            }
            if (m.get("lat") != null) {
                item.put("lat", new BigDecimal(m.get("lat").toString()).setScale(6, RoundingMode.HALF_UP).doubleValue());
            }
            result.add(item);
        }

        logger.info("热力图数据查询完成，区域数={}", result.size());
        return Result.success(result);
    }

    @Override
    public Result<List<Map<String, Object>>> getUserActiveData() {
        // 查询最近30天每日新增注册用户数
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.select("DATE(create_time) AS date", "count(*) AS count")
                .apply("DATE(create_time) >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)")
                .groupBy("DATE(create_time)")
                .orderByAsc("DATE(create_time)");
        List<Map<String, Object>> maps = userMapper.selectMaps(wrapper);

        // 构建最近30天完整日期范围
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(29L);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Map<String, Long> dataMap = new LinkedHashMap<>();
        for (Map<String, Object> m : maps) {
            String date = m.get("date") != null ? m.get("date").toString() : "";
            long count = m.get("count") != null ? ((Number) m.get("count")).longValue() : 0L;
            dataMap.put(date, count);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            LocalDate date = startDate.plusDays(i);
            String dateStr = date.format(formatter);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", dateStr);
            item.put("count", dataMap.getOrDefault(dateStr, 0L));
            result.add(item);
        }

        logger.info("用户活跃度数据查询完成，天数={}", result.size());
        return Result.success(result);
    }

    @Override
    public Result<Map<String, Object>> getCategoryRecoveryRate() {
        // 查询丢失物品各分类总数
        QueryWrapper<LostItem> lostTotalWrapper = new QueryWrapper<>();
        lostTotalWrapper.select("category", "count(*) AS count")
                .groupBy("category");
        List<Map<String, Object>> lostTotalMaps = lostItemMapper.selectMaps(lostTotalWrapper);

        // 查询丢失物品各分类已匹配数（status=1已匹配 或 2已关闭）
        QueryWrapper<LostItem> lostMatchedWrapper = new QueryWrapper<>();
        lostMatchedWrapper.select("category", "count(*) AS count")
                .in("status", Constants.STATUS_MATCHED, Constants.STATUS_CLOSED)
                .groupBy("category");
        List<Map<String, Object>> lostMatchedMaps = lostItemMapper.selectMaps(lostMatchedWrapper);

        // 查询拾到物品各分类总数
        QueryWrapper<FoundItem> foundTotalWrapper = new QueryWrapper<>();
        foundTotalWrapper.select("category", "count(*) AS count")
                .groupBy("category");
        List<Map<String, Object>> foundTotalMaps = foundItemMapper.selectMaps(foundTotalWrapper);

        // 查询拾到物品各分类已匹配数
        QueryWrapper<FoundItem> foundMatchedWrapper = new QueryWrapper<>();
        foundMatchedWrapper.select("category", "count(*) AS count")
                .in("status", Constants.STATUS_MATCHED, Constants.STATUS_CLOSED)
                .groupBy("category");
        List<Map<String, Object>> foundMatchedMaps = foundItemMapper.selectMaps(foundMatchedWrapper);

        // 合并各分类数据
        Map<String, long[]> categoryData = new LinkedHashMap<>();
        for (Map<String, Object> m : lostTotalMaps) {
            String cat = m.get("category") != null ? m.get("category").toString() : "other";
            long count = m.get("count") != null ? ((Number) m.get("count")).longValue() : 0L;
            categoryData.computeIfAbsent(cat, k -> new long[2])[0] += count;
        }
        for (Map<String, Object> m : foundTotalMaps) {
            String cat = m.get("category") != null ? m.get("category").toString() : "other";
            long count = m.get("count") != null ? ((Number) m.get("count")).longValue() : 0L;
            categoryData.computeIfAbsent(cat, k -> new long[2])[0] += count;
        }
        for (Map<String, Object> m : lostMatchedMaps) {
            String cat = m.get("category") != null ? m.get("category").toString() : "other";
            long count = m.get("count") != null ? ((Number) m.get("count")).longValue() : 0L;
            categoryData.computeIfAbsent(cat, k -> new long[2])[1] += count;
        }
        for (Map<String, Object> m : foundMatchedMaps) {
            String cat = m.get("category") != null ? m.get("category").toString() : "other";
            long count = m.get("count") != null ? ((Number) m.get("count")).longValue() : 0L;
            categoryData.computeIfAbsent(cat, k -> new long[2])[1] += count;
        }

        // 分类中文映射
        Map<String, String> categoryNameMap = new LinkedHashMap<>();
        categoryNameMap.put("electronics", "电子产品");
        categoryNameMap.put("certificate", "证件");
        categoryNameMap.put("book", "书籍");
        categoryNameMap.put("clothing", "衣物");
        categoryNameMap.put("other", "其他");

        List<String> categories = new ArrayList<>();
        List<Long> totalCounts = new ArrayList<>();
        List<Long> matchedCounts = new ArrayList<>();
        List<Double> recoveryRates = new ArrayList<>();

        for (Map.Entry<String, long[]> entry : categoryData.entrySet()) {
            String cat = entry.getKey();
            long total = entry.getValue()[0];
            long matched = entry.getValue()[1];
            double rate = total > 0 ? (double) matched / total * 100 : 0.0;
            rate = new BigDecimal(rate).setScale(2, RoundingMode.HALF_UP).doubleValue();

            categories.add(categoryNameMap.getOrDefault(cat, cat));
            totalCounts.add(total);
            matchedCounts.add(matched);
            recoveryRates.add(rate);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categories", categories);
        result.put("totalCounts", totalCounts);
        result.put("matchedCounts", matchedCounts);
        result.put("recoveryRates", recoveryRates);

        logger.info("分类找回率查询完成，分类数={}", categories.size());
        return Result.success(result);
    }
}

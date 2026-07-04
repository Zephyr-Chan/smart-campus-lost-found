package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.utils.GeohashUtil;
import com.campus.lostfound.entity.ItemLocation;
import com.campus.lostfound.mapper.ItemLocationMapper;
import com.campus.lostfound.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 地理位置服务实现
 * 基于 GeoHash 实现空间索引和距离计算
 */
@Service
public class LocationServiceImpl implements LocationService {

    private static final Logger logger = LoggerFactory.getLogger(LocationServiceImpl.class);

    @Autowired
    private ItemLocationMapper itemLocationMapper;

    @Override
    public Result<Boolean> saveLocation(Long itemId, String itemType, Double longitude, Double latitude, String address) {
        if (itemId == null || itemType == null || longitude == null || latitude == null) {
            return Result.fail(400, "参数不完整");
        }

        // 校验经纬度取值范围
        if (longitude < -180 || longitude > 180) {
            return Result.fail(400, "经度取值范围为 -180 到 180");
        }
        if (latitude < -90 || latitude > 90) {
            return Result.fail(400, "纬度取值范围为 -90 到 90");
        }

        // 已知限制：当前方法未接收 userId，无法校验操作者是否为物品所有者。
        // 后续应在方法签名中增加 userId 参数，并在保存前校验物品归属。
        // 临时方案：依赖控制器层的登录校验（requireUserId）保证用户已登录。

        // 计算 GeoHash 编码（7位精度约 153m x 153m）
        String geohash = GeohashUtil.encode(latitude, longitude, Constants.GEOHASH_PRECISION);

        // 查询是否已存在位置记录
        QueryWrapper<ItemLocation> wrapper = new QueryWrapper<>();
        wrapper.eq("item_id", itemId).eq("item_type", itemType);
        ItemLocation existing = itemLocationMapper.selectOne(wrapper);

        if (existing != null) {
            // 更新
            existing.setLongitude(BigDecimal.valueOf(longitude));
            existing.setLatitude(BigDecimal.valueOf(latitude));
            existing.setGeohash(geohash);
            existing.setAddress(address);
            itemLocationMapper.updateById(existing);
            logger.info("更新物品位置, itemId={}, itemType={}, geohash={}", itemId, itemType, geohash);
        } else {
            // 新增
            ItemLocation location = new ItemLocation();
            location.setItemId(itemId);
            location.setItemType(itemType);
            location.setLongitude(BigDecimal.valueOf(longitude));
            location.setLatitude(BigDecimal.valueOf(latitude));
            location.setGeohash(geohash);
            location.setAddress(address);
            itemLocationMapper.insert(location);
            logger.info("保存物品位置, itemId={}, itemType={}, geohash={}", itemId, itemType, geohash);
        }

        return Result.success(true);
    }

    @Override
    public Result<Map<String, Object>> getLocation(Long itemId, String itemType) {
        QueryWrapper<ItemLocation> wrapper = new QueryWrapper<>();
        wrapper.eq("item_id", itemId).eq("item_type", itemType);
        ItemLocation location = itemLocationMapper.selectOne(wrapper);

        if (location == null) {
            return Result.success(null);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("id", location.getId());
        map.put("itemId", location.getItemId());
        map.put("itemType", location.getItemType());
        map.put("longitude", location.getLongitude());
        map.put("latitude", location.getLatitude());
        map.put("geohash", location.getGeohash());
        map.put("address", location.getAddress());
        return Result.success(map);
    }

    @Override
    public Result<List<Map<String, Object>>> getLocationList(String itemType) {
        QueryWrapper<ItemLocation> wrapper = new QueryWrapper<>();
        if (itemType != null && !itemType.isEmpty()) {
            wrapper.eq("item_type", itemType);
        }
        wrapper.orderByDesc("create_time");

        List<ItemLocation> locations = itemLocationMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();

        for (ItemLocation loc : locations) {
            Map<String, Object> map = new HashMap<>();
            map.put("itemId", loc.getItemId());
            map.put("itemType", loc.getItemType());
            map.put("longitude", loc.getLongitude());
            map.put("latitude", loc.getLatitude());
            map.put("address", loc.getAddress());
            result.add(map);
        }

        return Result.success(result);
    }

    @Override
    public Result<Double> calculateDistance(Long itemId1, String itemType1, Long itemId2, String itemType2) {
        QueryWrapper<ItemLocation> wrapper1 = new QueryWrapper<>();
        wrapper1.eq("item_id", itemId1).eq("item_type", itemType1);
        ItemLocation loc1 = itemLocationMapper.selectOne(wrapper1);

        QueryWrapper<ItemLocation> wrapper2 = new QueryWrapper<>();
        wrapper2.eq("item_id", itemId2).eq("item_type", itemType2);
        ItemLocation loc2 = itemLocationMapper.selectOne(wrapper2);

        if (loc1 == null || loc2 == null) {
            return Result.success(null);
        }

        double distance = GeohashUtil.haversineDistance(
                loc1.getLatitude().doubleValue(),
                loc1.getLongitude().doubleValue(),
                loc2.getLatitude().doubleValue(),
                loc2.getLongitude().doubleValue()
        );

        logger.info("计算距离, item1={}({}), item2={}({}), distance={}m",
                itemId1, itemType1, itemId2, itemType2, String.format("%.2f", distance));

        return Result.success(distance);
    }
}

package com.campus.lostfound.service;

import com.campus.lostfound.common.result.Result;

import java.util.List;
import java.util.Map;

/**
 * 地理位置服务接口
 */
public interface LocationService {

    /**
     * 保存物品位置信息
     */
    Result<Boolean> saveLocation(Long itemId, String itemType, Double longitude, Double latitude, String address);

    /**
     * 获取物品位置
     */
    Result<Map<String, Object>> getLocation(Long itemId, String itemType);

    /**
     * 获取位置列表（地图标注用）
     */
    Result<List<Map<String, Object>>> getLocationList(String itemType);

    /**
     * 计算两个物品之间的距离
     */
    Result<Double> calculateDistance(Long itemId1, String itemType1, Long itemId2, String itemType2);
}

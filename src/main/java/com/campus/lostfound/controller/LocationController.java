package com.campus.lostfound.controller;

import com.campus.lostfound.common.base.BaseController;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 地理位置控制器
 * 提供物品位置保存、查询、距离计算等接口
 */
@RestController
@RequestMapping("/api/location")
public class LocationController extends BaseController {

    @Autowired
    private LocationService locationService;

    /**
     * 保存物品位置（需登录）
     */
    @PostMapping("/save")
    public Result<Boolean> saveLocation(
            @RequestParam Long itemId,
            @RequestParam String itemType,
            @RequestParam Double longitude,
            @RequestParam Double latitude,
            @RequestParam(required = false) String address,
            HttpServletRequest request) {
        requireUserId(request);
        return locationService.saveLocation(itemId, itemType, longitude, latitude, address);
    }

    /**
     * 获取物品位置（公开）
     */
    @GetMapping("/{itemId}")
    public Result<?> getLocation(
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "lost") String type) {
        return locationService.getLocation(itemId, type);
    }

    /**
     * 获取位置列表（公开，地图标注用）
     */
    @GetMapping("/list")
    public Result<?> getLocationList(@RequestParam(required = false) String type) {
        return locationService.getLocationList(type);
    }

    /**
     * 计算两个物品之间的距离（公开）
     */
    @GetMapping("/distance")
    public Result<Double> calculateDistance(
            @RequestParam Long itemId1,
            @RequestParam String type1,
            @RequestParam Long itemId2,
            @RequestParam String type2) {
        return locationService.calculateDistance(itemId1, type1, itemId2, type2);
    }
}

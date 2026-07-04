package com.campus.lostfound.controller;

import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.service.DashboardService;
import com.campus.lostfound.vo.DashboardVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 数据看板控制器
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * 总览数据
     */
    @GetMapping("/overview")
    public Result overview() {
        return dashboardService.getOverview();
    }

    /**
     * 品类统计
     */
    @GetMapping("/category")
    public Result category() {
        Result<DashboardVO> result = dashboardService.getOverview();
        if (result.getData() == null) {
            return Result.success(java.util.Collections.emptyList());
        }
        return Result.success(result.getData().getCategoryStats());
    }

    /**
     * 地点统计
     */
    @GetMapping("/location")
    public Result location() {
        Result<DashboardVO> result = dashboardService.getOverview();
        if (result.getData() == null) {
            return Result.success(java.util.Collections.emptyList());
        }
        return Result.success(result.getData().getLocationStats());
    }

    /**
     * 每日发布趋势数据（丢失 vs 拾到）
     *
     * @param days 天数，默认30
     */
    @GetMapping("/trend")
    public Result<Map<String, Object>> trend(@RequestParam(defaultValue = "30") int days) {
        return dashboardService.getTrendData(days);
    }

    /**
     * 地理位置热力图数据
     */
    @GetMapping("/heatmap")
    public Result<List<Map<String, Object>>> heatmap() {
        return dashboardService.getHeatmapData();
    }

    /**
     * 用户活跃度数据（每日新增注册）
     */
    @GetMapping("/user-active")
    public Result<List<Map<String, Object>>> userActive() {
        return dashboardService.getUserActiveData();
    }

    /**
     * 各分类找回率
     */
    @GetMapping("/category-recovery")
    public Result<Map<String, Object>> categoryRecovery() {
        return dashboardService.getCategoryRecoveryRate();
    }
}

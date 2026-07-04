package com.campus.lostfound.service;

import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.vo.DashboardVO;

import java.util.List;
import java.util.Map;

/**
 * 数据看板服务接口
 */
public interface DashboardService {

    /**
     * 获取总览数据
     */
    Result<DashboardVO> getOverview();

    /**
     * 获取每日发布趋势数据（最近N天）
     *
     * @param days 天数
     * @return 趋势数据（日期、丢失数量、拾到数量）
     */
    Result<Map<String, Object>> getTrendData(int days);

    /**
     * 获取基于地理位置的物品分布热力图数据
     *
     * @return 热力图数据列表（geohash前缀、物品数量、经纬度中心点）
     */
    Result<List<Map<String, Object>>> getHeatmapData();

    /**
     * 获取用户活跃度数据（每日新增注册用户数）
     *
     * @return 用户活跃度数据列表
     */
    Result<List<Map<String, Object>>> getUserActiveData();

    /**
     * 获取各分类的找回率
     *
     * @return 分类找回率数据（分类名称、总数、已匹配数、找回率）
     */
    Result<Map<String, Object>> getCategoryRecoveryRate();
}

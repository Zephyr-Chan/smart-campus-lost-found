package com.campus.lostfound.service;

import javax.servlet.http.HttpServletResponse;

/**
 * 数据导出服务接口
 * 所有方法直接将 Excel 流写入 HttpServletResponse，无统一 Result 封装
 */
public interface ExportService {

    /**
     * 导出物品列表
     *
     * @param response HTTP 响应
     * @param itemType 物品类型（lost/found）
     * @param status   状态过滤（可为空）
     */
    void exportItems(HttpServletResponse response, String itemType, String status);

    /**
     * 导出认领记录（管理员）
     *
     * @param response HTTP 响应
     * @param status   状态过滤（可为空）
     */
    void exportClaims(HttpServletResponse response, String status);

    /**
     * 导出操作日志（管理员）
     *
     * @param response HTTP 响应
     */
    void exportLogs(HttpServletResponse response);

    /**
     * 导出我的认领记录
     *
     * @param response HTTP 响应
     * @param userId   当前用户ID
     */
    void exportMyClaims(HttpServletResponse response, Long userId);
}

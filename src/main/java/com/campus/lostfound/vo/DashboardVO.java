package com.campus.lostfound.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 数据看板VO
 */
@Data
public class DashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 失物总数
     */
    private Long totalLost;

    /**
     * 拾物总数
     */
    private Long totalFound;

    /**
     * 已找回总数
     */
    private Long totalRecovered;

    /**
     * 待处理总数
     */
    private Long totalPending;

    /**
     * 找回率
     */
    private Double recoveryRate;

    /**
     * 分类统计
     */
    private List<CategoryStat> categoryStats;

    /**
     * 地点统计
     */
    private List<LocationStat> locationStats;

    /**
     * 分类统计内部类
     */
    @Data
    public static class CategoryStat implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 分类名称
         */
        private String category;

        /**
         * 数量
         */
        private Long count;

        public CategoryStat() {
        }

        public CategoryStat(String category, Long count) {
            this.category = category;
            this.count = count;
        }
    }

    /**
     * 地点统计内部类
     */
    @Data
    public static class LocationStat implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 地点名称
         */
        private String location;

        /**
         * 数量
         */
        private Long count;

        public LocationStat() {
        }

        public LocationStat(String location, Long count) {
            this.location = location;
            this.count = count;
        }
    }
}

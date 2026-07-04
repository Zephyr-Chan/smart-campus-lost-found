package com.campus.lostfound.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 匹配结果VO
 */
@Data
public class MatchResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 拾物ID
     */
    private Long foundItemId;

    /**
     * 物品标题
     */
    private String title;

    /**
     * 物品描述
     */
    private String description;

    /**
     * 物品分类
     */
    private String category;

    /**
     * 拾取地点
     */
    private String location;

    /**
     * 拾取时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventTime;

    /**
     * 图片路径（多个用逗号分隔）
     */
    private String images;

    /**
     * 匹配分数
     */
    private Double matchScore;

    /**
     * 文本相似度分数
     */
    private Double textScore;

    /**
     * 图像相似度分数
     */
    private Double imageScore;

    /**
     * 位置相似度分数
     */
    private Double locationScore;

    /**
     * 最终融合分数
     */
    private Double finalScore;

    /**
     * 匹配类型（tfidf/multimodal）
     */
    private String matchType;

    /**
     * 距离（米）
     */
    private Double distance;

    /**
     * 拾取者姓名
     */
    private String finderName;
}

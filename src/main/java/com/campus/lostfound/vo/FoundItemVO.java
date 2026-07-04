package com.campus.lostfound.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 拾物信息VO
 */
@Data
public class FoundItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 发布者用户ID
     */
    private Long userId;

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
     * 联系方式
     */
    private String contactInfo;

    /**
     * 图片路径（多个用逗号分隔）
     */
    private String images;

    /**
     * 状态（0-待匹配，1-已匹配，2-已关闭）
     */
    private Integer status;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 拾取者姓名
     */
    private String finderName;
}

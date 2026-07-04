package com.campus.lostfound.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 拾物发布请求DTO
 */
@Data
public class FoundItemDTO {

    /**
     * 物品标题
     */
    @NotBlank(message = "标题不能为空")
    private String title;

    /**
     * 物品描述
     */
    @NotBlank(message = "描述不能为空")
    private String description;

    /**
     * 物品分类
     */
    @NotBlank(message = "分类不能为空")
    private String category;

    /**
     * 拾取地点
     */
    @NotBlank(message = "地点不能为空")
    private String location;

    /**
     * 拾取时间
     */
    @NotNull(message = "事件时间不能为空")
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
}

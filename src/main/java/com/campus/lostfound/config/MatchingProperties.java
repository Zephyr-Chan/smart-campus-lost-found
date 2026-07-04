package com.campus.lostfound.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 多模态匹配权重配置
 * 通过 application.yml 的 matching.weights 绑定，便于调参演示
 *
 * 融合评分公式：final_score = w_text * text_sim + w_image * image_sim + w_location * location_sim
 */
@Data
@Component
@ConfigurationProperties(prefix = "matching")
public class MatchingProperties {

    private Weights weights = new Weights();
    private double threshold = 0.3;

    @Data
    public static class Weights {
        private double text = 0.4;
        private double image = 0.4;
        private double location = 0.2;
    }

    /**
     * 计算融合评分
     */
    public double fuse(double textSim, double imageSim, double locationSim) {
        return weights.getText() * textSim
                + weights.getImage() * imageSim
                + weights.getLocation() * locationSim;
    }
}

package com.campus.lostfound.common.utils;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 余弦相似度计算工具类
 */
@Component
public class CosineSimilarityUtil {

    /**
     * 计算两个向量的余弦相似度
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 余弦相似度 [0, 1]
     */
    public double calculate(Map<String, Double> vec1, Map<String, Double> vec2) {
        if (vec1 == null || vec2 == null || vec1.isEmpty() || vec2.isEmpty()) {
            return 0.0;
        }
        // 点积
        double dotProduct = 0.0;
        for (Map.Entry<String, Double> entry : vec1.entrySet()) {
            Double v2 = vec2.get(entry.getKey());
            if (v2 != null) {
                dotProduct += entry.getValue() * v2;
            }
        }
        if (dotProduct == 0.0) {
            return 0.0;
        }
        // 向量1的模
        double mag1 = 0.0;
        for (double v : vec1.values()) {
            mag1 += v * v;
        }
        mag1 = Math.sqrt(mag1);
        // 向量2的模
        double mag2 = 0.0;
        for (double v : vec2.values()) {
            mag2 += v * v;
        }
        mag2 = Math.sqrt(mag2);
        if (mag1 == 0.0 || mag2 == 0.0) {
            return 0.0;
        }
        return dotProduct / (mag1 * mag2);
    }
}

package com.campus.lostfound;

import com.campus.lostfound.config.MatchingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多模态匹配权重配置单元测试
 */
public class MatchingPropertiesTest {

    private MatchingProperties properties;

    @BeforeEach
    public void setUp() {
        properties = new MatchingProperties();
        // 默认权重: text=0.4, image=0.4, location=0.2
    }

    @Test
    public void testDefaultWeights() {
        assertEquals(0.4, properties.getWeights().getText(), 0.001, "默认文本权重应为 0.4");
        assertEquals(0.4, properties.getWeights().getImage(), 0.001, "默认图像权重应为 0.4");
        assertEquals(0.2, properties.getWeights().getLocation(), 0.001, "默认位置权重应为 0.2");
    }

    @Test
    public void testFuseAllOnes() {
        // 所有分数为 1.0，融合结果应为 0.4+0.4+0.2 = 1.0
        double result = properties.fuse(1.0, 1.0, 1.0);
        assertEquals(1.0, result, 0.001, "全 1.0 融合结果应为 1.0");
    }

    @Test
    public void testFuseAllZeros() {
        double result = properties.fuse(0.0, 0.0, 0.0);
        assertEquals(0.0, result, 0.001, "全 0.0 融合结果应为 0.0");
    }

    @Test
    public void testFuseMixedScores() {
        // text=0.5, image=0.8, location=0.3
        // result = 0.4*0.5 + 0.4*0.8 + 0.2*0.3 = 0.2 + 0.32 + 0.06 = 0.58
        double result = properties.fuse(0.5, 0.8, 0.3);
        assertEquals(0.58, result, 0.001, "混合分数融合结果应为 0.58");
    }

    @Test
    public void testFuseCustomWeights() {
        // 修改权重
        properties.getWeights().setText(0.5);
        properties.getWeights().setImage(0.3);
        properties.getWeights().setLocation(0.2);

        // result = 0.5*0.6 + 0.3*0.9 + 0.2*0.5 = 0.3 + 0.27 + 0.1 = 0.67
        double result = properties.fuse(0.6, 0.9, 0.5);
        assertEquals(0.67, result, 0.001, "自定义权重融合结果应为 0.67");
    }

    @Test
    public void testThreshold() {
        assertEquals(0.3, properties.getThreshold(), 0.001, "默认阈值应为 0.3");

        properties.setThreshold(0.5);
        assertEquals(0.5, properties.getThreshold(), 0.001, "修改后阈值应为 0.5");
    }
}

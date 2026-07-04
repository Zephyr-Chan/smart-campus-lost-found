package com.campus.lostfound;

import com.campus.lostfound.common.utils.CosineSimilarityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 余弦相似度工具类单元测试
 */
public class CosineSimilarityUtilTest {

    private CosineSimilarityUtil cosineSimilarityUtil;

    @BeforeEach
    public void setUp() {
        cosineSimilarityUtil = new CosineSimilarityUtil();
    }

    @Test
    public void testSameVector() {
        Map<String, Double> vec = new HashMap<>();
        vec.put("钱包", 0.5);
        vec.put("黑色", 0.3);
        // 相同向量的余弦相似度应为1.0
        double similarity = cosineSimilarityUtil.calculate(vec, vec);
        assertEquals(1.0, similarity, 0.001, "相同向量的相似度应为1.0");
    }

    @Test
    public void testOrthogonalVector() {
        Map<String, Double> vec1 = new HashMap<>();
        vec1.put("钱包", 0.5);
        vec1.put("黑色", 0.3);

        Map<String, Double> vec2 = new HashMap<>();
        vec2.put("手机", 0.4);
        vec2.put("华为", 0.6);
        // 无交集词的向量相似度应为0.0
        double similarity = cosineSimilarityUtil.calculate(vec1, vec2);
        assertEquals(0.0, similarity, 0.001, "无交集词的向量相似度应为0.0");
    }

    @Test
    public void testSimilarVector() {
        Map<String, Double> vec1 = new HashMap<>();
        vec1.put("钱包", 0.5);
        vec1.put("黑色", 0.3);
        vec1.put("学生证", 0.2);

        Map<String, Double> vec2 = new HashMap<>();
        vec2.put("钱包", 0.4);
        vec2.put("黑色", 0.3);
        vec2.put("银行卡", 0.1);
        // 部分相似的向量相似度应在(0, 1)之间
        double similarity = cosineSimilarityUtil.calculate(vec1, vec2);
        assertTrue(similarity > 0.0 && similarity < 1.0, "部分相似向量的相似度应在(0,1)之间, 实际值=" + similarity);
    }

    @Test
    public void testEmptyVector() {
        Map<String, Double> vec1 = new HashMap<>();
        Map<String, Double> vec2 = new HashMap<>();
        vec2.put("钱包", 0.5);
        // 空向量应返回0.0
        double similarity = cosineSimilarityUtil.calculate(vec1, vec2);
        assertEquals(0.0, similarity, 0.001, "空向量的相似度应为0.0");
    }

    @Test
    public void testNullVector() {
        Map<String, Double> vec = new HashMap<>();
        vec.put("钱包", 0.5);
        // null向量应返回0.0
        double similarity = cosineSimilarityUtil.calculate(null, vec);
        assertEquals(0.0, similarity, 0.001, "null向量的相似度应为0.0");
    }

    @Test
    public void testZeroVector() {
        Map<String, Double> vec1 = new HashMap<>();
        vec1.put("钱包", 0.0);

        Map<String, Double> vec2 = new HashMap<>();
        vec2.put("钱包", 0.5);
        // 零向量应返回0.0
        double similarity = cosineSimilarityUtil.calculate(vec1, vec2);
        assertEquals(0.0, similarity, 0.001, "零向量的相似度应为0.0");
    }
}

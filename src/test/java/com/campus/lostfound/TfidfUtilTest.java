package com.campus.lostfound;

import com.campus.lostfound.common.utils.TfidfUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TF-IDF算法工具类单元测试
 */
public class TfidfUtilTest {

    private TfidfUtil tfidfUtil;

    @BeforeEach
    public void setUp() {
        tfidfUtil = new TfidfUtil();
    }

    @Test
    public void testTokenize() {
        List<String> tokens = tfidfUtil.tokenize("黑色钱包 里面有学生证");
        // 应包含"黑色"、"钱包"、"学生证"等词
        assertTrue(tokens.contains("黑色"), "分词结果应包含'黑色'");
        assertTrue(tokens.contains("钱包"), "分词结果应包含'钱包'");
        // "里面"是停用词，应被过滤
        assertFalse(tokens.contains("里面"), "'里面'是停用词，应被过滤");
    }

    @Test
    public void testTokenizeEmpty() {
        List<String> tokens = tfidfUtil.tokenize("");
        assertTrue(tokens.isEmpty(), "空文本分词结果应为空列表");
    }

    @Test
    public void testTokenizeNull() {
        List<String> tokens = tfidfUtil.tokenize(null);
        assertTrue(tokens.isEmpty(), "null分词结果应为空列表");
    }

    @Test
    public void testComputeTF() {
        List<String> tokens = Arrays.asList("黑色", "钱包", "钱包");
        Map<String, Double> tfMap = tfidfUtil.computeTF(tokens);
        // "钱包"出现2次，总共3个词，TF = 2/3 ≈ 0.667
        assertEquals(2.0 / 3.0, tfMap.get("钱包"), 0.001, "钱包的TF应为2/3");
        // "黑色"出现1次，TF = 1/3 ≈ 0.333
        assertEquals(1.0 / 3.0, tfMap.get("黑色"), 0.001, "黑色的TF应为1/3");
    }

    @Test
    public void testComputeIDF() {
        List<List<String>> allDocs = Arrays.asList(
                Arrays.asList("钱包", "黑色"),
                Arrays.asList("手机", "华为")
        );
        Map<String, Double> idfMap = tfidfUtil.computeIDF(allDocs);
        // "钱包"出现在1篇文档中，文档总数=2
        // 平滑IDF = log((N+1)/(df+1)) + 1 = log(3/2) + 1 ≈ 1.405
        double expectedIdf = Math.log((double) (2 + 1) / (1 + 1)) + 1.0;
        assertEquals(expectedIdf, idfMap.get("钱包"), 0.001, "钱包的IDF应为log(3/2)+1≈1.405");
        assertEquals(expectedIdf, idfMap.get("手机"), 0.001, "手机的IDF应为log(3/2)+1≈1.405");
    }

    @Test
    public void testComputeIDFSingleDoc() {
        List<List<String>> allDocs = Arrays.asList(
                Arrays.asList("钱包", "黑色")
        );
        Map<String, Double> idfMap = tfidfUtil.computeIDF(allDocs);
        // 文档总数=1，"钱包"出现在1篇中
        // 平滑IDF = log((1+1)/(1+1)) + 1 = log(1) + 1 = 1.0（非负）
        double idf = idfMap.get("钱包");
        assertTrue(idf > 0, "平滑后IDF应为正数");
        assertEquals(1.0, idf, 0.001, "单文档时IDF应为log(1)+1=1.0");
    }

    @Test
    public void testBuildTfIdfVector() {
        List<String> tokens = Arrays.asList("钱包", "黑色", "钱包");
        List<List<String>> allDocs = Arrays.asList(
                tokens,
                Arrays.asList("手机", "华为")
        );
        Map<String, Double> idfMap = tfidfUtil.computeIDF(allDocs);
        Map<String, Double> vector = tfidfUtil.buildTfIdfVector(tokens, idfMap);
        // TF-IDF = TF * IDF
        double tfOfWallet = 2.0 / 3.0;
        double idfOfWallet = idfMap.get("钱包");
        assertEquals(tfOfWallet * idfOfWallet, vector.get("钱包"), 0.001, "钱包的TF-IDF应为TF*IDF");
    }

    @Test
    public void testBuildTfIdfVectorEmpty() {
        Map<String, Double> vector = tfidfUtil.buildTfIdfVector(null, new java.util.HashMap<>());
        assertTrue(vector.isEmpty(), "空tokens的TF-IDF向量应为空");
    }
}

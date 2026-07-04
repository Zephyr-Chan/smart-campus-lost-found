package com.campus.lostfound.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TF-IDF算法工具类（纯Java实现，不依赖第三方分词库）
 */
@Slf4j
@Component
public class TfidfUtil {

    /** 中文停用词集合 */
    private static final Set<String> STOP_WORDS = new HashSet<>();

    static {
        String[] words = {
                "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "上",
                "也", "很", "到", "说", "要", "去", "你", "会", "着", "看", "好", "这", "那",
                "里", "为", "个", "们", "把", "被", "让", "从", "向", "对", "与", "及", "以",
                "但", "而", "或", "且", "一个", "一些", "这个", "那个", "这些", "那些",
                "可以", "什么", "怎么", "如果", "因为", "所以", "虽然", "但是", "然后",
                "里面", "内有", "里面", "没有", "不是", "还有", "带有", "含", "捡到", "丢失",
                "the", "a", "an", "is", "are", "was", "were", "in", "on", "at", "to",
                "for", "of", "and", "or", "not", "it", "this", "that"
        };
        for (String w : words) {
            STOP_WORDS.add(w);
        }
    }

    /**
     * 中文+英文分词
     * 策略：按标点符号和空格切分，中文部分采用单字+二字组合（bigram）方式
     *
     * @param text 待分词文本
     * @return 分词结果列表
     */
    public List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return tokens;
        }
        // 按标点符号、空格切分（使用Unicode转义避免编码问题）
        String[] parts = text.split("[\\s\\p{Punct}\uff0c\u3002\uff01\uff1f\uff1b\uff1a\u3001\u201c\u201d\u2018\u2019\uff08\uff09\u3010\u3011\u300a\u300b\u2014\u2026\u00b7]+");
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (part.matches("^[a-zA-Z0-9]+$")) {
                // 纯英文/数字：转小写，长度>1且非停用词
                String lower = part.toLowerCase();
                if (lower.length() > 1 && !STOP_WORDS.contains(lower)) {
                    tokens.add(lower);
                }
            } else {
                // 含中文字符：提取中文部分，采用单字+二字组合
                StringBuilder chineseBuf = new StringBuilder();
                for (int i = 0; i < part.length(); i++) {
                    char c = part.charAt(i);
                    if (isChineseChar(c)) {
                        chineseBuf.append(c);
                    } else {
                        processChinese(chineseBuf.toString(), tokens);
                        chineseBuf.setLength(0);
                        // 非中文字符按英文处理
                        if (Character.isLetterOrDigit(c)) {
                            tokens.add(String.valueOf(Character.toLowerCase(c)));
                        }
                    }
                }
                processChinese(chineseBuf.toString(), tokens);
            }
        }
        log.debug("分词结果: {}", tokens);
        return tokens;
    }

    private void processChinese(String chinese, List<String> tokens) {
        if (chinese.isEmpty()) {
            return;
        }
        for (int i = 0; i < chinese.length(); i++) {
            // 单字
            String ch = String.valueOf(chinese.charAt(i));
            if (!STOP_WORDS.contains(ch)) {
                tokens.add(ch);
            }
            // 二字组合（bigram）
            if (i + 1 < chinese.length()) {
                String bigram = chinese.substring(i, i + 2);
                if (!STOP_WORDS.contains(bigram)) {
                    tokens.add(bigram);
                }
            }
        }
    }

    private boolean isChineseChar(char c) {
        return c >= '\u4e00' && c <= '\u9fa5';
    }

    /**
     * 计算词频TF
     *
     * @param tokens 分词结果
     * @return 词频Map
     */
    public Map<String, Double> computeTF(List<String> tokens) {
        Map<String, Double> tfMap = new HashMap<>();
        if (tokens == null || tokens.isEmpty()) {
            return tfMap;
        }
        int total = tokens.size();
        Map<String, Integer> freq = new HashMap<>();
        for (String token : tokens) {
            freq.put(token, freq.getOrDefault(token, 0) + 1);
        }
        for (Map.Entry<String, Integer> entry : freq.entrySet()) {
            tfMap.put(entry.getKey(), (double) entry.getValue() / total);
        }
        return tfMap;
    }

    /**
     * 计算逆文档频率IDF（平滑公式，确保非负）
     * IDF = log((N+1) / (df+1)) + 1，其中N为文档总数，df为包含该词的文档数
     *
     * @param allDocs 所有文档的分词列表
     * @return IDF Map
     */
    public Map<String, Double> computeIDF(List<List<String>> allDocs) {
        Map<String, Double> idfMap = new HashMap<>();
        if (allDocs == null || allDocs.isEmpty()) {
            return idfMap;
        }
        int totalDocs = allDocs.size();
        Map<String, Integer> docFreq = new HashMap<>();
        for (List<String> doc : allDocs) {
            Set<String> uniqueTokens = new HashSet<>(doc);
            for (String token : uniqueTokens) {
                docFreq.put(token, docFreq.getOrDefault(token, 0) + 1);
            }
        }
        for (Map.Entry<String, Integer> entry : docFreq.entrySet()) {
            // 使用标准平滑公式 log((N+1)/(df+1)) + 1，确保IDF非负
            double idf = Math.log((double) (totalDocs + 1) / (entry.getValue() + 1)) + 1.0;
            idfMap.put(entry.getKey(), idf);
        }
        log.debug("IDF计算完成，文档总数={}，词条数={}", totalDocs, idfMap.size());
        return idfMap;
    }

    /**
     * 构建TF-IDF特征向量
     *
     * @param tokens  分词结果
     * @param idfMap  IDF词典
     * @return TF-IDF向量
     */
    public Map<String, Double> buildTfIdfVector(List<String> tokens, Map<String, Double> idfMap) {
        Map<String, Double> tfidfVector = new HashMap<>();
        if (tokens == null || tokens.isEmpty() || idfMap == null || idfMap.isEmpty()) {
            return tfidfVector;
        }
        Map<String, Double> tfMap = computeTF(tokens);
        for (Map.Entry<String, Double> entry : tfMap.entrySet()) {
            String word = entry.getKey();
            double tf = entry.getValue();
            double idf = idfMap.getOrDefault(word, 0.0);
            tfidfVector.put(word, tf * idf);
        }
        log.debug("TF-IDF向量构建完成，维度={}", tfidfVector.size());
        return tfidfVector;
    }
}

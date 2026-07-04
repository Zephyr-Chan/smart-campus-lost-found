package com.campus.lostfound.client;

import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.utils.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 匹配服务客户端
 * 封装对 Python AI 微服务（多模态向量匹配）的调用，内置熔断式自动降级：
 * <ul>
 *     <li>调用失败时在 Redis 写入降级标记（TTL 60s），冷却期内直接短路返回，避免反复请求已宕机的服务</li>
 *     <li>所有方法均做异常兜底，失败时记录告警并返回 null/false，不影响主流程</li>
 * </ul>
 */
@Component
public class AiMatchingClient {

    private static final Logger logger = LoggerFactory.getLogger(AiMatchingClient.class);

    /** 降级标记缓存 key */
    private static final String DEGRADE_KEY = Constants.AI_DEGRADE_PREFIX + "flag";

    @Value("${ai.service.url:http://localhost:8001}")
    private String aiServiceUrl;

    @Value("${ai.service.timeout:5000}")
    private int timeout;

    @Value("${ai.service.enabled:true}")
    private boolean enabled;

    @Autowired
    private RedisUtil redisUtil;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        this.restTemplate = new RestTemplate(factory);
        logger.info("AiMatchingClient初始化完成, url={}, timeout={}ms, enabled={}", aiServiceUrl, timeout, enabled);
    }

    /**
     * 健康检查
     * <p>若 AI 服务被关闭（enabled=false）或处于降级冷却期，直接返回 false 不发起 HTTP 请求。</p>
     *
     * @return true=服务可用；false=不可用或已降级
     */
    public boolean healthCheck() {
        if (!enabled) {
            return false;
        }
        if (isDegraded()) {
            return false;
        }
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(
                    aiServiceUrl + "/api/ai/health", Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return true;
            }
            markDegraded();
            return false;
        } catch (Exception e) {
            logger.warn("AI服务健康检查失败: {}", e.getMessage());
            markDegraded();
            return false;
        }
    }

    /**
     * 调用 AI 服务提取文本/图像向量
     *
     * @param text      物品描述文本
     * @param imagePath 图片路径（可为 null）
     * @return 向量信息 Map，失败返回 null
     */
    public Map<String, Object> extractVectors(String text, String imagePath) {
        if (!enabled || isDegraded()) {
            return null;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("text", text);
            body.put("imagePath", imagePath);

            ParameterizedTypeReference<Map<String, Object>> typeRef =
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    };
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    aiServiceUrl + "/api/ai/extract", HttpMethod.POST, new HttpEntity<>(body), typeRef);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return resp.getBody();
            }
            return null;
        } catch (Exception e) {
            logger.warn("AI向量提取失败: {}", e.getMessage());
            markDegraded();
            return null;
        }
    }

    /**
     * 将物品向量索引到 AI 服务
     *
     * @param itemId    物品ID
     * @param itemType  物品类型（lost/found）
     * @param text      描述文本
     * @param imagePath 图片路径（可为 null）
     * @return true=索引成功；false=失败或已降级
     */
    public boolean indexItem(Long itemId, String itemType, String text, String imagePath) {
        if (!enabled || isDegraded()) {
            return false;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("itemId", itemId);
            body.put("itemType", itemType);
            body.put("text", text);
            body.put("imagePath", imagePath);

            ResponseEntity<Object> resp = restTemplate.postForEntity(
                    aiServiceUrl + "/api/ai/index", body, Object.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.warn("AI索引物品失败, itemId={}, itemType={}: {}", itemId, itemType, e.getMessage());
            markDegraded();
            return false;
        }
    }

    /**
     * 多模态匹配查询
     *
     * @param itemId    查询物品ID
     * @param itemType  查询物品类型（lost/found）
     * @param text      描述文本
     * @param imagePath 图片路径（可为 null）
     * @param topK      返回结果数量
     * @return 匹配结果列表，每项为包含 foundItemId/textScore/imageScore/finalScore 等字段的 Map；失败返回 null
     */
    public List<Map<String, Object>> matchItems(Long itemId, String itemType, String text, String imagePath, int topK) {
        if (!enabled || isDegraded()) {
            return null;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("itemId", itemId);
            body.put("itemType", itemType);
            body.put("text", text);
            body.put("imagePath", imagePath);
            body.put("topK", topK);

            ParameterizedTypeReference<List<Map<String, Object>>> typeRef =
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    };
            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    aiServiceUrl + "/api/ai/match", HttpMethod.POST, new HttpEntity<>(body), typeRef);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return resp.getBody();
            }
            return null;
        } catch (Exception e) {
            logger.warn("AI多模态匹配失败, itemId={}: {}", itemId, e.getMessage());
            markDegraded();
            return null;
        }
    }

    /**
     * 是否处于降级冷却期
     */
    private boolean isDegraded() {
        try {
            if (!redisUtil.isAvailable()) {
                return false;
            }
            return redisUtil.hasKey(DEGRADE_KEY);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 标记降级，60 秒冷却避免反复请求已宕机的服务
     */
    private void markDegraded() {
        try {
            if (!redisUtil.isAvailable()) {
                return;
            }
            // Constants.AI_DEGRADE_TTL 单位为秒，RedisUtil#set 以分钟为单位，此处向上取整换算
            long ttlMinutes = Math.max(1, (Constants.AI_DEGRADE_TTL + 59) / 60);
            redisUtil.set(DEGRADE_KEY, "1", ttlMinutes);
            logger.info("AI服务已标记降级, 冷却{}秒", Constants.AI_DEGRADE_TTL);
        } catch (Exception e) {
            logger.warn("标记AI降级状态失败: {}", e.getMessage());
        }
    }
}

package com.campus.lostfound;

import com.campus.lostfound.client.AiMatchingClient;
import com.campus.lostfound.common.utils.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AI 匹配客户端单元测试
 * 验证服务降级逻辑
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AiMatchingClientTest {

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private AiMatchingClient aiMatchingClient;

    @BeforeEach
    public void setUp() {
        // 设置 @Value 属性
        ReflectionTestUtils.setField(aiMatchingClient, "aiServiceUrl", "http://localhost:8001");
        ReflectionTestUtils.setField(aiMatchingClient, "timeout", 5000);
        ReflectionTestUtils.setField(aiMatchingClient, "enabled", true);
    }

    @Test
    public void testHealthCheckWhenDisabled() {
        ReflectionTestUtils.setField(aiMatchingClient, "enabled", false);
        boolean result = aiMatchingClient.healthCheck();
        assertFalse(result, "AI 服务未启用时应返回 false");
    }

    @Test
    public void testHealthCheckWhenDegraded() {
        // 模拟 Redis 可用且降级标记存在
        when(redisUtil.isAvailable()).thenReturn(true);
        when(redisUtil.get("ai:degrade:flag")).thenReturn("1");

        boolean result = aiMatchingClient.healthCheck();
        assertFalse(result, "降级标记存在时应返回 false");
    }

    @Test
    public void testHealthCheckRedisUnavailable() {
        // Redis 不可用时，降级检查跳过，尝试 HTTP 调用（会失败因为无服务）
        when(redisUtil.isAvailable()).thenReturn(false);

        boolean result = aiMatchingClient.healthCheck();
        assertFalse(result, "AI 服务不可达时应返回 false");
    }

    @Test
    public void testMatchItemsWhenDegraded() {
        // 设置降级标记
        when(redisUtil.isAvailable()).thenReturn(true);
        when(redisUtil.get("ai:degrade:flag")).thenReturn("1");

        var result = aiMatchingClient.matchItems(1L, "lost", "测试文本", null, 10);
        assertNull(result, "降级时 matchItems 应返回 null");
    }

    @Test
    public void testIndexItemWhenDisabled() {
        ReflectionTestUtils.setField(aiMatchingClient, "enabled", false);
        boolean result = aiMatchingClient.indexItem(1L, "lost", "测试", null);
        assertFalse(result, "AI 未启用时 indexItem 应返回 false");
    }

    @Test
    public void testExtractVectorsWhenDisabled() {
        ReflectionTestUtils.setField(aiMatchingClient, "enabled", false);
        var result = aiMatchingClient.extractVectors("测试文本", null);
        assertNull(result, "AI 未启用时 extractVectors 应返回 null");
    }
}

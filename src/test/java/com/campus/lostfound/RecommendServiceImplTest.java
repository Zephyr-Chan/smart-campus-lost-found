package com.campus.lostfound;

import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.utils.RedisUtil;
import com.campus.lostfound.entity.FoundItem;
import com.campus.lostfound.entity.UserViewHistory;
import com.campus.lostfound.mapper.FoundItemMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.mapper.UserViewHistoryMapper;
import com.campus.lostfound.service.impl.RecommendServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Item-CF 推荐服务单元测试
 */
@ExtendWith(MockitoExtension.class)
public class RecommendServiceImplTest {

    @Mock
    private UserViewHistoryMapper viewHistoryMapper;

    @Mock
    private LostItemMapper lostItemMapper;

    @Mock
    private FoundItemMapper foundItemMapper;

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private RecommendServiceImpl recommendService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(recommendService, "objectMapper", new ObjectMapper());
    }

    @Test
    public void testRecommendForGuestWithNoHistory() {
        // 无浏览历史时，应降级到最新物品
        when(redisUtil.isAvailable()).thenReturn(false);
        when(viewHistoryMapper.selectList(any())).thenReturn(Collections.emptyList());

        // 模拟查询最新拾到物品
        FoundItem foundItem = new FoundItem();
        foundItem.setId(1L);
        foundItem.setTitle("测试拾到物品");
        foundItem.setCategory("electronics");
        foundItem.setStatus(Constants.STATUS_PENDING);
        when(foundItemMapper.selectList(any())).thenReturn(Arrays.asList(foundItem));

        Result<List<Map<String, Object>>> result = recommendService.recommendForGuest(10);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertFalse(result.getData().isEmpty(), "应返回最新物品作为推荐");
        assertEquals("测试拾到物品", result.getData().get(0).get("title"));
    }

    @Test
    public void testRecommendForGuestCached() {
        // Redis 缓存命中
        when(redisUtil.isAvailable()).thenReturn(true);
        String cachedJson = "[{\"itemId\":1,\"itemType\":\"found\",\"title\":\"缓存物品\"}]";
        when(redisUtil.get("recommend:guest")).thenReturn(cachedJson);

        Result<List<Map<String, Object>>> result = recommendService.recommendForGuest(10);

        assertNotNull(result);
        assertNotNull(result.getData());
        assertFalse(result.getData().isEmpty());
        assertEquals("缓存物品", result.getData().get(0).get("title"));
    }

    @Test
    public void testRecordViewHistory() {
        recommendService.recordViewHistory(1L, 100L, "lost");

        // 验证 insert 被调用
        verify(viewHistoryMapper, times(1)).insert(any(UserViewHistory.class));
    }

    @Test
    public void testRecordViewHistoryNullParams() {
        recommendService.recordViewHistory(null, 100L, "lost");
        recommendService.recordViewHistory(1L, null, "lost");
        recommendService.recordViewHistory(1L, 100L, null);

        // 参数为 null 时不应调用 insert
        verify(viewHistoryMapper, never()).insert(any());
    }

    @Test
    public void testSimilarItemsWithNoViewers() {
        // 无浏览数据
        when(viewHistoryMapper.selectList(any())).thenReturn(Collections.emptyList());

        Result<List<Map<String, Object>>> result = recommendService.similarItems(1L, "lost", 5);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty(), "无浏览数据时应返回空列表");
    }
}

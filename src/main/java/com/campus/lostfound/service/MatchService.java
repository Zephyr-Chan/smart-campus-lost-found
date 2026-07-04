package com.campus.lostfound.service;

import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.vo.MatchResultVO;

import java.util.List;

/**
 * 智能匹配服务接口
 */
public interface MatchService {

    /**
     * 查找匹配的拾到物品（核心匹配方法）
     *
     * @param lostItemId  丢失物品ID
     * @param currentUserId 当前登录用户ID（用于权限校验）
     * @return 匹配结果列表
     */
    Result<List<MatchResultVO>> findMatches(Long lostItemId, Long currentUserId);
}

package com.campus.lostfound.service;

import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.entity.Claim;

import java.util.List;

/**
 * 认领服务接口
 */
public interface ClaimService {

    /**
     * 创建认领申请
     */
    Result create(Long claimantId, Long lostItemId, Long foundItemId, Double matchScore, String remark);

    /**
     * 审核通过
     */
    Result approve(Long userId, Long claimId);

    /**
     * 审核拒绝
     */
    Result reject(Long userId, Long claimId);

    /**
     * 确认完成
     */
    Result complete(Long userId, Long claimId);

    /**
     * 我的认领列表
     */
    Result<List<Claim>> myList(Long userId);
}

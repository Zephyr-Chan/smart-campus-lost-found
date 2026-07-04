package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.exception.BizException;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.result.ResultCode;
import com.campus.lostfound.entity.Claim;
import com.campus.lostfound.entity.FoundItem;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.FoundItemMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.service.ClaimService;
import com.campus.lostfound.service.CreditService;
import com.campus.lostfound.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 认领服务实现类
 */
@Service
public class ClaimServiceImpl implements ClaimService {

    @Autowired
    private ClaimMapper claimMapper;

    @Autowired
    private LostItemMapper lostItemMapper;

    @Autowired
    private FoundItemMapper foundItemMapper;

    @Autowired
    private CreditService creditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result create(Long claimantId, Long lostItemId, Long foundItemId, Double matchScore, String remark) {
        // 查找拾到物品，确定发布人（respondent）
        FoundItem foundItem = foundItemMapper.selectById(foundItemId);
        if (foundItem == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "拾到物品不存在");
        }

        // H1修复：校验拾到物品状态（只有待处理状态才能认领）
        if (foundItem.getStatus() != Constants.STATUS_PENDING) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "该物品已被认领或已关闭，无法发起认领");
        }

        // H1修复：不能认领自己发布的拾到物品
        if (foundItem.getUserId().equals(claimantId)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "不能认领自己发布的拾到物品");
        }

        // H1修复：如果传了lostItemId，校验丢失物品存在且属于认领人
        if (lostItemId != null && lostItemId > 0) {
            LostItem lostItem = lostItemMapper.selectById(lostItemId);
            if (lostItem == null) {
                throw new BizException(ResultCode.NOT_FOUND.getCode(), "丢失物品不存在");
            }
            if (!lostItem.getUserId().equals(claimantId)) {
                throw new BizException(ResultCode.FORBIDDEN.getCode(), "只能对自己发布的丢失物品发起认领");
            }
        }

        // H1修复：重复认领检查（同一认领人对同一拾到物品已有待审核申请）
        QueryWrapper<Claim> dupWrapper = new QueryWrapper<>();
        dupWrapper.eq("claimant_id", claimantId)
                .eq("found_item_id", foundItemId)
                .eq("status", Constants.CLAIM_PENDING);
        Long existCount = claimMapper.selectCount(dupWrapper);
        if (existCount > 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "您已对该物品提交过认领申请，请等待审核");
        }

        Claim claim = new Claim();
        claim.setLostItemId(lostItemId);
        claim.setFoundItemId(foundItemId);
        claim.setClaimantId(claimantId);
        claim.setRespondentId(foundItem.getUserId());
        claim.setMatchScore(matchScore != null ? BigDecimal.valueOf(matchScore) : null);
        claim.setStatus(Constants.CLAIM_PENDING);
        claim.setRemark(remark);
        claimMapper.insert(claim);

        // WebSocket通知respondent（拾到物品发布者）
        String message = String.format(
                "{\"type\":\"claim\",\"action\":\"new\",\"claimId\":%d,\"foundItemId\":%d}",
                claim.getId(), foundItemId);
        WebSocketServer.sendMessage(foundItem.getUserId(), message);

        return Result.success(claim);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result approve(Long userId, Long claimId) {
        Claim claim = claimMapper.selectById(claimId);
        if (claim == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "认领记录不存在");
        }
        if (!claim.getRespondentId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权审核此认领申请");
        }
        if (claim.getStatus() != Constants.CLAIM_PENDING) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "认领申请状态不允许此操作");
        }

        // H2修复：校验拾到物品状态（只有待处理状态才能审核通过）
        FoundItem foundItem = foundItemMapper.selectById(claim.getFoundItemId());
        if (foundItem == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "关联的拾到物品不存在");
        }
        if (foundItem.getStatus() != Constants.STATUS_PENDING) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "该物品已被其他申请认领或已关闭，无法审核通过");
        }

        claim.setStatus(Constants.CLAIM_APPROVED);
        claimMapper.updateById(claim);

        // 更新物品状态为已匹配
        if (claim.getLostItemId() != null) {
            LostItem lostItem = lostItemMapper.selectById(claim.getLostItemId());
            if (lostItem != null && lostItem.getStatus() != Constants.STATUS_CLOSED) {
                lostItem.setStatus(Constants.STATUS_MATCHED);
                lostItemMapper.updateById(lostItem);
            }
        }
        foundItem.setStatus(Constants.STATUS_MATCHED);
        foundItemMapper.updateById(foundItem);

        // 自动拒绝同一拾到物品的其他待审核认领申请
        QueryWrapper<Claim> otherClaimsWrapper = new QueryWrapper<>();
        otherClaimsWrapper.eq("found_item_id", claim.getFoundItemId())
                .eq("status", Constants.CLAIM_PENDING)
                .ne("id", claimId);
        List<Claim> otherPendingClaims = claimMapper.selectList(otherClaimsWrapper);
        for (Claim otherClaim : otherPendingClaims) {
            otherClaim.setStatus(Constants.CLAIM_REJECTED);
            claimMapper.updateById(otherClaim);
            // 通知其他认领人申请被拒绝
            String rejectMsg = String.format(
                    "{\"type\":\"claim\",\"action\":\"rejected\",\"claimId\":%d}", otherClaim.getId());
            WebSocketServer.sendMessage(otherClaim.getClaimantId(), rejectMsg);
        }

        // WebSocket通知claimant（认领人）
        String message = String.format(
                "{\"type\":\"claim\",\"action\":\"approved\",\"claimId\":%d}", claimId);
        WebSocketServer.sendMessage(claim.getClaimantId(), message);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result reject(Long userId, Long claimId) {
        Claim claim = claimMapper.selectById(claimId);
        if (claim == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "认领记录不存在");
        }
        if (!claim.getRespondentId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权审核此认领申请");
        }
        if (claim.getStatus() != Constants.CLAIM_PENDING) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "认领申请状态不允许此操作");
        }

        claim.setStatus(Constants.CLAIM_REJECTED);
        claimMapper.updateById(claim);

        // WebSocket通知claimant（认领人）
        String message = String.format(
                "{\"type\":\"claim\",\"action\":\"rejected\",\"claimId\":%d}", claimId);
        WebSocketServer.sendMessage(claim.getClaimantId(), message);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result complete(Long userId, Long claimId) {
        Claim claim = claimMapper.selectById(claimId);
        if (claim == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "认领记录不存在");
        }
        if (!claim.getClaimantId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权操作此认领记录");
        }
        if (claim.getStatus() != Constants.CLAIM_APPROVED) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "认领申请尚未通过审核，无法完成");
        }

        claim.setStatus(Constants.CLAIM_COMPLETED);
        claimMapper.updateById(claim);

        // 同时更新lost_item和found_item状态为已关闭
        if (claim.getLostItemId() != null) {
            LostItem lostItem = lostItemMapper.selectById(claim.getLostItemId());
            if (lostItem != null) {
                lostItem.setStatus(Constants.STATUS_CLOSED);
                lostItemMapper.updateById(lostItem);
            }
        }
        if (claim.getFoundItemId() != null) {
            FoundItem foundItem = foundItemMapper.selectById(claim.getFoundItemId());
            if (foundItem != null) {
                foundItem.setStatus(Constants.STATUS_CLOSED);
                foundItemMapper.updateById(foundItem);
            }
        }

        // 认领完成奖励信用积分（认领人和拾到物品发布人均奖励）
        try {
            creditService.addCredit(userId, creditService.getClaimSuccessScore(),
                    "认领完成奖励", claimId);
        } catch (Exception e) {
            // 积分奖励失败不影响主流程
        }
        try {
            creditService.addCredit(claim.getRespondentId(), creditService.getClaimSuccessScore(),
                    "认领完成奖励", claimId);
        } catch (Exception e) {
            // 积分奖励失败不影响主流程
        }

        return Result.success();
    }

    @Override
    public Result<List<Claim>> myList(Long userId) {
        QueryWrapper<Claim> wrapper = new QueryWrapper<>();
        wrapper.eq("claimant_id", userId).or().eq("respondent_id", userId);
        wrapper.orderByDesc("create_time");
        List<Claim> list = claimMapper.selectList(wrapper);
        return Result.success(list);
    }
}

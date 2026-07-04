package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.exception.BizException;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.result.ResultCode;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.entity.UserCreditLog;
import com.campus.lostfound.mapper.UserCreditLogMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.service.CreditService;
import com.campus.lostfound.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 信用积分服务实现类
 */
@Slf4j
@Service
public class CreditServiceImpl implements CreditService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserCreditLogMapper userCreditLogMapper;

    @Autowired
    private MessageService messageService;

    /** 发布拾到物品奖励积分 */
    @Value("${credit.rules.publish-found}")
    private int publishFoundScore;

    /** 认领成功奖励积分 */
    @Value("${credit.rules.claim-success}")
    private int claimSuccessScore;

    /** 恶意行为扣减积分 */
    @Value("${credit.rules.malicious}")
    private int maliciousScore;

    @Override
    public Result<Map<String, Object>> getMyCredit(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }

        // 计算排名：信用积分高于当前用户的用户数 + 1
        QueryWrapper<User> rankWrapper = new QueryWrapper<>();
        rankWrapper.gt("credit_score", user.getCreditScore());
        Long higherCount = userMapper.selectCount(rankWrapper);
        Long totalUsers = userMapper.selectCount(null);

        Map<String, Object> data = new HashMap<>(8);
        data.put("creditScore", user.getCreditScore());
        data.put("rank", higherCount.intValue() + 1);
        data.put("totalUsers", totalUsers);
        // 击败百分比
        double beatPercent = totalUsers > 0
                ? (double) (totalUsers - higherCount - 1) / totalUsers * 100
                : 0.0;
        data.put("beatPercent", Math.round(beatPercent * 100) / 100.0);
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        return Result.success(data);
    }

    @Override
    public Result listCreditLogs(Long userId, int page, int size) {
        Page<UserCreditLog> pageParam = new Page<>(page, size);
        QueryWrapper<UserCreditLog> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("create_time");
        Page<UserCreditLog> result = userCreditLogMapper.selectPage(pageParam, wrapper);

        Map<String, Object> data = new HashMap<>(8);
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("size", size);
        data.put("totalPages", result.getPages());
        return Result.success(data);
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Result<Boolean> addCredit(Long userId, int score, String reason, Long refId) {
        if (score <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "增加积分必须为正数");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }

        int currentScore = user.getCreditScore() == null ? 0 : user.getCreditScore();
        int afterScore = currentScore + score;
        user.setCreditScore(afterScore);
        userMapper.updateById(user);

        // 记录积分变动日志
        UserCreditLog creditLog = new UserCreditLog();
        creditLog.setUserId(userId);
        creditLog.setChangeScore(score);
        creditLog.setAfterScore(afterScore);
        creditLog.setReason(reason);
        creditLog.setRefId(refId);
        userCreditLogMapper.insert(creditLog);

        // 站内消息通知
        try {
            messageService.sendMessage(userId, 0L, Constants.MSG_TYPE_CREDIT,
                    "信用积分变动通知",
                    "您的信用积分增加 " + score + " 分（原因：" + reason + "），当前积分：" + afterScore,
                    refId);
        } catch (Exception e) {
            log.warn("发送积分变动消息失败, userId={}, 错误: {}", userId, e.getMessage());
        }
        return Result.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> deductCredit(Long userId, int score, String reason, Long refId) {
        if (score <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "扣减积分必须为正数");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }

        int currentScore = user.getCreditScore() == null ? 0 : user.getCreditScore();
        // 确保积分不低于0
        int actualDeduct = Math.min(score, currentScore);
        int afterScore = currentScore - actualDeduct;
        user.setCreditScore(afterScore);
        userMapper.updateById(user);

        // 记录积分变动日志（扣减记为负值）
        UserCreditLog creditLog = new UserCreditLog();
        creditLog.setUserId(userId);
        creditLog.setChangeScore(-actualDeduct);
        creditLog.setAfterScore(afterScore);
        creditLog.setReason(reason);
        creditLog.setRefId(refId);
        userCreditLogMapper.insert(creditLog);

        // 站内消息通知
        try {
            messageService.sendMessage(userId, 0L, Constants.MSG_TYPE_CREDIT,
                    "信用积分变动通知",
                    "您的信用积分扣减 " + actualDeduct + " 分（原因：" + reason + "），当前积分：" + afterScore,
                    refId);
        } catch (Exception e) {
            log.warn("发送积分变动消息失败, userId={}, 错误: {}", userId, e.getMessage());
        }
        return Result.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> adminDeduct(Long userId, int score, String reason, Long operatorId) {
        // 校验操作者是否为管理员
        User operator = userMapper.selectById(operatorId);
        if (operator == null || operator.getRole() == null || operator.getRole() != Constants.ROLE_ADMIN) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权限执行此操作");
        }
        return deductCredit(userId, score, reason, null);
    }

    /**
     * 获取发布拾到物品奖励积分（供其他服务调用）
     */
    public int getPublishFoundScore() {
        return publishFoundScore;
    }

    /**
     * 获取认领成功奖励积分（供其他服务调用）
     */
    public int getClaimSuccessScore() {
        return claimSuccessScore;
    }

    /**
     * 获取恶意行为扣减积分（供其他服务调用）
     */
    public int getMaliciousScore() {
        return maliciousScore;
    }
}

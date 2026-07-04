package com.campus.lostfound.service;

import com.campus.lostfound.common.result.Result;

import java.util.Map;

/**
 * 信用积分服务接口
 * 管理用户信用积分的查询、增减及日志记录
 */
public interface CreditService {

    /**
     * 获取当前用户的信用积分信息（含排名）
     *
     * @param userId 用户ID
     * @return 积分详情Map
     */
    Result<Map<String, Object>> getMyCredit(Long userId);

    /**
     * 分页查询当前用户的积分变动日志
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页条数
     * @return 分页日志列表
     */
    Result listCreditLogs(Long userId, int page, int size);

    /**
     * 增加信用积分
     *
     * @param userId 用户ID
     * @param score  积分值（正数）
     * @param reason 变动原因
     * @param refId  关联业务ID
     * @return 是否操作成功
     */
    Result<Boolean> addCredit(Long userId, int score, String reason, Long refId);

    /**
     * 扣减信用积分（不低于0）
     *
     * @param userId 用户ID
     * @param score  积分值（正数，表示扣减量）
     * @param reason 变动原因
     * @param refId  关联业务ID
     * @return 是否操作成功
     */
    Result<Boolean> deductCredit(Long userId, int score, String reason, Long refId);

    /**
     * 管理员扣减用户信用积分
     *
     * @param userId     被扣减用户ID
     * @param score      积分值（正数，表示扣减量）
     * @param reason     变动原因
     * @param operatorId 操作者（管理员）ID
     * @return 是否操作成功
     */
    Result<Boolean> adminDeduct(Long userId, int score, String reason, Long operatorId);

    /**
     * 获取发布拾到物品奖励积分（供其他服务调用）
     *
     * @return 奖励积分值
     */
    int getPublishFoundScore();

    /**
     * 获取认领成功奖励积分（供其他服务调用）
     *
     * @return 奖励积分值
     */
    int getClaimSuccessScore();
}

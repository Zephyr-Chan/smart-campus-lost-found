package com.campus.lostfound.service;

import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.entity.OperationLog;

import java.util.List;

/**
 * 操作日志服务接口
 */
public interface OperationLogService {

    /**
     * 操作日志列表（管理员）
     *
     * @param keyword 搜索关键词（操作描述/用户名）
     * @return 日志列表
     */
    Result<List<OperationLog>> getList(String keyword);

    /**
     * 记录操作日志
     *
     * @param userId    用户ID
     * @param username  用户名
     * @param operation 操作描述
     * @param method    方法名
     * @param params    请求参数
     * @param ip        请求IP
     */
    void log(Long userId, String username, String operation, String method, String params, String ip);
}

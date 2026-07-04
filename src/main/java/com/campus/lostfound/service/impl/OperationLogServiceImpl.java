package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.entity.OperationLog;
import com.campus.lostfound.mapper.OperationLogMapper;
import com.campus.lostfound.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志服务实现类
 */
@Service
public class OperationLogServiceImpl implements OperationLogService {

    private static final Logger logger = LoggerFactory.getLogger(OperationLogServiceImpl.class);

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Override
    public Result<List<OperationLog>> getList(String keyword) {
        QueryWrapper<OperationLog> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like("operation", keyword)
                    .or().like("username", keyword);
        }
        wrapper.orderByDesc("create_time");
        wrapper.last("LIMIT 200");
        List<OperationLog> list = operationLogMapper.selectList(wrapper);
        return Result.success(list);
    }

    @Override
    @Async
    public void log(Long userId, String username, String operation, String method, String params, String ip) {
        try {
            OperationLog log = new OperationLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setOperation(operation);
            log.setMethod(method);
            log.setParams(params);
            log.setIp(ip);
            operationLogMapper.insert(log);
        } catch (Exception e) {
            logger.warn("记录操作日志失败: {}", e.getMessage());
        }
    }
}

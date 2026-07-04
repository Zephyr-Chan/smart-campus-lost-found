package com.campus.lostfound.controller;

import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志控制器（管理员）
 */
@RestController
@RequestMapping("/api/log")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 操作日志列表
     */
    @GetMapping("/list")
    public Result list(@RequestParam(required = false) String keyword) {
        return operationLogService.getList(keyword);
    }
}

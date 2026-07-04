package com.campus.lostfound.controller;

import com.campus.lostfound.common.base.BaseController;
import com.campus.lostfound.common.exception.BizException;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.result.ResultCode;
import com.campus.lostfound.service.CreditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 信用积分控制器
 */
@RestController
@RequestMapping("/api/credit")
public class CreditController extends BaseController {

    @Autowired
    private CreditService creditService;

    /**
     * 获取我的信用积分信息（含排名）
     */
    @GetMapping("/my")
    public Result my(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return creditService.getMyCredit(userId);
    }

    /**
     * 分页查询我的积分变动日志
     */
    @GetMapping("/log")
    public Result log(HttpServletRequest request,
                      @RequestParam(defaultValue = "1") int page,
                      @RequestParam(defaultValue = "10") int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 50) size = 10;
        Long userId = requireUserId(request);
        return creditService.listCreditLogs(userId, page, size);
    }

    /**
     * 管理员扣减用户信用积分
     */
    @PutMapping("/deduct")
    public Result deduct(HttpServletRequest request,
                         @RequestParam Long userId,
                         @RequestParam int score,
                         @RequestParam String reason) {
        Long operatorId = requireUserId(request);
        if (score <= 0 || score > 1000) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "扣分值必须为 1~1000 之间的正数");
        }
        if (userId == null || userId <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "用户ID非法");
        }
        return creditService.adminDeduct(userId, score, reason, operatorId);
    }
}

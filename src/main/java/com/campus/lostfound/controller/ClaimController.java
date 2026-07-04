package com.campus.lostfound.controller;

import com.campus.lostfound.common.exception.BizException;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.result.ResultCode;
import com.campus.lostfound.common.utils.JwtUtil;
import com.campus.lostfound.service.ClaimService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 认领控制器
 */
@RestController
@RequestMapping("/api/claim")
public class ClaimController {

    @Autowired
    private ClaimService claimService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 创建认领申请
     */
    @PostMapping
    public Result create(HttpServletRequest request,
                         @RequestParam Long lostItemId,
                         @RequestParam Long foundItemId,
                         @RequestParam(required = false) Double matchScore,
                         @RequestParam(required = false) String remark) {
        Long claimantId = getCurrentUserId(request);
        return claimService.create(claimantId, lostItemId, foundItemId, matchScore, remark);
    }

    /**
     * 审核通过
     */
    @PutMapping("/approve/{id}")
    public Result approve(HttpServletRequest request, @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "ID非法");
        }
        Long userId = getCurrentUserId(request);
        return claimService.approve(userId, id);
    }

    /**
     * 审核拒绝
     */
    @PutMapping("/reject/{id}")
    public Result reject(HttpServletRequest request, @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "ID非法");
        }
        Long userId = getCurrentUserId(request);
        return claimService.reject(userId, id);
    }

    /**
     * 确认完成
     */
    @PutMapping("/complete/{id}")
    public Result complete(HttpServletRequest request, @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "ID非法");
        }
        Long userId = getCurrentUserId(request);
        return claimService.complete(userId, id);
    }

    /**
     * 我的认领列表
     */
    @GetMapping("/my")
    public Result myList(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return claimService.myList(userId);
    }

    /**
     * 从请求头获取当前登录用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || token.isEmpty()) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        if (!jwtUtil.validateToken(token)) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return jwtUtil.getUserIdFromToken(token);
    }
}

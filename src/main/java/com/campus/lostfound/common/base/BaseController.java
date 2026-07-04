package com.campus.lostfound.common.base;

import com.campus.lostfound.common.exception.BizException;
import com.campus.lostfound.common.result.ResultCode;

import javax.servlet.http.HttpServletRequest;

/**
 * 基础控制器 - 提供统一的用户身份获取能力
 * 所有 Controller 继承此类，消除重复的 JWT 解析代码
 */
public abstract class BaseController {

    /**
     * 从请求属性中获取当前登录用户ID
     * LoginInterceptor 在 preHandle 中将 userId 存入 request.setAttribute("currentUserId", userId)
     *
     * @param request HTTP 请求
     * @return 用户ID，未登录返回 null
     */
    protected Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId == null) {
            return null;
        }
        return (Long) userId;
    }

    /**
     * 获取当前登录用户ID，未登录则抛出异常
     *
     * @param request HTTP 请求
     * @return 用户ID
     * @throws BizException 未登录时抛出 401 异常
     */
    protected Long requireUserId(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        return userId;
    }
}

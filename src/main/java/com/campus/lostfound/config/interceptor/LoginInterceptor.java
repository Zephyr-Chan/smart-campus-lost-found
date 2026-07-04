package com.campus.lostfound.config.interceptor;

import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.utils.JwtUtil;
import com.campus.lostfound.common.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器 - 校验JWT Token
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行OPTIONS预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("未携带Token, uri={}", request.getRequestURI());
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录或Token缺失\"}");
            return false;
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            log.warn("Token无效, uri={}", request.getRequestURI());
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"Token无效或已过期\"}");
            return false;
        }
        Long userId = jwtUtil.getUserIdFromToken(token);

        // M4修复：校验Redis中Token是否仍然有效（支持主动注销/封禁）
        // 仅在Redis可用时执行检查，Redis不可用时降级为JWT-only模式
        if (redisUtil.isAvailable()) {
            String cachedToken = redisUtil.get(Constants.TOKEN_PREFIX + userId);
            if (cachedToken == null || !cachedToken.equals(token)) {
                log.warn("Token已被注销或失效, userId={}, uri={}", userId, request.getRequestURI());
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"msg\":\"登录已失效，请重新登录\"}");
                return false;
            }
        } else {
            log.debug("Redis不可用，跳过Token注销检查, userId={}", userId);
        }

        request.setAttribute("currentUserId", userId);
        log.debug("登录校验通过, userId={}, uri={}", userId, request.getRequestURI());
        return true;
    }
}

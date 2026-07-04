package com.campus.lostfound.config.interceptor;

import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 角色权限拦截器 - 校验管理员权限
 */
@Slf4j
@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Long userId = (Long) request.getAttribute("currentUserId");
        if (userId == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录\"}");
            return false;
        }
        User user = userMapper.selectById(userId);
        if (user == null || user.getRole() == null || user.getRole() != Constants.ROLE_ADMIN) {
            log.warn("非管理员访问受限资源, userId={}, uri={}", userId, request.getRequestURI());
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"msg\":\"无管理员权限\"}");
            return false;
        }
        return true;
    }
}

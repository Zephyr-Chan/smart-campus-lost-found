package com.campus.lostfound.config;

import com.campus.lostfound.entity.User;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 操作日志AOP切面
 * 拦截Controller层写操作（POST/PUT/DELETE），自动记录操作日志
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(OperationLogAspect.class);

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 拦截所有Controller层的public方法
     */
    @Pointcut("execution(public * com.campus.lostfound.controller..*.*(..))")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取请求信息
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = attributes.getRequest();
        String method = request.getMethod();

        // 只记录写操作（POST/PUT/DELETE）
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method) && !"DELETE".equalsIgnoreCase(method)) {
            return joinPoint.proceed();
        }

        // 排除登录/注册接口（不记录密码）
        String uri = request.getRequestURI();
        if (uri.contains("/login") || uri.contains("/register")) {
            return joinPoint.proceed();
        }

        // 提前获取用户信息（proceed前，确保异常时也可记录）
        Long userId = (Long) request.getAttribute("currentUserId");
        String username = "匿名用户";
        if (userId != null) {
            User user = userMapper.selectById(userId);
            if (user != null) {
                username = user.getUsername();
            }
        }

        String operation = buildOperation(method, uri);
        String methodDesc = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        String params = buildParams(joinPoint);
        String ip = getClientIp(request);

        Object result;
        boolean success = true;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            success = false;
            throw e;
        } finally {
            // 异步记录日志（成功和失败均记录）
            try {
                String logOperation = success ? operation : operation + "(失败)";
                operationLogService.log(userId, username, logOperation, methodDesc, params, ip);
            } catch (Exception e) {
                logger.warn("记录操作日志失败: {}", e.getMessage());
            }
        }

        return result;
    }

    /**
     * 根据HTTP方法和URI构建操作描述
     */
    private String buildOperation(String httpMethod, String uri) {
        String action;
        switch (httpMethod.toUpperCase()) {
            case "POST": action = "新增"; break;
            case "PUT": action = "更新"; break;
            case "DELETE": action = "删除"; break;
            default: action = httpMethod;
        }
        // 简化URI
        String desc = uri;
        if (uri.contains("/user/")) desc = "用户操作";
        else if (uri.contains("/lost")) desc = "失物操作";
        else if (uri.contains("/found")) desc = "招领操作";
        else if (uri.contains("/claim")) desc = "认领操作";
        else if (uri.contains("/announcement")) desc = "公告操作";
        else if (uri.contains("/match")) desc = "智能匹配";
        else if (uri.contains("/avatar")) desc = "头像上传";
        else if (uri.contains("/status")) desc = "状态变更";

        return action + "-" + desc;
    }

    /**
     * 构建参数摘要（截断防过长）
     */
    private String buildParams(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (Object arg : args) {
                if (arg == null) continue;
                // 跳过HttpServletRequest/Response等非数据参数
                if (arg instanceof HttpServletRequest) continue;
                String str = arg.toString();
                if (str.length() > 200) {
                    str = str.substring(0, 200) + "...";
                }
                sb.append(str).append("; ");
            }
            return sb.length() > 0 ? sb.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

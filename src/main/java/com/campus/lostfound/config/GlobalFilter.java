package com.campus.lostfound.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 全局过滤器
 * 1. 字符编码统一UTF-8
 * 2. CORS跨域支持
 * 3. 请求日志记录
 * 4. 从Cookie中提取Token到Header（兼容Cookie认证）
 */
@WebFilter(filterName = "globalFilter", urlPatterns = "/*")
public class GlobalFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(GlobalFilter.class);

    private static final String TOKEN_COOKIE_NAME = "lost_found_token";

    @Override
    public void init(FilterConfig filterConfig) {
        logger.info("GlobalFilter 初始化完成");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 1. 设置字符编码
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        // 仅对API路径设置JSON内容类型，JSP/静态资源由各自机制处理
        String uri = httpRequest.getRequestURI();
        if (uri.startsWith("/api/")) {
            response.setContentType("application/json;charset=UTF-8");
        }

        // 2. CORS跨域（允许前端分离开发，支持 Cookie 携带）
        String origin = httpRequest.getHeader("Origin");
        if (origin != null && !origin.isEmpty()) {
            httpResponse.setHeader("Access-Control-Allow-Origin", origin);
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
        }
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        httpResponse.setHeader("Access-Control-Allow-Headers",
                "Content-Type, Authorization, X-Requested-With");
        httpResponse.setHeader("Access-Control-Max-Age", "3600");

        // 放行OPTIONS预检请求
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // 3. 从Cookie中提取Token，若无Authorization Header则补充
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || authHeader.isEmpty()) {
            // 尝试从Cookie中获取Token
            if (httpRequest.getCookies() != null) {
                for (javax.servlet.http.Cookie cookie : httpRequest.getCookies()) {
                    if (TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                        String token = cookie.getValue();
                        if (token != null && !token.isEmpty()) {
                            // 将Cookie中的Token包装为Bearer Token放入Header
                            javax.servlet.http.HttpServletRequestWrapper wrapper =
                                    new TokenRequestWrapper(httpRequest, "Bearer " + token);
                            chain.doFilter(wrapper, response);
                            return;
                        }
                    }
                }
            }
        }

        // 4. 请求日志
        String method = httpRequest.getMethod();
        String ip = getClientIp(httpRequest);
        logger.debug("请求: {} {} IP: {}", method, uri, ip);

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("GlobalFilter 销毁");
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
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 请求包装器 - 用于覆盖getHeader，注入Cookie中的Token
     */
    private static class TokenRequestWrapper extends javax.servlet.http.HttpServletRequestWrapper {
        private final String authHeaderValue;

        public TokenRequestWrapper(HttpServletRequest request, String authHeaderValue) {
            super(request);
            this.authHeaderValue = authHeaderValue;
        }

        @Override
        public String getHeader(String name) {
            if ("Authorization".equalsIgnoreCase(name)) {
                return authHeaderValue;
            }
            return super.getHeader(name);
        }

        @Override
        public java.util.Enumeration<String> getHeaders(String name) {
            if ("Authorization".equalsIgnoreCase(name)) {
                return java.util.Collections.enumeration(
                        java.util.Collections.singletonList(authHeaderValue));
            }
            return super.getHeaders(name);
        }

        @Override
        public java.util.Enumeration<String> getHeaderNames() {
            java.util.Enumeration<String> names = super.getHeaderNames();
            java.util.Set<String> set = new java.util.HashSet<>();
            if (names != null) {
                while (names.hasMoreElements()) {
                    set.add(names.nextElement());
                }
            }
            set.add("Authorization");
            return java.util.Collections.enumeration(set);
        }
    }
}

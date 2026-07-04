package com.campus.lostfound.config;

import com.campus.lostfound.config.interceptor.LoginInterceptor;
import com.campus.lostfound.config.interceptor.RoleInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类 - 静态资源映射 + 拦截器注册
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-path}")
    private String uploadPath;

    @Value("${file.access-path}")
    private String accessPath;

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Autowired
    private RoleInterceptor roleInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器 - 仅拦截API请求，JSP页面路由不拦截
        // 公开GET接口放行（浏览物品列表/详情、看板数据、匹配结果、推荐、搜索），写操作需要登录
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/login", "/api/user/register",
                        "/api/announcement/list", "/api/announcement/get/**",
                        "/api/lost/list", "/api/lost/get/**",
                        "/api/found/list", "/api/found/get/**",
                        "/api/dashboard/**",
                        "/api/recommend/guest",
                        "/api/recommend/similar/**",
                        "/api/rank/list",
                        "/api/search",
                        "/api/search/suggest",
                        "/api/comment/list",
                        "/api/ai/health"
                );

        // 角色拦截器 - 拦截管理员写操作接口（公告增删改、用户管理、日志查看）
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns(
                        "/api/announcement",
                        "/api/announcement/**",
                        "/api/user/list",
                        "/api/user/status/**",
                        "/api/log/**",
                        "/api/credit/deduct"
                )
                .excludePathPatterns(
                        "/api/announcement/list",
                        "/api/announcement/get/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射上传文件访问路径到本地磁盘目录（转为绝对路径，兼容相对路径配置）
        String absPath = java.nio.file.Paths.get(uploadPath).toAbsolutePath().toString().replace('\\', '/');
        if (!absPath.endsWith("/")) {
            absPath = absPath + "/";
        }
        registry.addResourceHandler(accessPath + "**")
                .addResourceLocations("file:" + absPath);
    }
}

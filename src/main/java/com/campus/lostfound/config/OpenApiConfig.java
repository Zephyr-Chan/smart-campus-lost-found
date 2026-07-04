package com.campus.lostfound.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 3 文档配置
 * <p>
 * 基于 springdoc-openapi 1.7.0，提供统一的接口文档与在线调试入口。
 * 访问路径：
 * <ul>
 *   <li>接口文档 JSON：{@code /v3/api-docs}</li>
 *   <li>Swagger UI：{@code /swagger-ui/index.html}</li>
 * </ul>
 * </p>
 */
@Configuration
public class OpenApiConfig {

    /** 安全方案标识 */
    private static final String SECURITY_SCHEME_NAME = "bearer-jwt";

    /**
     * 全局 OpenAPI 元信息 + JWT Bearer 鉴权方案
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("校园失物招领系统 API")
                        .version("2.0.0")
                        .description("校园失物招领系统后端接口文档，涵盖物品发布、智能匹配、认领流程、"
                                + "消息通知、积分体系、推荐与全文检索等模块。所有需鉴权的接口请在请求头"
                                + "携带 Authorization: Bearer <JWT>。")
                        .contact(new Contact()
                                .name("Campus Lost & Found Team")
                                .email("support@campus.edu")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")
                                        .description("请输入 JWT Token，格式：Bearer <token>")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    /**
     * API 分组：仅扫描 /api/** 路径下的接口
     */
    @Bean
    public GroupedOpenApi apiGroup() {
        return GroupedOpenApi.builder()
                .group("api")
                .pathsToMatch("/api/**")
                .packagesToScan("com.campus.lostfound.controller")
                .build();
    }
}

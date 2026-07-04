package com.campus.lostfound.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 微服务配置属性
 * 绑定 application.yml 中 ai.service.* 配置项，便于统一管理与热更新
 *
 * 对应配置：
 * <pre>
 * ai:
 *   service:
 *     url: http://localhost:8001
 *     timeout: 5000
 *     enabled: true
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.service")
public class AiServiceProperties {

    /**
     * AI 服务地址
     */
    private String url = "http://localhost:8001";

    /**
     * 调用超时时间（毫秒）
     */
    private int timeout = 5000;

    /**
     * 是否启用 AI 服务（关闭后自动降级到 TF-IDF）
     */
    private boolean enabled = true;
}

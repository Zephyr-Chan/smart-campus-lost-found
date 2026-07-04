package com.campus.lostfound.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 邮件配置类
 * <p>
 * 仅当 {@code mail.notify.enabled=true} 时才装配 {@link JavaMailSender}，
 * 避免 SMTP 服务器未配置场景下出现无意义的 Bean。配置项读取自
 * {@code spring.mail.*} 属性，与 application.yml 中的 SMTP 配置保持一致。
 * </p>
 * <p>
 * 当开关关闭时，{@link com.campus.lostfound.service.impl.MailServiceImpl}
 * 通过 {@code @Autowired(required=false)} 拿到 null，并在调用时记录日志后直接返回，
 * 实现邮件通知的优雅降级。
 * </p>
 */
@Configuration
@ConditionalOnProperty(name = "mail.notify.enabled", havingValue = "true")
public class MailConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    /**
     * 自定义 JavaMailSender Bean（覆盖 Spring Boot 自动配置）
     *
     * @return 配置好的 JavaMailSender 实例
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        return sender;
    }
}

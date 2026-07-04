package com.campus.lostfound.service.impl;

import com.campus.lostfound.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;

/**
 * 邮件通知服务实现
 * <p>
 * 采用 {@code @Async} 异步发送，避免阻塞业务主流程。当
 * {@code mail.notify.enabled=false} 或 {@link JavaMailSender} 未注入时，
 * 所有方法仅记录日志后返回，实现优雅降级。
 * </p>
 */
@Slf4j
@Service
public class MailServiceImpl implements MailService {

    /**
     * 可选注入：当 {@code mail.notify.enabled=false} 时 MailConfig 不装配该 Bean
     */
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${mail.notify.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:noreply@campus.edu}")
    private String fromAddress;

    @Override
    @Async
    public void sendNotificationEmail(String toEmail, String title, String content) {
        if (!mailEnabled || mailSender == null) {
            log.warn("邮件通知未启用或 JavaMailSender 未注入，跳过发送: to={}, title={}", toEmail, title);
            return;
        }
        if (toEmail == null || toEmail.isEmpty()) {
            log.warn("收件人邮箱为空，跳过发送邮件: title={}", title);
            return;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(title);
            helper.setText(buildHtmlWrapper(title, content), true);
            mailSender.send(mimeMessage);
            log.info("通知邮件发送成功: to={}, title={}", toEmail, title);
        } catch (Exception e) {
            log.warn("通知邮件发送失败，降级跳过: to={}, title={}, 错误={}", toEmail, title, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendMatchNotification(String toEmail, String itemTitle, double score) {
        if (!mailEnabled || mailSender == null) {
            log.warn("邮件通知未启用或 JavaMailSender 未注入，跳过匹配邮件: to={}, item={}", toEmail, itemTitle);
            return;
        }
        if (toEmail == null || toEmail.isEmpty()) {
            log.warn("收件人邮箱为空，跳过发送匹配邮件: item={}", itemTitle);
            return;
        }
        try {
            String title = "【校园失物招领】匹配命中提醒";
            int percent = (int) Math.round(score * 100);
            String content = "<div style='padding:16px;font-family:Microsoft YaHei,sans-serif;'>"
                    + "<h2 style='color:#2196F3;'>匹配命中提醒</h2>"
                    + "<p>您好，您关注的物品疑似出现匹配结果：</p>"
                    + "<p style='font-size:16px;'>物品标题：<strong>" + escapeHtml(itemTitle) + "</strong></p>"
                    + "<p>匹配相似度：<strong style='color:#4CAF50;'>" + percent + "%</strong></p>"
                    + "<p>请尽快登录系统查看详情并完成认领流程。</p>"
                    + "<hr style='border:none;border-top:1px solid #eee;'/>"
                    + "<p style='color:#999;font-size:12px;'>此邮件由系统自动发送，请勿直接回复。</p>"
                    + "</div>";
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(title);
            helper.setText(content, true);
            mailSender.send(mimeMessage);
            log.info("匹配邮件发送成功: to={}, item={}, score={}", toEmail, itemTitle, percent);
        } catch (Exception e) {
            log.warn("匹配邮件发送失败，降级跳过: to={}, item={}, 错误={}", toEmail, itemTitle, e.getMessage());
        }
    }

    /**
     * 将纯文本内容包装为统一风格的 HTML 邮件模板
     */
    private String buildHtmlWrapper(String title, String content) {
        return "<div style='padding:16px;font-family:Microsoft YaHei,sans-serif;'>"
                + "<h2 style='color:#2196F3;'>" + escapeHtml(title) + "</h2>"
                + "<div style='font-size:14px;line-height:1.6;'>" + content + "</div>"
                + "<hr style='border:none;border-top:1px solid #eee;'/>"
                + "<p style='color:#999;font-size:12px;'>此邮件由校园失物招领系统自动发送，请勿直接回复。</p>"
                + "</div>";
    }

    /**
     * 简单的 HTML 转义，避免用户输入破坏邮件结构
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

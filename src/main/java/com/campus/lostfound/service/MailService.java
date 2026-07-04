package com.campus.lostfound.service;

/**
 * 邮件通知服务接口
 * <p>
 * 封装校园失物招领系统的邮件通知能力，所有实现应支持在邮件服务
 * 未启用或不可用时优雅降级（仅记录日志，不抛出异常）。
 * </p>
 */
public interface MailService {

    /**
     * 发送通用通知邮件（HTML 格式）
     *
     * @param toEmail 收件人邮箱
     * @param title   邮件标题
     * @param content 邮件正文（支持 HTML）
     */
    void sendNotificationEmail(String toEmail, String title, String content);

    /**
     * 发送匹配命中通知邮件，包含匹配分数
     *
     * @param toEmail   收件人邮箱
     * @param itemTitle 命中物品标题
     * @param score     匹配分数（0~1）
     */
    void sendMatchNotification(String toEmail, String itemTitle, double score);
}

package com.campus.lostfound.mq;

import com.campus.lostfound.config.RabbitMqConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 消息生产者
 * <p>
 * 统一封装失物招领系统三类异步消息的发送：匹配、通知、ES 同步。
 * 所有发送方法均包裹 try-catch，当 RabbitTemplate 未注入或 broker 不可达时，
 * 仅记录警告日志并继续执行，调用方可平滑降级为同步处理流程。
 * </p>
 */
@Slf4j
@Component
public class MessageProducer {

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送匹配计算消息
     *
     * @param itemId   物品ID
     * @param itemType 物品类型（lost / found）
     * @param action   动作（如 create / update / delete）
     */
    public void sendMatchingMessage(Long itemId, String itemType, String action) {
        if (rabbitTemplate == null) {
            log.warn("RabbitTemplate 未注入，匹配消息降级同步处理: itemId={}, itemType={}, action={}",
                    itemId, itemType, action);
            return;
        }
        try {
            Map<String, Object> message = new HashMap<>(8);
            message.put("itemId", itemId);
            message.put("itemType", itemType);
            message.put("action", action);
            message.put("timestamp", System.currentTimeMillis());
            String routingKey = "matching." + action;
            rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, routingKey, message);
            log.info("发送匹配消息成功: itemId={}, action={}, routingKey={}", itemId, action, routingKey);
        } catch (Exception e) {
            log.warn("发送匹配消息失败，降级同步处理: itemId={}, action={}, 错误: {}",
                    itemId, action, e.getMessage());
        }
    }

    /**
     * 发送站内通知消息（由 NotificationConsumer 消费并持久化）
     *
     * @param receiverId 接收者ID
     * @param type       消息类型（match / claim / credit / system）
     * @param title      消息标题
     * @param content    消息内容
     * @param refId      关联业务ID
     */
    public void sendNotificationMessage(Long receiverId, String type, String title, String content, Long refId) {
        if (rabbitTemplate == null) {
            log.warn("RabbitTemplate 未注入，通知消息降级同步处理: receiverId={}, type={}", receiverId, type);
            return;
        }
        try {
            Map<String, Object> message = new HashMap<>(8);
            message.put("receiverId", receiverId);
            message.put("type", type);
            message.put("title", title);
            message.put("content", content);
            message.put("refId", refId);
            message.put("timestamp", System.currentTimeMillis());
            String routingKey = "notification." + type;
            rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, routingKey, message);
            log.info("发送通知消息成功: receiverId={}, type={}, routingKey={}", receiverId, type, routingKey);
        } catch (Exception e) {
            log.warn("发送通知消息失败，降级同步处理: receiverId={}, type={}, 错误: {}",
                    receiverId, type, e.getMessage());
        }
    }

    /**
     * 发送 Elasticsearch 数据同步消息
     *
     * @param itemId   物品ID
     * @param itemType 物品类型（lost / found）
     * @param action   动作（如 create / update / delete）
     */
    public void sendEsSyncMessage(Long itemId, String itemType, String action) {
        if (rabbitTemplate == null) {
            log.warn("RabbitTemplate 未注入，ES 同步消息降级同步处理: itemId={}, action={}", itemId, action);
            return;
        }
        try {
            Map<String, Object> message = new HashMap<>(8);
            message.put("itemId", itemId);
            message.put("itemType", itemType);
            message.put("action", action);
            message.put("timestamp", System.currentTimeMillis());
            String routingKey = "es." + action;
            rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, routingKey, message);
            log.info("发送 ES 同步消息成功: itemId={}, action={}, routingKey={}", itemId, action, routingKey);
        } catch (Exception e) {
            log.warn("发送 ES 同步消息失败，降级同步处理: itemId={}, action={}, 错误: {}",
                    itemId, action, e.getMessage());
        }
    }
}

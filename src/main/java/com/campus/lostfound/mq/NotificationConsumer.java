package com.campus.lostfound.mq;

import com.campus.lostfound.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 站内通知消息消费者
 * <p>
 * 监听 {@code notification.queue}，解析 JSON 消息体后调用
 * {@link MessageService#sendMessage} 持久化站内消息并触发 WebSocket 实时推送。
 * </p>
 * <p>
 * 采用手动 ACK 模式：
 * <ul>
 *   <li>消费成功 -> {@code Channel.basicAck} 确认消息</li>
 *   <li>消费失败 -> {@code Channel.basicNack(requeue=false)} 投递至死信队列</li>
 * </ul>
 * 所有异常均在方法内部捕获消化，不向上抛出，避免触发重试拦截器导致重复消费。
 * </p>
 */
@Slf4j
@Component
public class NotificationConsumer {

    @Autowired
    private MessageService messageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "notification.queue")
    public void handleNotification(Message message, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        if (message == null) {
            ackQuietly(channel, deliveryTag);
            return;
        }
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("收到通知消息: {}", body);

            Map<?, ?> data = objectMapper.readValue(body, Map.class);

            Long receiverId = toLong(data.get("receiverId"));
            String type = asString(data.get("type"));
            String title = asString(data.get("title"));
            String content = asString(data.get("content"));
            Long refId = toLong(data.get("refId"));

            if (receiverId == null) {
                log.warn("通知消息缺少 receiverId，丢弃至死信队列: {}", body);
                nackQuietly(channel, deliveryTag);
                return;
            }

            // 调用站内消息服务持久化（senderId=0 表示系统消息）
            messageService.sendMessage(receiverId, 0L, type, title, content, refId);
            log.info("通知消息消费成功: receiverId={}, type={}", receiverId, type);

            ackQuietly(channel, deliveryTag);
        } catch (Exception e) {
            log.error("通知消息消费失败，投递至死信队列: 错误={}", e.getMessage(), e);
            nackQuietly(channel, deliveryTag);
        }
    }

    /**
     * 静默 ACK，吞掉 IO 异常
     */
    private void ackQuietly(Channel channel, long deliveryTag) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            log.error("消息 ACK 失败: deliveryTag={}, 错误={}", deliveryTag, e.getMessage());
        }
    }

    /**
     * 静默 NACK（不重入队列，路由至死信队列），吞掉 IO 异常
     */
    private void nackQuietly(Channel channel, long deliveryTag) {
        try {
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException e) {
            log.error("消息 NACK 失败: deliveryTag={}, 错误={}", deliveryTag, e.getMessage());
        }
    }

    /**
     * 安全转换为 Long，兼容 JSON 中 Integer / Number / String 形式
     */
    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}

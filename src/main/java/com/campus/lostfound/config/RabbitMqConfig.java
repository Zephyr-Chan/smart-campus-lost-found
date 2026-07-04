package com.campus.lostfound.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 * <p>
 * 定义失物招领系统的核心 Topic Exchange、三个业务队列（匹配 / 通知 / ES 同步），
 * 以及死信交换机与死信队列，用于消费失败的消息兜底。
 * </p>
 * <p>
 * 当 RabbitMQ 服务不可用时，{@code spring-boot-starter-amqp} 的自动配置依然会
 * 创建连接工厂与 RabbitTemplate（惰性连接），应用可正常启动；
 * 消息发送方在发送失败时通过 try-catch 降级为同步处理。
 * </p>
 */
@Configuration
public class RabbitMqConfig {

    // ===== 主交换机 =====
    public static final String EXCHANGE = "lost.found.exchange";

    // ===== 业务队列 =====
    public static final String MATCHING_QUEUE = "matching.queue";
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String ES_QUEUE = "es.queue";

    // ===== 路由键 =====
    public static final String MATCHING_ROUTING_KEY = "matching.#";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.#";
    public static final String ES_ROUTING_KEY = "es.#";

    // ===== 死信交换机 / 死信队列 =====
    public static final String DLX_EXCHANGE = "lost.found.dlx.exchange";
    public static final String DLQ_QUEUE = "lost.found.dlq";
    public static final String DLQ_ROUTING_KEY = "dlq.routing";

    /**
     * 业务主交换机（Topic 类型，支持通配符路由）
     */
    @Bean
    public TopicExchange lostFoundExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    /**
     * 死信交换机（接收消费失败的消息）
     */
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    /**
     * 匹配队列 - 绑定死信交换机，消费失败的消息进入 DLQ
     */
    @Bean
    public Queue matchingQueue() {
        return QueueBuilder.durable(MATCHING_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * 通知队列 - 绑定死信交换机
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * ES 同步队列 - 绑定死信交换机
     */
    @Bean
    public Queue esQueue() {
        return QueueBuilder.durable(ES_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * 死信队列 - 兜底存储所有消费失败的消息
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    /**
     * 匹配队列绑定到主交换机
     */
    @Bean
    public Binding matchingBinding(Queue matchingQueue, TopicExchange lostFoundExchange) {
        return BindingBuilder.bind(matchingQueue).to(lostFoundExchange).with(MATCHING_ROUTING_KEY);
    }

    /**
     * 通知队列绑定到主交换机
     */
    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange lostFoundExchange) {
        return BindingBuilder.bind(notificationQueue).to(lostFoundExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    /**
     * ES 同步队列绑定到主交换机
     */
    @Bean
    public Binding esBinding(Queue esQueue, TopicExchange lostFoundExchange) {
        return BindingBuilder.bind(esQueue).to(lostFoundExchange).with(ES_ROUTING_KEY);
    }

    /**
     * 死信队列绑定到死信交换机
     */
    @Bean
    public Binding dlqBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DLQ_ROUTING_KEY);
    }

    /**
     * JSON 消息转换器
     * <p>
     * 定义为 Bean 后，Spring Boot 自动配置的 {@link org.springframework.amqp.rabbit.core.RabbitTemplate}
     * 会通过 {@code messageConverter.getIfUnique()} 注入并使用该转换器，
     * 使消息体以 JSON 格式在队列中传输。
     * </p>
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

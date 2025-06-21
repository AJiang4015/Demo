package com.ajiang.common.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 * 配置操作日志相关的队列、交换机和绑定关系
 * 实现消息可靠性机制：持久化、死信队列、延迟重试
 */
@Configuration
public class RabbitMQConfig {

    // ==================== 操作日志相关配置 ====================
    // 操作日志队列名称
    public static final String OPERATION_LOG_QUEUE = "operation.log.queue";
    // 操作日志交换机名称
    public static final String OPERATION_LOG_EXCHANGE = "operation.log.exchange";
    // 操作日志路由键
    public static final String OPERATION_LOG_ROUTING_KEY = "operation.log";

    // ==================== 死信队列相关配置 ====================
    // 死信交换机
    public static final String DEAD_LETTER_EXCHANGE = "dlx.exchange";
    public static final String DLX_EXCHANGE = DEAD_LETTER_EXCHANGE; // 别名，保持兼容性
    // 死信队列
    public static final String DEAD_LETTER_QUEUE = "dlx.queue";
    public static final String DLX_QUEUE = DEAD_LETTER_QUEUE; // 别名，保持兼容性
    // 死信路由键
    public static final String DEAD_LETTER_ROUTING_KEY = "dlx.operation.log";
    public static final String DLX_ROUTING_KEY = DEAD_LETTER_ROUTING_KEY; // 别名，保持兼容性

    // ==================== 延迟重试队列相关配置 ====================
    // 延迟重试交换机
    public static final String RETRY_EXCHANGE = "retry.exchange";
    // 延迟重试队列
    public static final String RETRY_QUEUE = "retry.operation.log.queue";
    // 延迟重试路由键
    public static final String RETRY_ROUTING_KEY = "retry.operation.log";

    // 重试延迟时间（毫秒）
    public static final int RETRY_DELAY_MS = 30000; // 30秒
    // 最大重试次数
    public static final int MAX_RETRY_COUNT = 3;

    // ==================== 主要业务队列配置 ====================

    /**
     * 声明操作日志队列（持久化，绑定死信队列）
     */
    @Bean
    public Queue operationLogQueue() {
        return QueueBuilder
                .durable(OPERATION_LOG_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    /**
     * 声明操作日志交换机（持久化）
     */
    @Bean
    public DirectExchange operationLogExchange() {
        return ExchangeBuilder
                .directExchange(OPERATION_LOG_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 绑定操作日志队列和交换机
     */
    @Bean
    public Binding operationLogBinding() {
        return BindingBuilder
                .bind(operationLogQueue())
                .to(operationLogExchange())
                .with(OPERATION_LOG_ROUTING_KEY);
    }

    // ==================== 死信队列配置 ====================

    /**
     * 声明死信交换机
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder
                .directExchange(DEAD_LETTER_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 声明死信队列
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
                .durable(DEAD_LETTER_QUEUE)
                .build();
    }

    /**
     * 绑定死信队列和死信交换机
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DEAD_LETTER_ROUTING_KEY);
    }

    // ==================== 延迟重试队列配置 ====================

    /**
     * 声明延迟重试交换机
     */
    @Bean
    public DirectExchange retryExchange() {
        return ExchangeBuilder
                .directExchange(RETRY_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 声明延迟重试队列（TTL队列，消息过期后进入死信队列重新处理）
     */
    @Bean
    public Queue retryQueue() {
        return QueueBuilder
                .durable(RETRY_QUEUE)
                .withArgument("x-message-ttl", RETRY_DELAY_MS)
                .withArgument("x-dead-letter-exchange", OPERATION_LOG_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", OPERATION_LOG_ROUTING_KEY)
                .build();
    }

    /**
     * 绑定延迟重试队列和交换机
     */
    @Bean
    public Binding retryBinding() {
        return BindingBuilder
                .bind(retryQueue())
                .to(retryExchange())
                .with(RETRY_ROUTING_KEY);
    }
}
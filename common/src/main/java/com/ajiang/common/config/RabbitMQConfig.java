package com.ajiang.common.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 * 配置操作日志相关的队列、交换机和绑定关系
 */
@Configuration
public class RabbitMQConfig {

    // 操作日志队列名称
    public static final String OPERATION_LOG_QUEUE = "operation.log.queue";
    // 操作日志交换机名称
    public static final String OPERATION_LOG_EXCHANGE = "operation.log.exchange";
    // 操作日志路由键
    public static final String OPERATION_LOG_ROUTING_KEY = "operation.log";

    /**
     * 声明操作日志队列
     */
    @Bean
    public Queue operationLogQueue() {
        return QueueBuilder.durable(OPERATION_LOG_QUEUE).build();
    }

    /**
     * 声明操作日志交换机
     */
    @Bean
    public DirectExchange operationLogExchange() {
        return new DirectExchange(OPERATION_LOG_EXCHANGE);
    }

    /**
     * 绑定队列和交换机
     */
    @Bean
    public Binding operationLogBinding() {
        return BindingBuilder
                .bind(operationLogQueue())
                .to(operationLogExchange())
                .with(OPERATION_LOG_ROUTING_KEY);
    }
}
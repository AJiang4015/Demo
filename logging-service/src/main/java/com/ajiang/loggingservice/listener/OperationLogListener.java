package com.ajiang.loggingservice.listener;

import com.ajiang.common.config.RabbitMQConfig;
import com.ajiang.loggingservice.entity.OperationLog;
import com.ajiang.loggingservice.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 操作日志消息监听器
 * 监听RabbitMQ中的操作日志消息并落库
 * 实现消息可靠性机制：手动确认、重试机制、死信队列处理
 */
@Slf4j
@Component
public class OperationLogListener {

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 监听操作日志队列
     * 使用手动确认模式确保消息可靠性
     */
    @RabbitListener(queues = RabbitMQConfig.OPERATION_LOG_QUEUE, ackMode = "MANUAL")
    public void onMessage(String messageBody, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("接收到操作日志消息: {}", messageBody);

            // 解析JSON消息为OperationLog对象
            OperationLog operationLog = objectMapper.readValue(messageBody, OperationLog.class);

            // 保存操作日志到数据库
            operationLogService.save(operationLog);

            log.info("操作日志保存成功: logId={}, userId={}, action={}",
                    operationLog.getLogId(), operationLog.getUserId(), operationLog.getAction());

            // 手动确认消息
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("处理操作日志消息失败: {}", messageBody, e);

            // 获取重试次数
            Map<String, Object> headers = message.getMessageProperties().getHeaders();
            Integer retryCount = (Integer) headers.getOrDefault("x-retry-count", 0);

            if (retryCount < 3) {
                // 重试次数未达到上限，拒绝消息并重新入队
                log.warn("消息处理失败，准备重试，当前重试次数: {}", retryCount);
                channel.basicNack(deliveryTag, false, true);
            } else {
                // 重试次数已达上限，发送到死信队列
                log.error("消息处理失败，重试次数已达上限，发送到死信队列: {}", messageBody);
                sendToDeadLetterQueue(messageBody, e.getMessage());
                // 确认消息，避免重复处理
                channel.basicAck(deliveryTag, false);
            }
        }
    }

    /**
     * 监听死信队列
     * 处理最终失败的消息
     */
    @RabbitListener(queues = RabbitMQConfig.DLX_QUEUE, ackMode = "MANUAL")
    public void onDeadLetterMessage(String messageBody, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.warn("接收到死信队列消息: {}", messageBody);

            // 记录失败的消息到特殊的错误日志表或文件
            // 这里可以实现告警机制，通知运维人员
            logFailedMessage(messageBody, message.getMessageProperties().getHeaders());

            // 确认死信消息
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("处理死信队列消息失败: {}", messageBody, e);
            // 死信队列处理失败，直接确认避免无限循环
            channel.basicAck(deliveryTag, false);
        }
    }

    /**
     * 将失败消息发送到死信队列
     */
    private void sendToDeadLetterQueue(String messageBody, String errorReason) {
        try {
            // 添加失败原因到消息头
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DLX_EXCHANGE,
                    RabbitMQConfig.DLX_ROUTING_KEY,
                    messageBody,
                    messagePostProcessor -> {
                        messagePostProcessor.getMessageProperties().getHeaders().put("error-reason", errorReason);
                        messagePostProcessor.getMessageProperties().getHeaders().put("failed-time",
                                LocalDateTime.now().toString());
                        return messagePostProcessor;
                    });
            log.info("消息已发送到死信队列: {}", messageBody);
        } catch (Exception e) {
            log.error("发送消息到死信队列失败: {}", messageBody, e);
        }
    }

    /**
     * 记录失败的消息
     */
    private void logFailedMessage(String messageBody, Map<String, Object> headers) {
        try {
            String errorReason = (String) headers.get("error-reason");
            String failedTime = (String) headers.get("failed-time");

            log.error("=== 消息处理最终失败 ===");
            log.error("消息内容: {}", messageBody);
            log.error("失败原因: {}", errorReason);
            log.error("失败时间: {}", failedTime);
            log.error("消息头信息: {}", headers);
            log.error("========================");

            // 这里可以实现：
            // 1. 保存到错误日志表
            // 2. 发送告警邮件
            // 3. 推送到监控系统
            // 4. 写入专门的错误日志文件

        } catch (Exception e) {
            log.error("记录失败消息时出错", e);
        }
    }
}

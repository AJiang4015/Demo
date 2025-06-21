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
 * 使用Spring Boot自带的重试机制处理失败消息
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
     * 使用手动确认模式和自定义重试机制
     */
    @RabbitListener(queues = RabbitMQConfig.OPERATION_LOG_QUEUE, ackMode = "MANUAL")
    public void onMessage(String messageBody, Message message, Channel channel) throws IOException {
        long startTime = System.currentTimeMillis();
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        Integer retryCount = (Integer) headers.getOrDefault("x-retry-count", 0);

        log.info("[消息接收] 操作日志消息: deliveryTag={}, retryCount={}, messageSize={}bytes",
                deliveryTag, retryCount, messageBody.length());
        log.debug("[消息内容] {}", messageBody);

        try {
            // JSON 反序列化
            long parseStartTime = System.currentTimeMillis();
            OperationLog operationLog = objectMapper.readValue(messageBody, OperationLog.class);
            long parseTime = System.currentTimeMillis() - parseStartTime;

            log.debug("[消息解析] 成功解析操作日志: userId={}, action={}, ip={}, 解析耗时={}ms",
                    operationLog.getUserId(), operationLog.getAction(), operationLog.getIp(), parseTime);

            // 保存数据库
            long saveStartTime = System.currentTimeMillis();
            operationLogService.save(operationLog);
            long saveTime = System.currentTimeMillis() - saveStartTime;

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("[消息处理] 操作日志保存成功: logId={}, userId={}, action={}, 保存耗时={}ms, 总耗时={}ms",
                    operationLog.getLogId(), operationLog.getUserId(), operationLog.getAction(), saveTime, totalTime);

            // 正常确认
            channel.basicAck(deliveryTag, false);
            log.debug("[消息确认] 消息已确认: deliveryTag={}", deliveryTag);

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // JSON 反序列化失败：直接丢弃并进死信队列
            log.error("[反序列化失败] 消息格式错误，直接进入死信队列", e);
            sendToDeadLetterQueue(messageBody, "反序列化失败：" + e.getMessage());
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("[消息异常] 操作日志消息处理失败: deliveryTag={}, retryCount={}, error={}, 总耗时={}ms",
                    deliveryTag, retryCount, e.getMessage(), totalTime, e);

            if (retryCount < 3) {
                // 使用 rabbitTemplate 重新发送消息（增加 retryCount）
                log.warn("[消息重试] 第{}次失败，重新投递: deliveryTag={}", retryCount, deliveryTag);

                try {
                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.OPERATION_LOG_EXCHANGE,
                            RabbitMQConfig.OPERATION_LOG_ROUTING_KEY,
                            messageBody,
                            msg -> {
                                msg.getMessageProperties().getHeaders().put("x-retry-count", retryCount + 1);
                                return msg;
                            }
                    );
                    log.info("[消息重试] 成功重新投递消息");

                } catch (Exception resendEx) {
                    log.error("[消息重试失败] 重新发送消息失败，将直接丢弃", resendEx);
                    sendToDeadLetterQueue(messageBody, "消息重试发送失败：" + resendEx.getMessage());
                }

                // 无论成功失败，当前消息需确认避免死循环
                channel.basicAck(deliveryTag, false);

            } else {
                // 超过重试次数，转入死信队列
                log.error("[死信处理] 消息重试超过最大次数，转入死信队列: deliveryTag={}", deliveryTag);
                sendToDeadLetterQueue(messageBody, "已达最大重试次数：" + e.getMessage());
                channel.basicAck(deliveryTag, false);
            }
        }
    }


    /**
     * 监听死信队列
     * 处理最终失败的消息
     */
    @RabbitListener(queues = RabbitMQConfig.DEAD_LETTER_QUEUE, ackMode = "MANUAL")
    public void onDeadLetterMessage(String messageBody, Message message, Channel channel) throws IOException {
        long startTime = System.currentTimeMillis();
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        Map<String, Object> headers = message.getMessageProperties().getHeaders();

        log.warn("[死信接收] 接收到死信队列消息: deliveryTag={}, messageSize={}bytes",
                deliveryTag, messageBody.length());
        log.debug("[死信内容] {}", messageBody);
        log.debug("[死信头部] headers={}", headers);

        try {
            // 记录失败的消息到特殊的错误日志表或文件
            // 这里可以实现告警机制，通知运维人员
            long logStartTime = System.currentTimeMillis();
            logFailedMessage(messageBody, headers);
            long logTime = System.currentTimeMillis() - logStartTime;

            long totalTime = System.currentTimeMillis() - startTime;
            log.warn("[死信处理] 死信消息处理完成: deliveryTag={}, 记录耗时={}ms, 总耗时={}ms",
                    deliveryTag, logTime, totalTime);

            // 确认死信消息
            channel.basicAck(deliveryTag, false);
            log.debug("[死信确认] 死信消息已确认: deliveryTag={}", deliveryTag);

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("[死信异常] 处理死信队列消息失败: deliveryTag={}, error={}, 总耗时={}ms",
                    deliveryTag, e.getMessage(), totalTime, e);
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
                    RabbitMQConfig.DEAD_LETTER_EXCHANGE,
                    RabbitMQConfig.DEAD_LETTER_ROUTING_KEY,
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

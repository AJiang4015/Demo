package com.ajiang.loggingservice.listener;

import com.ajiang.common.config.RabbitMQConfig;
import com.ajiang.loggingservice.entity.OperationLog;
import com.ajiang.loggingservice.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 操作日志消息监听器
 * 监听RabbitMQ中的操作日志消息并落库
 */
@Slf4j
@Component
public class OperationLogListener {

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.OPERATION_LOG_QUEUE)
    public void onMessage(String message) {
        try {
            log.info("接收到操作日志消息: {}", message);

            // 解析JSON消息为OperationLog对象
            OperationLog operationLog = objectMapper.readValue(message, OperationLog.class);

            // 保存操作日志到数据库
            operationLogService.save(operationLog);

            log.info("操作日志保存成功: logId={}, userId={}, action={}",
                    operationLog.getLogId(), operationLog.getUserId(), operationLog.getAction());

        } catch (Exception e) {
            log.error("处理操作日志消息失败: {}", message, e);
            // 这里可以根据需要实现重试机制或者将失败消息发送到死信队列
        }
    }
}

package com.ajiang.userservice.mq;

import com.ajiang.common.mq.LogMessage;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *@BelongsProject: Demo
 *@BelongsPackage: com.ajiang.userservice.mq
 *@Author: ajiang
 *@CreateTime: 2025-06-17  21:25
 *@Description: TODO
 *@Version: 1.0
 */
@Component
public class LogProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void sendRegisterLog(LogMessage logMessage) {
        rocketMQTemplate.convertAndSend("OPERATION_LOG_TOPIC", logMessage);
    }
}

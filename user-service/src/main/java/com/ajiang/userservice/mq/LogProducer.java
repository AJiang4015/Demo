package com.ajiang.userservice.mq;

import com.ajiang.common.config.RabbitMQConfig;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * AI
 * 日志消息服务
 * 负责发送操作日志消息到RabbitMQ
 * 实现生产者可靠性：消息确认、持久化、重试机制
 */
@Slf4j
@Service
public class LogProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 存储待确认的消息，用于重试机制
    private final Map<String, PendingMessage> pendingMessages = new ConcurrentHashMap<>();

    // 定时任务执行器，用于重试失败的消息
    private final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(2);

    /**
     * 待确认消息实体
     */
    private static class PendingMessage {
        private final String messageContent;
        private final LocalDateTime sendTime;
        private int retryCount;
        private final Map<String, Object> originalData;

        public PendingMessage(String messageContent, Map<String, Object> originalData) {
            this.messageContent = messageContent;
            this.originalData = originalData;
            this.sendTime = LocalDateTime.now();
            this.retryCount = 0;
        }

        // getters and setters
        public String getMessageContent() {
            return messageContent;
        }

        public LocalDateTime getSendTime() {
            return sendTime;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public void incrementRetryCount() {
            this.retryCount++;
        }

        public Map<String, Object> getOriginalData() {
            return originalData;
        }
    }

    /**
     * 初始化生产者确认机制
     */
    @PostConstruct
    public void init() {
        // 设置生产者确认回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (correlationData != null) {
                String messageId = correlationData.getId();
                PendingMessage pendingMessage = pendingMessages.get(messageId);

                if (ack) {
                    // 消息成功到达交换机，移除待确认消息
                    pendingMessages.remove(messageId);
                    log.info("[消息确认] 消息成功到达交换机: messageId={}, pendingCount={}",
                            messageId, pendingMessages.size());
                } else {
                    // 消息未到达交换机，记录失败原因并重试
                    int retryCount = pendingMessage != null ? pendingMessage.getRetryCount() : 0;
                    log.error("[消息确认] 消息未到达交换机: messageId={}, cause={}, currentRetryCount={}",
                            messageId, cause, retryCount);
                    retryMessage(messageId);
                }
            } else {
                log.warn("[消息确认] 收到空的关联数据，无法处理确认回调");
            }
        });

        // 设置消息返回回调（当消息无法路由到队列时触发）
        rabbitTemplate.setReturnsCallback(returned -> {
            String messageId = returned.getMessage().getMessageProperties().getMessageId();
            PendingMessage pendingMessage = pendingMessages.get(messageId);
            int retryCount = pendingMessage != null ? pendingMessage.getRetryCount() : 0;

            log.error(
                    "[消息路由] 消息无法路由到队列: messageId={}, replyCode={}, replyText={}, exchange={}, routingKey={}, currentRetryCount={}",
                    messageId, returned.getReplyCode(), returned.getReplyText(),
                    returned.getExchange(), returned.getRoutingKey(), retryCount);
            retryMessage(messageId);
        });

        // 启动定时清理任务，清理超时的待确认消息
        retryExecutor.scheduleWithFixedDelay(this::cleanupExpiredMessages, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * 重试失败的消息
     */
    private void retryMessage(String messageId) {
        PendingMessage pendingMessage = pendingMessages.get(messageId);
        if (pendingMessage != null) {
            pendingMessage.incrementRetryCount();
            int currentRetryCount = pendingMessage.getRetryCount();
            int maxRetryCount = RabbitMQConfig.MAX_RETRY_COUNT;

            log.info("[消息重试] 开始重试消息: messageId={}, currentRetryCount={}, maxRetryCount={}, pendingCount={}",
                    messageId, currentRetryCount, maxRetryCount, pendingMessages.size());

            if (currentRetryCount <= maxRetryCount) {
                // 延迟重试
                retryExecutor.schedule(() -> {
                    try {
                        log.info("[消息重试] 执行重试发送: messageId={}, retryCount={}", messageId, currentRetryCount);
                        sendMessageWithConfirm(pendingMessage.getMessageContent(), pendingMessage.getOriginalData(),
                                messageId);
                    } catch (Exception e) {
                        log.error("[消息重试] 重试发送失败: messageId={}, retryCount={}, error={}",
                                messageId, currentRetryCount, e.getMessage(), e);
                    }
                }, 5, TimeUnit.SECONDS);
            } else {
                // 超过最大重试次数，发送到死信队列
                log.error("[消息重试] 重试次数超限，发送到死信队列: messageId={}, finalRetryCount={}, maxRetryCount={}",
                        messageId, currentRetryCount, maxRetryCount);
                sendToDeadLetterQueue(pendingMessage);
                pendingMessages.remove(messageId);
                log.info("[消息重试] 已移除超限消息: messageId={}, remainingPendingCount={}",
                        messageId, pendingMessages.size());
            }
        } else {
            log.warn("[消息重试] 未找到待重试消息: messageId={}", messageId);
        }
    }

    /**
     * 清理过期的待确认消息
     */
    private void cleanupExpiredMessages() {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(10); // 10分钟超时
        int beforeCount = pendingMessages.size();

        log.debug("[消息清理] 开始清理过期消息: currentPendingCount={}, expireTime={}",
                beforeCount, expireTime);

        pendingMessages.entrySet().removeIf(entry -> {
            if (entry.getValue().getSendTime().isBefore(expireTime)) {
                log.warn("[消息清理] 清理过期消息: messageId={}, sendTime={}, retryCount={}",
                        entry.getKey(), entry.getValue().getSendTime(), entry.getValue().getRetryCount());
                return true;
            }
            return false;
        });

        int afterCount = pendingMessages.size();
        int cleanedCount = beforeCount - afterCount;

        if (cleanedCount > 0) {
            log.info("[消息清理] 清理完成: cleanedCount={}, remainingCount={}", cleanedCount, afterCount);
        } else {
            log.debug("[消息清理] 无过期消息需要清理: pendingCount={}", afterCount);
        }
    }

    /**
     * 发送消息到死信队列
     */
    private void sendToDeadLetterQueue(PendingMessage pendingMessage) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("[死信发送] 开始发送消息到死信队列: retryCount={}, originalSendTime={}",
                    pendingMessage.getRetryCount(), pendingMessage.getSendTime());

            // 添加失败信息到消息中
            Map<String, Object> deadLetterData = new HashMap<>(pendingMessage.getOriginalData());
            deadLetterData.put("failureReason", "超过最大重试次数");
            deadLetterData.put("retryCount", pendingMessage.getRetryCount());
            deadLetterData.put("originalSendTime", pendingMessage.getSendTime());
            deadLetterData.put("deadLetterTime", LocalDateTime.now());

            String deadLetterMessage = JSON.toJSONString(deadLetterData);

            log.debug("[死信发送] 死信消息内容: messageSize={}bytes", deadLetterMessage.length());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DEAD_LETTER_EXCHANGE,
                    RabbitMQConfig.DEAD_LETTER_ROUTING_KEY,
                    deadLetterMessage);

            long sendTime = System.currentTimeMillis() - startTime;
            log.info("[死信发送] 消息已发送到死信队列: exchange={}, routingKey={}, 发送耗时={}ms",
                    RabbitMQConfig.DEAD_LETTER_EXCHANGE, RabbitMQConfig.DEAD_LETTER_ROUTING_KEY, sendTime);
            log.debug("[死信发送] 死信消息详情: {}", deadLetterMessage);

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("[死信发送] 发送消息到死信队列失败: error={}, 耗时={}ms", e.getMessage(), totalTime, e);
        }
    }

    /**
     * 带确认机制的消息发送
     */
    private void sendMessageWithConfirm(String messageContent, Map<String, Object> originalData, String messageId) {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("[消息发送] 开始发送消息: messageId={}, messageSize={}bytes, pendingCount={}",
                    messageId, messageContent.length(), pendingMessages.size());

            // 创建关联数据用于确认回调
            CorrelationData correlationData = new CorrelationData(messageId);

            // 创建消息属性，设置持久化
            MessageProperties properties = new MessageProperties();
            properties.setDeliveryMode(MessageProperties.DEFAULT_DELIVERY_MODE); // 持久化消息
            properties.setMessageId(messageId);
            properties.setTimestamp(new java.util.Date());

            // 创建消息对象
            Message message = new Message(messageContent.getBytes(), properties);

            // 发送消息
            rabbitTemplate.send(
                    RabbitMQConfig.OPERATION_LOG_EXCHANGE,
                    RabbitMQConfig.OPERATION_LOG_ROUTING_KEY,
                    message,
                    correlationData);

            long sendTime = System.currentTimeMillis() - startTime;
            log.info("[消息发送] 消息发送完成: messageId={}, exchange={}, routingKey={}, 发送耗时={}ms",
                    messageId, RabbitMQConfig.OPERATION_LOG_EXCHANGE,
                    RabbitMQConfig.OPERATION_LOG_ROUTING_KEY, sendTime);

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("[消息发送] 发送消息失败: messageId={}, error={}, 耗时={}ms",
                    messageId, e.getMessage(), totalTime, e);
            throw e;
        }
    }

    /**
     * 发送操作日志消息（增强版，支持可靠性机制）
     *
     * @param userId 用户ID
     * @param action 操作类型
     * @param ip     IP地址
     * @param detail 操作详情
     */
    public void sendOperationLog(Long userId, String action, String ip, Map<String, Object> detail) {
        long startTime = System.currentTimeMillis();
        String messageId = UUID.randomUUID().toString();

        try {
            log.info("[操作日志] 开始发送操作日志: messageId={}, userId={}, action={}, ip={}",
                    messageId, userId, action, ip);
            log.debug("[操作日志] 操作详情: {}", detail);

            // 构建日志消息
            long buildStartTime = System.currentTimeMillis();
            Map<String, Object> logMessage = new HashMap<>();
            logMessage.put("userId", userId);
            logMessage.put("action", action);
            logMessage.put("ip", ip);
            logMessage.put("detail", JSON.toJSONString(detail));
            logMessage.put("timestamp", LocalDateTime.now());
            logMessage.put("retryCount", 0);

            // 转换为JSON字符串
            String messageContent = JSON.toJSONString(logMessage);
            long buildTime = System.currentTimeMillis() - buildStartTime;

            log.debug("[操作日志] 消息构建完成: messageId={}, messageSize={}bytes, 构建耗时={}ms",
                    messageId, messageContent.length(), buildTime);

            // 存储待确认消息
            pendingMessages.put(messageId, new PendingMessage(messageContent, logMessage));
            log.debug("[操作日志] 消息已加入待确认队列: messageId={}, pendingCount={}",
                    messageId, pendingMessages.size());

            // 发送消息
            sendMessageWithConfirm(messageContent, logMessage, messageId);

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("[操作日志] 操作日志发送完成: messageId={}, userId={}, action={}, ip={}, 总耗时={}ms",
                    messageId, userId, action, ip, totalTime);

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("[操作日志] 操作日志发送失败: messageId={}, userId={}, action={}, ip={}, error={}, 总耗时={}ms",
                    messageId, userId, action, ip, e.getMessage(), totalTime, e);
            // 可以考虑将失败的消息存储到本地文件或数据库中，作为最后的保障
        }
    }

    /**
     * 发送用户注册日志
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param email    邮箱
     * @param phone    手机号
     * @param ip       IP地址
     */
    public void sendUserRegisterLog(Long userId, String username, String email, String phone, String ip) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("username", username);
        detail.put("email", email);
        detail.put("phone", phone);

        sendOperationLog(userId, "USER_REGISTER", ip, detail);
    }

    /**
     * 发送用户登录日志
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param ip       IP地址
     */
    public void sendUserLoginLog(Long userId, String username, String ip) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("username", username);

        sendOperationLog(userId, "USER_LOGIN", ip, detail);
    }

    /**
     * 发送用户登出日志
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param ip       IP地址
     */
    public void sendUserLogoutLog(Long userId, String username, String ip) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("username", username);

        sendOperationLog(userId, "USER_LOGOUT", ip, detail);
    }

    /**
     * 发送用户信息更新日志
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param ip       IP地址
     * @param changes  变更内容
     */
    public void sendUserUpdateLog(Long userId, String username, String ip, Map<String, Object> changes) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("username", username);
        detail.put("changes", changes);

        sendOperationLog(userId, "USER_UPDATE", ip, detail);
    }

    /**
     * 发送密码重置日志
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param ip       IP地址
     */
    public void sendPasswordResetLog(Long userId, String username, String ip) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("username", username);

        sendOperationLog(userId, "PASSWORD_RESET", ip, detail);
    }

    /**
     * 发送查看用户信息日志
     *
     * @param currentUserId  当前用户ID
     * @param targetUsername 目标用户名
     * @param targetUserId   目标用户ID
     * @param ip             客户端IP
     */
    public void sendUserInfoViewLog(Long currentUserId, String targetUsername, Long targetUserId, String ip) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("targetUsername", targetUsername);
        detail.put("targetUserId", targetUserId);
        detail.put("viewType", "single_user");

        sendOperationLog(currentUserId, "USER_INFO_VIEW", ip, detail);
    }

    /**
     * 发送查看用户列表日志
     *
     * @param currentUserId   当前用户ID
     * @param currentUserRole 当前用户角色
     * @param pageNo          页码
     * @param pageSize        页大小
     * @param resultCount     结果数量
     * @param ip              客户端IP
     */
    public void sendUserListViewLog(Long currentUserId, String currentUserRole, Long pageNo, Long pageSize,
                                    int resultCount, String ip) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("currentUserRole", currentUserRole);
        detail.put("pageNo", pageNo);
        detail.put("pageSize", pageSize);
        detail.put("resultCount", resultCount);
        detail.put("viewType", "user_list");

        sendOperationLog(currentUserId, "USER_LIST_VIEW", ip, detail);
    }
}
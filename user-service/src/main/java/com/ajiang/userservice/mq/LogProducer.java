package com.ajiang.userservice.mq;

import com.ajiang.common.config.RabbitMQConfig;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

/**
 * 日志消息服务
 * 负责发送操作日志消息到RabbitMQ
 */
@Slf4j
@Service
public class LogProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送操作日志消息
     *
     * @param userId 用户ID
     * @param action 操作类型
     * @param ip     IP地址
     * @param detail 操作详情
     */
    public void sendOperationLog(Long userId, String action, String ip, Map<String, Object> detail) {
        try {
            // 构建日志消息
            Map<String, Object> logMessage = new HashMap<>();
            logMessage.put("userId", userId);
            logMessage.put("action", action);
            logMessage.put("ip", ip);
            logMessage.put("detail", JSON.toJSONString(detail));

            // 转换为JSON字符串
            String message = JSON.toJSONString(logMessage);

            // 发送消息到RabbitMQ
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.OPERATION_LOG_EXCHANGE,
                    RabbitMQConfig.OPERATION_LOG_ROUTING_KEY,
                    message);

            log.info("操作日志消息发送成功: userId={}, action={}, ip={}", userId, action, ip);

        } catch (Exception e) {
            log.error("操作日志消息发送失败: userId={}, action={}, ip={}, 错误: {}",
                    userId, action, ip, e.getMessage(), e);
            // 这里可以根据需要实现重试机制或者记录到本地日志
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
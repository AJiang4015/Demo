package com.ajiang.userservice.feignclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PermissionServiceFallbackFactory implements FallbackFactory<PermissionServiceClient> {

    @Override
    public PermissionServiceClient create(Throwable cause) {
        log.error("PermissionService调用失败: {}", cause.getMessage());
        
        return new PermissionServiceClient() {
            @Override
            public void bindDefaultRole(Long userId) {
                log.warn("bindDefaultRole降级处理: userId={}", userId);
                // 这里可添加本地补偿逻辑
            }

            @Override
            public String getUserRoleCode(Long userId) {
                log.warn("getUserRoleCode降级处理: userId={}", userId);
                return "user"; // 默认返回普通用户角色
            }

            @Override
            public void upgradeToAdmin(Long userId) {
                log.warn("upgradeToAdmin降级处理: userId={}", userId);
            }

            @Override
            public void downgradeToUser(Long userId) {
                log.warn("downgradeToUser降级处理: userId={}", userId);
            }
        };
    }
}
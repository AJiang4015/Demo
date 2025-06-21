package com.ajiang.userservice.feignclient;

import com.ajiang.common.model.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
            }

            @Override
            public String getUserRoleCode(Long userId) {
                log.warn("getUserRoleCode降级处理: userId={}", userId);
                return "user";
            }

            @Override
            public void upgradeToAdmin(Long userId) {
                log.warn("upgradeToAdmin降级处理: userId={}", userId);
            }

            @Override
            public void downgradeToUser(Long userId) {
                log.warn("downgradeToUser降级处理: userId={}", userId);
            }

            @Override
            public PageResult<Long> getVisibleUserIds(Long currentUserId, String currentUserRole, int pageNo,
                                                      int pageSize) {
                log.warn("getVisibleUserIds降级处理: currentUserId={}, role={}", currentUserId, currentUserRole);
                // 降级时只返回当前用户自己的ID
                PageResult<Long> result = new PageResult<>();
                result.setItems(Collections.singletonList(currentUserId));
                result.setCounts(1L);
                result.setPage(pageNo);
                result.setPageSize(pageSize);
                return result;
            }
        };
    }
}

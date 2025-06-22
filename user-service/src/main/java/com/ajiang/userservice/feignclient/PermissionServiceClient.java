package com.ajiang.userservice.feignclient;

import com.ajiang.common.model.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 权限服务RPC接口客户端
 * 提供用户角色管理相关的远程调用功能
 */
@FeignClient(name = "permission-service", // 服务名称
        contextId = "permissionService", // 上下文ID防止冲突
        fallbackFactory = PermissionServiceFallbackFactory.class // 降级处理
)
public interface PermissionServiceClient {


    @PostMapping("/role/bind/{userId}")
    void bindDefaultRole(@PathVariable("userId") Long userId);

    @GetMapping("/role/code/{userId}")
    String getUserRoleCode(@PathVariable("userId") Long userId);

    @PostMapping("/role/upgrade/{userId}")
    void upgradeToAdmin(@PathVariable("userId") Long userId);

    @PostMapping("/role/downgrade/{userId}")
    void downgradeToUser(@PathVariable("userId") Long userId);

    @PostMapping("/role/visible-users")
    PageResult<Long> getVisibleUserIds(
            @RequestParam Long currentUserId,
            @RequestParam String currentUserRole,
            @RequestParam int pageNo,
            @RequestParam int pageSize);
}

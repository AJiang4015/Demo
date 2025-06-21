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

    /**
     * 绑定默认角色（普通用户）
     * 用于用户注册时自动绑定默认角色
     *
     * @param userId 用户ID
     */
    @PostMapping("/role/bind/{userId}")
    void bindDefaultRole(@PathVariable("userId") Long userId);

    /**
     * 查询用户角色码
     * 返回用户的角色代码：super_admin/admin/user
     *
     * @param userId 用户ID
     * @return 角色代码
     */
    @GetMapping("/role/code/{userId}")
    String getUserRoleCode(@PathVariable("userId") Long userId);

    /**
     * 超管调用：升级用户为管理员
     * 只有超级管理员可以调用此接口
     *
     * @param userId 用户ID
     */
    @PostMapping("/role/upgrade/{userId}")
    void upgradeToAdmin(@PathVariable("userId") Long userId);

    /**
     * 超管调用：降级用户为普通角色
     * 只有超级管理员可以调用此接口
     *
     * @param userId 用户ID
     */
    @PostMapping("/role/downgrade/{userId}")
    void downgradeToUser(@PathVariable("userId") Long userId);

    /**
     * 分页查询可见用户ID列表
     *
     * @param currentUserId 当前用户ID
     * @param currentUserRole 当前用户角色
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @return 分页结果（只包含用户ID）
     */
    @PostMapping("/role/visible-users")
    PageResult<Long> getVisibleUserIds(
            @RequestParam Long currentUserId,
            @RequestParam String currentUserRole,
            @RequestParam int pageNo,
            @RequestParam int pageSize);
}

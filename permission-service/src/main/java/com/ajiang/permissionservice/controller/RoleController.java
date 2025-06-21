package com.ajiang.permissionservice.controller;

import com.ajiang.common.model.ApiResponse;
import com.ajiang.common.model.PageResult;
import com.ajiang.permissionservice.serivce.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 权限服务控制器
 * 提供用户角色管理的REST接口
 *
 * @author ajiang
 * @version 1.0
 * @since 2025-06-17
 */
@Slf4j
@RestController
@RequestMapping("/role")
public class RoleController {

    @Autowired
    RoleService roleService;

    /**
     * 绑定默认角色（普通用户）
     * 为新注册用户绑定默认的普通用户角色
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/bind/{userId}")
    public ApiResponse<Void> bindDefaultRole(@PathVariable Long userId) {
        log.info("绑定默认角色请求: userId={}", userId);
        roleService.bindDefaultRole(userId);
        log.info("绑定默认角色成功: userId={}", userId);
        return ApiResponse.success();
    }

    /**
     * 查询用户角色码
     * 根据用户ID查询对应的角色代码
     *
     * @param userId 用户ID
     * @return 角色代码（super_admin/admin/user）
     */
    @GetMapping("/code/{userId}")
    public String getUserRoleCode(@PathVariable Long userId) {
        log.debug("查询用户角色码请求: userId={}", userId);
        String roleCode = roleService.getUserRoleCode(userId);
        log.debug("查询用户角色码成功: userId={}, roleCode={}", userId, roleCode);
        return roleCode;
    }

    /**
     * 升级用户为管理员
     * 超级管理员调用：将普通用户升级为管理员
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/upgrade/{userId}")
    public ApiResponse<Void> upgradeToAdmin(@PathVariable Long userId) {
        log.info("升级用户为管理员请求: userId={}", userId);
        roleService.upgradeToAdmin(userId);
        log.info("升级用户为管理员成功: userId={}", userId);
        return ApiResponse.success();
    }

    /**
     * 降级用户为普通用户
     * 超级管理员调用：将管理员降级为普通用户
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/downgrade/{userId}")
    public ApiResponse<Void> downgradeToUser(@PathVariable Long userId) {
        log.info("降级用户为普通用户请求: userId={}", userId);
        roleService.downgradeToUser(userId);
        log.info("降级用户为普通用户成功: userId={}", userId);
        return ApiResponse.success();
    }

    /**
     * 分页查询可见用户ID列表
     *
     * @param currentUserId 当前用户ID
     * @param currentUserRole 当前用户角色
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @return 分页结果（只包含用户ID）
     */
    @PostMapping("/visible-users")
    public PageResult<Long> getVisibleUserIds(
            @RequestParam Long currentUserId,
            @RequestParam String currentUserRole,
            @RequestParam int pageNo,
            @RequestParam int pageSize) {

        log.info("分页查询可见用户ID列表: currentUserId={}, currentUserRole={}, pageNo={}, pageSize={}",
                currentUserId, currentUserRole, pageNo, pageSize);

        // 参数验证
        if (currentUserId == null || currentUserId <= 0) {
            throw new IllegalArgumentException("当前用户ID不能为空且必须大于0");
        }
        if (currentUserRole == null || currentUserRole.trim().isEmpty()) {
            throw new IllegalArgumentException("当前用户角色不能为空");
        }
        if (pageNo <= 0) {
            throw new IllegalArgumentException("页码必须大于0");
        }
        if (pageSize <= 0 || pageSize > 100) {
            throw new IllegalArgumentException("每页大小必须在1-100之间");
        }

        try {
            PageResult<Long> visibleUserIds = roleService.getVisibleUserIds(currentUserId, currentUserRole, pageNo,
                    pageSize);

            log.info("分页查询可见用户ID列表成功: 返回{}个用户ID, 总数={}",
                    visibleUserIds.getItems().size(), visibleUserIds.getCounts());

            return visibleUserIds;
        } catch (Exception e) {
            log.error("分页查询可见用户ID列表失败: currentUserId={}, currentUserRole={}, 错误: {}",
                    currentUserId, currentUserRole, e.getMessage(), e);
            throw e;
        }
    }
}

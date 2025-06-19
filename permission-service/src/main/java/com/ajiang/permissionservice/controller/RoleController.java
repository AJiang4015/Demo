package com.ajiang.permissionservice.controller;

import com.ajiang.common.model.ApiResponse;
import com.ajiang.permissionservice.serivce.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<String> getUserRoleCode(@PathVariable Long userId) {
        log.debug("查询用户角色码请求: userId={}", userId);
        String roleCode = roleService.getUserRoleCode(userId);
        log.debug("查询用户角色码成功: userId={}, roleCode={}", userId, roleCode);
        return ApiResponse.success(roleCode);
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

}

/*
package com.ajiang.userservice.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "permission-service", // 服务名称
    contextId = "permissionService", // 上下文ID防止冲突
    fallbackFactory = PermissionServiceFallbackFactory.class // 降级处理
)
public interface PermissionServiceClient {
    

    */
/**
     * @description: 绑定默认角色（普通用户）
     * @author: ajiang
     * @date: 2025/6/17 22:30
     * @param: [userId]
     * @return: void
     **//*

    @PostMapping("/role/bind")
    void bindDefaultRole(@RequestParam("userId") Long userId);
    
    */
/**
     * @description: 查询用户角色码
     * @author: ajiang
     * @date: 2025/6/17 22:31
     * @param: [userId]
     * @return: role_code
     **//*

    @GetMapping("/role/code")
    String getUserRoleCode(@RequestParam("userId") Long userId);
    
    */
/**
     * @description: 超管调用：升级用户为管理员
     * @author: ajiang
     * @date: 2025/6/17 22:31
     * @param: [userId]
     * @return: void
     **//*

    @PostMapping("/role/upgrade")
    void upgradeToAdmin(@RequestParam("userId") Long userId);
    
    */
/**
     * @description: 超管调用：降级用户为普通角色
     * @author: ajiang
     * @date: 2025/6/17 22:32
     * @param: [userId]
     * @return: void
     **//*

    @PostMapping("/role/downgrade")
    void downgradeToUser(@RequestParam("userId") Long userId);
}*/

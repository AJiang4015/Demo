package com.ajiang.permissionservice.controller;

import com.ajiang.permissionservice.serivce.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 *@BelongsProject: Demo
 *@BelongsPackage: com.ajiang.permissionservice.controller
 *@Author: ajiang
 *@CreateTime: 2025-06-17  22:35
 *@Description: TODO
 *@Version: 1.0
 */
@RestController
@RequestMapping("/permission")
public class RoleController {

    @Autowired
    RoleService roleService;

    /**
     * @description: 绑定默认角色（普通用户）
     * @author: ajiang
     * @date: 2025/6/17 22:30
     * @param: [userId]
     * @return: void
     **/
    @PostMapping("/role/bind")
    void bindDefaultRole(@RequestParam("userId") Long userId) {
        roleService.bindDefaultRole(userId);
    }

    /**
     * @description: 查询用户角色码
     * @author: ajiang
     * @date: 2025/6/17 22:31
     * @param: [userId]
     * @return: role_code
     **/
    @GetMapping("/role/code")
    String getUserRoleCode(@RequestParam("userId") Long userId){
        return roleService.getUserRoleCode(userId);
    }

    /**
     * @description: 超管调用：升级用户为管理员
     * @author: ajiang
     * @date: 2025/6/17 22:31
     * @param: [userId]
     * @return: void
     **/
    @PostMapping("/role/upgrade")
    void upgradeToAdmin(@RequestParam("userId") Long userId){
            roleService.upgradeToAdmin(userId);
    }

    /**
     * @description: 超管调用：降级用户为普通角色
     * @author: ajiang
     * @date: 2025/6/17 22:32
     * @param: [userId]
     * @return: void
     **/
    @PostMapping("/role/downgrade")
    void downgradeToUser(@RequestParam("userId") Long userId){
            roleService.downgradeToUser(userId);
    }

}

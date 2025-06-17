package com.ajiang.permissionservice.serivce;

import com.ajiang.permissionservice.entity.Role;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *@BelongsProject: Demo
 *@BelongsPackage: com.ajiang.permissionservice.serivce
 *@Author: ajiang
 *@CreateTime: 2025-06-17  22:36
 *@Description: TODO
 *@Version: 1.0
 */
@Service
public interface RoleService extends IService<Role> {

    void bindDefaultRole(Long userId);

    String getUserRoleCode(Long userId);

    void upgradeToAdmin(Long userId);

    void downgradeToUser(Long userId);
}

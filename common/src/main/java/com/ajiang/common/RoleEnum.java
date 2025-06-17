package com.ajiang.common;

import java.util.Arrays;

/**
 *@BelongsProject: Demo
 *@BelongsPackage: com.ajiang.common
 *@Author: ajiang
 *@CreateTime: 2025-06-17  23:33
 *@Description: TODO
 *@Version: 1.0
 */
// 在common模块中定义
public enum RoleEnum {
    SUPER_ADMIN(1, "super_admin"),
    USER(2, "user"),
    ADMIN(3, "admin");

    private int roleId;
    private String roleCode;

    RoleEnum(int roleId, String roleCode) {
        this.roleId = roleId;
        this.roleCode = roleCode;
    }

}
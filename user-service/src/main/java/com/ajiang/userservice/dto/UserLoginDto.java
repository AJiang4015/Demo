package com.ajiang.userservice.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 *@BelongsProject: Demo
 *@BelongsPackage: com.ajiang.userservice.dto
 *@Author: ajiang
 *@CreateTime: 2025-06-18  09:14
 *@Description: TODO
 *@Version: 1.0
 */
@Data
public class UserLoginDto {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
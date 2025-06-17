package com.ajiang.userservice.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 密码重置数据传输对象
 * 用于接收密码重置请求的数据
 */
@Data
public class PasswordResetDto {

    /**
     * 用户ID
     * 不能为空
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 新密码
     * 不能为空，长度在6-20之间
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    private String newPassword;
}
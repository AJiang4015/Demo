package com.ajiang.userservice.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class PasswordResetDto {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    private String newPassword;
}
package com.ajiang.userservice.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 用户注册数据传输对象
 * 用于接收用户注册请求的数据
 */
@Data
public class UserRegisterDto {

    /**
     * 用户名
     * 不能为空，长度在4-20之间
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度必须在4-20之间")
    private String username;

    /**
     * 密码
     * 不能为空，长度在6-20之间
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    private String password;

    /**
     * 邮箱
     * 必须是有效的邮箱格式
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 手机号
     * 必须是有效的手机号格式
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
package com.ajiang.userservice.controller;

import com.ajiang.common.model.ApiResponse;
import com.ajiang.userservice.dto.PasswordResetDto;
import com.ajiang.userservice.dto.UserRegisterDto;
import com.ajiang.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 *@BelongsProject: Demo
 *@BelongsPackage: com.ajiang.userservice.controller
 *@Author: ajiang
 *@CreateTime: 2025-06-17  15:22
 *@Description: TODO
 *@Version: 1.0
 */

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;

    /**
     * @description: 用户注册
     * @author: ajiang
     * @date: 2025/6/17 15:28
     * @param: []
     * @return: response
     **/
    @PostMapping("/register")
    public ApiResponse<Long> register(@RequestBody @Valid UserRegisterDto registerDto){

        Long userId = userService.register(registerDto, ip);
        return ApiResponse.success(userId);
    }

    /**
     * @description: 用户登录
     * @author: ajiang
     * @date: 2025/6/17 15:27
     * @param: []
     * @return: response
     **/
    @PostMapping("/login")
    public ApiResponse login(@RequestBody @Valid ){

    }

    /**
     * @description: 查询用户信息
     * @author: ajiang
     * @date: 2025/6/17 15:29
     * @param: [userId]
     * @return: response
     **/
    @GetMapping("/{userId}")
    public ApiResponse getUserInfo(@PathVariable Long userId){

    }

    /**
     * @description: 修改用户信息
     * @author: ajiang
     * @date: 2025/6/17 15:31
     * @param: [userId]
     * @return: response
     **/
    @PutMapping("{userId}")
    public ApiResponse updateUser(@PathVariable Long userId){

    }

    /**
     * @description: 重置密码
     * @author: ajiang
     * @date: 2025/6/17 15:30
     * @param:
     * @return:
     **/
    @PostMapping("/rest-password")
    public ApiResponse resetPassword(@RequestBody @Valid PasswordResetDto passwordResetDto){

    }


}

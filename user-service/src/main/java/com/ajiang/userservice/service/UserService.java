package com.ajiang.userservice.service;

import com.ajiang.userservice.dto.UserRegisterDto;
import com.ajiang.userservice.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

import javax.validation.Valid;

/**
 *@BelongsProject: Demo
 *@BelongsPackage: com.ajiang.userservice.service
 *@Author: ajiang
 *@CreateTime: 2025-06-17  15:53
 *@Description: TODO
 *@Version: 1.0
 */
@Service
public interface UserService extends IService<User> {

    Long register(UserRegisterDto registerDto, String ip);
}

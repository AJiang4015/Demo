package com.ajiang.userservice.service.impl;

import com.ajiang.userservice.dto.UserRegisterDto;
import com.ajiang.userservice.entity.User;
import com.ajiang.userservice.service.UserService;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 *@BelongsProject: Demo
 *@BelongsPackage: com.ajiang.userservice.service.impl
 *@Author: ajiang
 *@CreateTime: 2025-06-17  15:54
 *@Description: TODO
 *@Version: 1.0
 */
public class UserServiceImpl implements UserService {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Override
    @GlobalTransactional(name = "user")
    public Long register(UserRegisterDto registerDto) {

        User user = User.builder()
                .username(registerDto.getUsername())
                .password(passwordEncoder.encode(registerDto.getPassword()))
                .email(registerDto.getEmail())
                .phone(registerDto.getPhone())
                .gmtCreate(LocalDateTime.now())
                .build();



    }


}

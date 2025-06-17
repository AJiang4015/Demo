package com.ajiang.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 *@BelongsProject: Demo
 *@BelongsPackage: com.ajiang.userservice.config
 *@Author: ajiang
 *@CreateTime: 2025-06-17  16:08
 *@Description: TODO
 *@Version: 1.0
 */
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ObjectMapper objectMapper() {

    }
}

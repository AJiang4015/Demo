package com.ajiang.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @BelongsProject: Demo
 * @BelongsPackage: com.ajiang.userservice.config
 * @Author: ajiang
 * @CreateTime: 2025-06-17 16:08
 * @Description: TODO
 * @Version: 1.0
 */
@Configuration
public class AppConfig {

    @Bean
    public SimplePasswordEncoder passwordEncoder() {
        return new SimplePasswordEncoder();
    }

    /**
     * 简单的密码编码器实现
     * 使用MD5加密（注意：生产环境建议使用更安全的加密方式）
     */
    public static class SimplePasswordEncoder {

        public String encode(CharSequence rawPassword) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] digest = md.digest(rawPassword.toString().getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("MD5算法不可用", e);
            }
        }

        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encode(rawPassword).equals(encodedPassword);
        }
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}

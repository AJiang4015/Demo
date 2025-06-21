package com.ajiang.permissionservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 权限服务启动类
 * 提供用户角色管理的微服务
 *
 * @author ajiang
 * @version 1.0
 * @since 2025-06-17
 */
@SpringBootApplication(scanBasePackages = { "com.ajiang.permissionservice", "com.ajiang.common" })
@EnableDiscoveryClient
@EnableTransactionManagement
@MapperScan("com.ajiang.permissionservice.mapper")
public class PermissionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PermissionServiceApplication.class, args);
    }
}

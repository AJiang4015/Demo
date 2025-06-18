package com.ajiang.loggingservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 日志服务启动类
 * 提供操作日志记录和查询的微服务
 *
 * @author ajiang
 * @version 1.0
 * @since 2025-06-17
 */
@SpringBootApplication(scanBasePackages = "com.ajiang")
@EnableDiscoveryClient
@EnableTransactionManagement
@MapperScan("com.ajiang.loggingservice.mapper")
public class LoggingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoggingServiceApplication.class, args);
    }

}

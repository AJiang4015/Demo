package com.ajiang.userservice.config;

import com.ajiang.userservice.interceptor.TokenValidationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * AI
 * Web配置类
 * 注册拦截器和其他Web相关配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private TokenValidationInterceptor tokenValidationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenValidationInterceptor)
                .addPathPatterns("/user/**") // 拦截所有用户相关接口
                .excludePathPatterns(
                        "/user/login", // 排除登录接口
                        "/user/register" // 排除注册接口
                );
    }
}
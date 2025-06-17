package com.ajiang.common.config;


import com.ajiang.common.exception.BusinessException;
import com.ajiang.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT拦截器
 * 用于验证请求中的JWT令牌
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    JwtUtil jwtUtil;

    /**
     * 在请求处理之前进行调用
     * 验证JWT令牌
     *
     * @param request  请求
     * @param response 响应
     * @param handler  处理器
     * @return 是否通过验证
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行登录和注册请求
        String requestURI = request.getRequestURI();
        if (requestURI.contains("/user/login") || requestURI.contains("/user/register")) {
            return true;
        }

        // 获取请求头中的Token
        String token = getTokenFromRequest(request);
        if (token == null) {
            log.warn("未提供Token");
            throw new BusinessException("未授权，请先登录");
        }

        // 验证Token
        if (!jwtUtil.validateToken(token)) {
            log.warn("无效的Token");
            throw new BusinessException("无效的Token，请重新登录");
        }

        return true;
    }

    /**
     * 从请求中获取Token
     *
     * @param request 请求
     * @return Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
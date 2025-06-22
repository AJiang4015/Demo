package com.ajiang.userservice.interceptor;

import com.ajiang.common.exception.BusinessException;
import com.ajiang.common.util.JwtUtil;
import com.ajiang.userservice.service.TokenWhitelistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Token验证拦截器
 * 验证JWT Token的有效性和白名单状态
 */
@Slf4j
@Component
public class TokenValidationInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenWhitelistService tokenWhitelistService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 跳过登录和注册接口
        String requestURI = request.getRequestURI();
        if (requestURI.contains("/login") || requestURI.contains("/register")) {
            return true;
        }

        try {
            // 获取token
            String token = getTokenFromRequest(request);
            if (token == null) {
                throw new BusinessException("未提供认证token");
            }

            // 验证JWT token的格式和签名
            if (!jwtUtil.validateToken(token)) {
                throw new BusinessException("无效的token");
            }

            // 检查token是否在Redis白名单中
            if (!tokenWhitelistService.isTokenInWhitelist(token)) {
                throw new BusinessException("token已失效，请重新登录");
            }

            // 验证通过，继续处理请求
            return true;

        } catch (BusinessException e) {
            log.warn("Token验证失败: {}, URI: {}", e.getMessage(), requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"" + e.getMessage() + "\",\"data\":null}");
            return false;
        } catch (Exception e) {
            log.error("Token验证异常: {}, URI: {}", e.getMessage(), requestURI, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":500,\"message\":\"服务器内部错误\",\"data\":null}");
            return false;
        }
    }

    /**
     * 从请求中获取Token
     *
     * @param request HTTP请求
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
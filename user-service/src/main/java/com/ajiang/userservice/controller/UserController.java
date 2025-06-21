package com.ajiang.userservice.controller;

import com.ajiang.common.model.ApiResponse;
import com.ajiang.common.model.PageParams;
import com.ajiang.common.model.PageResult;
import com.ajiang.common.util.JwtUtil;
import com.ajiang.userservice.dto.PasswordResetDto;
import com.ajiang.userservice.dto.UserLoginDto;
import com.ajiang.userservice.dto.UserRegisterDto;
import com.ajiang.userservice.dto.UserResponseDto;
import com.ajiang.userservice.entity.User;
import com.ajiang.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * @description: 用户注册
     * @author: ajiang
     * @date: 2025/6/21 17:39
     * @param: [registerDto, request]
     * @return: com.ajiang.common.model.ApiResponse<java.lang.Long>
     **/
    @PostMapping("/register")
    public ApiResponse<Long> register(@RequestBody @Valid UserRegisterDto registerDto, HttpServletRequest request) {
        log.info("用户注册请求: {}", registerDto.getUsername());
        String ip = getClientIp(request);
        Long userId = userService.register(registerDto, ip);
        log.info("用户注册成功: {}, userId={}", registerDto.getUsername(), userId);
        return ApiResponse.success(userId);
    }

    /**
     * @description: 用户登录
     * @author: ajiang
     * @date: 2025/6/21 17:39
     * @param: [loginDto, request]
     * @return: com.ajiang.common.model.ApiResponse<java.util.Map<java.lang.String,java.lang.String>>
     **/
    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@RequestBody @Valid UserLoginDto loginDto,
                                                  HttpServletRequest request) {
        log.info("用户登录请求: {}", loginDto.getUsername());
        String ip = getClientIp(request);
        String token = userService.login(loginDto, ip);

        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        log.info("用户登录成功: {}", loginDto.getUsername());
        return ApiResponse.success(result);
    }

    /**
     * @description: 用户登出
     * @author: ajiang
     * @date: 2025/6/21 17:39
     * @param: [request]
     * @return: com.ajiang.common.model.ApiResponse<java.lang.Boolean>
     **/
    @PostMapping("/logout")
    public ApiResponse<Boolean> logout(HttpServletRequest request) {
        log.info("用户登出请求");
        String token = getTokenFromRequest(request);
        String ip = getClientIp(request);

        userService.logout(token, ip);

        log.info("用户登出成功");
        return ApiResponse.success(true);
    }

    /**
     * @description: 获取用户列表
     * @author: ajiang
     * @date: 2025/6/21 17:39
     * @param: [pageParams, request]
     * @return: com.ajiang.common.model.ApiResponse<com.ajiang.common.model.PageResult<com.ajiang.userservice.entity.User>>
     **/
    @GetMapping("/list")
    public ApiResponse<PageResult<User>> getUserList(PageParams pageParams, HttpServletRequest request) {
        log.info("获取用户列表请求: pageNo={}, pageSize={}", pageParams.getPageNo(), pageParams.getPageSize());
        Long currentUserId = getCurrentUserId(request);
        String ip = getClientIp(request);
        PageResult<User> userList = userService.getUserList(pageParams, currentUserId, ip);
        log.info("获取用户列表成功: 共{}条记录", userList.getCounts());
        return ApiResponse.success(userList);
    }

    /**
     * @description: 获取用户信息
     * @author: ajiang
     * @date: 2025/6/21 17:38
     * @param: [userId, request]
     * @return: com.ajiang.common.model.ApiResponse<com.ajiang.userservice.dto.UserResponseDto>
     **/
    @GetMapping("/{userId}")
    public ApiResponse<UserResponseDto> getUserInfo(@PathVariable Long userId, HttpServletRequest request) {
        log.info("获取用户信息请求: userId={}", userId);
        Long currentUserId = getCurrentUserId(request);
        String ip = getClientIp(request);
        UserResponseDto userInfo = userService.getUserInfo(currentUserId, userId, ip);
        log.info("获取用户信息成功: userId={}", userId);
        return ApiResponse.success(userInfo);
    }

    /**
     * @description: 修改用户信息
     * @author: ajiang
     * @date: 2025/6/21 17:38
     * @param: [userId, user, request]
     * @return: com.ajiang.common.model.ApiResponse<java.lang.Boolean>
     **/
    @PutMapping("/{userId}")
    public ApiResponse<Boolean> updateUser(
            @PathVariable Long userId,
            @RequestBody User user,
            HttpServletRequest request) {
        log.info("修改用户信息请求: userId={}", userId);
        Long currentUserId = getCurrentUserId(request);
        String ip = getClientIp(request);

        // 设置用户ID，确保与路径参数一致
        user.setUserId(userId);

        boolean result = userService.updateUser(userId, user, currentUserId, ip);
        log.info("修改用户信息{}: userId={}", result ? "成功" : "失败", userId);
        return ApiResponse.success(result);
    }

    /**
     * @description: 重置密码
     * @author: ajiang
     * @date: 2025/6/21 17:38
     * @param: [passwordResetDto, request]
     * @return: com.ajiang.common.model.ApiResponse<java.lang.Boolean>
     **/
    @PostMapping("/reset-password")
    public ApiResponse<Boolean> resetPassword(
            @RequestBody @Valid PasswordResetDto passwordResetDto,
            HttpServletRequest request) {
        log.info("重置密码请求: userId={}", passwordResetDto.getUserId());
        Long currentUserId = getCurrentUserId(request);
        String ip = getClientIp(request);

        boolean result = userService.resetPassword(passwordResetDto, currentUserId, ip);
        log.info("重置密码{}: userId={}", result ? "成功" : "失败", passwordResetDto.getUserId());
        return ApiResponse.success(result);
    }

    /**
     * AI生成
     * @description: 从Token中获取UserId
     * @author: ajiang
     * @date: 2025/6/21 17:37
     * @param: [request]
     * @return: java.lang.Long
     **/
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        return jwtUtil.getUserIdFromToken(token);
    }

    /**
     * AI生成
     * @description: 从请求中获取Token
     * @author: ajiang
     * @date: 2025/6/21 17:37
     * @param: [request]
     * @return: java.lang.String
     **/
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new IllegalArgumentException("未提供有效的Token");
    }

    /**
     * AI生成
     * @description: 获取客户端IP
     * @author: ajiang
     * @date: 2025/6/21 17:36
     * @param: [request]
     * @return:
     **/
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}

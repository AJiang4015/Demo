package com.ajiang.userservice.controller;

import com.ajiang.common.exception.BusinessException;
import com.ajiang.common.model.ApiResponse;
import com.ajiang.common.model.PageParams;
import com.ajiang.common.model.PageResult;
import com.ajiang.common.util.JwtUtil;
import com.ajiang.userservice.dto.PasswordResetDto;
import com.ajiang.userservice.dto.UserLoginDto;
import com.ajiang.userservice.dto.UserRegisterDto;
import com.ajiang.userservice.dto.UserResponseDto;
import com.ajiang.userservice.entity.User;
import com.ajiang.userservice.feignclient.PermissionServiceClient;
import com.ajiang.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务控制器
 * 提供用户注册、登录、查询和修改等接口
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;
    /*
     * @Autowired
     * private JwtUtil jwtUtil;
     */

    @Autowired
    private PermissionServiceClient permissionServiceClient;

    /**
     * 用户注册
     * POST /register
     * 分库分表写入用户表 → RPC调用绑定默认角色 → 发送日志消息至MQ
     *
     * @param registerDto 注册信息
     * @param request     HTTP请求
     * @return 用户ID
     */
    @PostMapping("/register")
    public ApiResponse<Long> register(@RequestBody @Valid UserRegisterDto registerDto, HttpServletRequest request) {
        log.info("用户注册请求: {}", registerDto.getUsername());
        String ip = getClientIp(request);
        Long userId = userService.register(registerDto, ip);
        log.info("用户注册成功: {}, userId={}", registerDto.getUsername(), userId);
        return ApiResponse.success(userId);
    }

    /**
     * 用户登录
     * POST /login
     * 校验密码 → 生成Token
     *
     * @param loginDto 登录信息
     * @param request  HTTP请求
     * @return JWT Token
     */
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
     * 用户登出
     * POST /logout
     * 从Redis白名单中移除Token
     *
     * @param request HTTP请求
     * @return 操作结果
     */
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
     * 获取用户列表
     * GET /list
     * 根据权限校验结果返回：
     * - 普通用户仅自己
     * - 管理员所有普通用户
     * - 超管全部
     *
     * @param pageParams 分页参数
     * @param request    HTTP请求
     * @return 分页用户列表
     */
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
     * 获取用户信息
     * GET /{userId}
     * 根据权限校验结果返回：
     * - 普通用户仅自己
     * - 管理员所有普通用户
     * - 超管全部
     *
     * @param userId 用户ID
     * @return 用户信息
     */
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
     * 修改用户信息
     * PUT /{userId}
     * 根据权限限制：
     * - 普通用户改自己
     * - 管理员改普通用户
     * - 超管改所有
     *
     * @param userId  用户ID
     * @param user    用户信息
     * @param request HTTP请求
     * @return 是否成功
     */
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
     * 重置密码
     * POST /reset-password
     * 普通用户重置自己，管理员重置普通用户，超管重置所有人
     *
     * @param passwordResetDto 密码重置信息
     * @param request          HTTP请求
     * @return 是否成功
     */
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
     * 获取当前用户ID
     *
     * @param request HTTP请求
     * @return 当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        return jwtUtil.getUserIdFromToken(token);
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
        throw new IllegalArgumentException("未提供有效的Token");
    }

    /**
     * 获取客户端IP
     *
     * @param request HTTP请求
     * @return 客户端IP
     */
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

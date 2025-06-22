package com.ajiang.permissionservice.controller;

import com.ajiang.common.exception.BusinessException;
import com.ajiang.common.model.ApiResponse;
import com.ajiang.common.model.PageResult;
import com.ajiang.common.util.JwtUtil;
import com.ajiang.permissionservice.serivce.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

/**
 * 权限服务控制器
 * 提供用户角色管理的REST接口
 *
 * @author ajiang
 * @version 1.0
 * @since 2025-06-17
 */
@Slf4j
@RestController
@RequestMapping("/role")
public class RoleController {

    @Autowired
    RoleService roleService;

    @Autowired
    JwtUtil jwtUtil;

    /**
     * @description: 绑定默认角色
     * @author: ajiang
     * @date: 2025/6/22 13:07
     * @param: [userId]
     * @return: com.ajiang.common.model.ApiResponse<java.lang.Void>
     **/
    @PostMapping("/bind/{userId}")
    public ApiResponse<Void> bindDefaultRole(@PathVariable Long userId) {
        log.info("绑定默认角色请求: userId={}", userId);
        roleService.bindDefaultRole(userId);
        log.info("绑定默认角色成功: userId={}", userId);
        return ApiResponse.success();
    }

    /**
     * @description: 查询用户角色码
     * @author: ajiang
     * @date: 2025/6/22 13:07
     * @param: [userId]
     * @return: java.lang.String
     **/
    @GetMapping("/code/{userId}")
    public String getUserRoleCode(@PathVariable Long userId) {
        log.debug("查询用户角色码请求: userId={}", userId);
        String roleCode = roleService.getUserRoleCode(userId);
        log.debug("查询用户角色码成功: userId={}, roleCode={}", userId, roleCode);
        return roleCode;
    }

    /**
     * @description: 升级管理员
     * @author: ajiang
     * @date: 2025/6/22 13:08
     * @param: [userId, request]
     * @return: com.ajiang.common.model.ApiResponse<java.lang.Void>
     **/
    @PostMapping("/upgrade/{userId}")
    public ApiResponse<Void> upgradeToAdmin(@PathVariable Long userId, HttpServletRequest request) {
        log.info("升级用户为管理员请求: userId={}", userId);

        // 从token获取当前用户信息并验证权限
        Long currentUserId = getCurrentUserIdFromToken(request);
        String currentUserRole = getCurrentUserRoleFromToken(request);

        log.info("当前操作用户: userId={}, role={}", currentUserId, currentUserRole);

        roleService.upgradeToAdmin(currentUserId, currentUserRole, userId);
        log.info("升级用户为管理员成功: userId={}", userId);
        return ApiResponse.success();
    }

    /**
     * @description: 降级普通用户
     * @author: ajiang
     * @date: 2025/6/22 13:08
     * @param: [userId, request]
     * @return: com.ajiang.common.model.ApiResponse<java.lang.Void>
     **/
    @PostMapping("/downgrade/{userId}")
    public ApiResponse<Void> downgradeToUser(@PathVariable Long userId, HttpServletRequest request) {
        log.info("降级用户为普通用户请求: userId={}", userId);

        // 从token获取当前用户信息并验证权限
        Long currentUserId = getCurrentUserIdFromToken(request);
        String currentUserRole = getCurrentUserRoleFromToken(request);

        log.info("当前操作用户: userId={}, role={}", currentUserId, currentUserRole);

        roleService.downgradeToUser(currentUserId, currentUserRole, userId);
        log.info("降级用户为普通用户成功: userId={}", userId);
        return ApiResponse.success();
    }

    /**
     * AI
     * @description: 分页查询可见列表
     * @author: ajiang
     * @date: 2025/6/22 13:08
     * @param: [currentUserId, currentUserRole, pageNo, pageSize]
     * @return: com.ajiang.common.model.PageResult<java.lang.Long>
     **/
    @PostMapping("/visible-users")
    public PageResult<Long> getVisibleUserIds(
            @RequestParam Long currentUserId,
            @RequestParam String currentUserRole,
            @RequestParam int pageNo,
            @RequestParam int pageSize) {

        log.info("分页查询可见用户ID列表: currentUserId={}, currentUserRole={}, pageNo={}, pageSize={}",
                currentUserId, currentUserRole, pageNo, pageSize);

        // 参数验证
        if (currentUserId == null || currentUserId <= 0) {
            throw new IllegalArgumentException("当前用户ID不能为空且必须大于0");
        }
        if (currentUserRole == null || currentUserRole.trim().isEmpty()) {
            throw new IllegalArgumentException("当前用户角色不能为空");
        }
        if (pageNo <= 0) {
            throw new IllegalArgumentException("页码必须大于0");
        }
        if (pageSize <= 0 || pageSize > 100) {
            throw new IllegalArgumentException("每页大小必须在1-100之间");
        }

        try {
            PageResult<Long> visibleUserIds = roleService.getVisibleUserIds(currentUserId, currentUserRole, pageNo,
                    pageSize);

            log.info("分页查询可见用户ID列表成功: 返回{}个用户ID, 总数={}",
                    visibleUserIds.getItems().size(), visibleUserIds.getCounts());

            return visibleUserIds;
        } catch (Exception e) {
            log.error("分页查询可见用户ID列表失败: currentUserId={}, currentUserRole={}, 错误: {}",
                    currentUserId, currentUserRole, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * AI
     * 从请求中获取Token
     *
     * @param request HTTP请求对象
     * @return Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new BusinessException("未提供有效的Token");
    }

    /**
     * AI
     * 从token中获取当前用户ID
     *
     * @param request HTTP请求对象
     * @return 当前用户ID
     */
    private Long getCurrentUserIdFromToken(HttpServletRequest request) {
        try {
            String token = getTokenFromRequest(request);
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            log.error("获取当前用户ID失败: {}", e.getMessage());
            throw new BusinessException("获取当前用户信息失败");
        }
    }

    /**
     * AI
     * 从token中获取当前用户角色
     *
     * @param request HTTP请求对象
     * @return 当前用户角色
     */
    private String getCurrentUserRoleFromToken(HttpServletRequest request) {
        try {
            String token = getTokenFromRequest(request);
            return jwtUtil.getRoleCodeFromToken(token);
        } catch (Exception e) {
            log.error("获取当前用户角色失败: {}", e.getMessage());
            throw new BusinessException("获取当前用户角色信息失败");
        }
    }
}

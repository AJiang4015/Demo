package com.ajiang.permissionservice.serivce;

import com.ajiang.common.model.PageResult;
import com.ajiang.permissionservice.entity.Role;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 角色服务接口
 * 提供用户角色管理的业务逻辑接口
 *
 * @author ajiang
 * @version 1.0
 * @since 2025-06-17
 */
public interface RoleService extends IService<Role> {

    /**
     * 绑定默认角色（普通用户）
     *
     * @param userId 用户ID
     */
    void bindDefaultRole(Long userId);

    /**
     * 查询用户角色码
     *
     * @param userId 用户ID
     * @return 角色代码
     */
    String getUserRoleCode(Long userId);

    /**
     * 升级用户为管理员
     * 只有超级管理员可以调用，且只能升级普通用户
     *
     * @param currentUserId   当前操作用户ID
     * @param currentUserRole 当前操作用户角色
     * @param targetUserId    目标用户ID
     */
    void upgradeToAdmin(Long currentUserId, String currentUserRole, Long targetUserId);

    /**
     * 降级用户为普通用户
     * 只有超级管理员可以调用，且只能降级管理员
     *
     * @param currentUserId   当前操作用户ID
     * @param currentUserRole 当前操作用户角色
     * @param targetUserId    目标用户ID
     */
    void downgradeToUser(Long currentUserId, String currentUserRole, Long targetUserId);

    PageResult<Long> getVisibleUserIds(
            @RequestParam Long currentUserId,
            @RequestParam String currentUserRole,
            @RequestParam int pageNo,
            @RequestParam int pageSize);
}

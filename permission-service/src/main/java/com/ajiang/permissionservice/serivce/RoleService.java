package com.ajiang.permissionservice.serivce;

import com.ajiang.permissionservice.entity.Role;
import com.baomidou.mybatisplus.extension.service.IService;

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
     *
     * @param userId 用户ID
     */
    void upgradeToAdmin(Long userId);

    /**
     * 降级用户为普通用户
     *
     * @param userId 用户ID
     */
    void downgradeToUser(Long userId);

}

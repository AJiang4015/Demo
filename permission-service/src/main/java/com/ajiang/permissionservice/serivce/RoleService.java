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

    void bindDefaultRole(Long userId);

    String getUserRoleCode(Long userId);

    void upgradeToAdmin(Long currentUserId, String currentUserRole, Long targetUserId);

    void downgradeToUser(Long currentUserId, String currentUserRole, Long targetUserId);

    PageResult<Long> getVisibleUserIds(
            @RequestParam Long currentUserId,
            @RequestParam String currentUserRole,
            @RequestParam int pageNo,
            @RequestParam int pageSize);
}

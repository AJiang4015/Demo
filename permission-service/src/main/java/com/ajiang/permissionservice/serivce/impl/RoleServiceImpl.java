package com.ajiang.permissionservice.serivce.impl;

import com.ajiang.common.exception.BusinessException;
import com.ajiang.permissionservice.entity.Role;
import com.ajiang.permissionservice.entity.UserRole;
import com.ajiang.permissionservice.mapper.RoleMapper;
import com.ajiang.permissionservice.mapper.UserRoleMapper;
import com.ajiang.permissionservice.serivce.RoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 权限服务实现类
 * 负责用户角色绑定、查询和管理
 *
 * @author ajiang
 * @version 1.0
 * @since 2025-06-17
 */
@Slf4j
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Autowired
    RoleMapper roleMapper;

    @Autowired
    UserRoleMapper userRoleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindDefaultRole(Long userId) {

        // 检查用户是否已经有角色绑定
        LambdaQueryWrapper<UserRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRole::getUserId, userId);
        if (userRoleMapper.selectCount(queryWrapper) > 0) {
            log.info("用户已存在角色绑定");
            return;
        }

        try {
            UserRole userRole = UserRole.builder()
                    .userId(userId)
                    .roleId(2)
                    .build();
            log.debug("插入前 userRole.id={}", userRole.getId());
            if (userRoleMapper.insert(userRole) <= 0) {
                log.error("绑定默认角色失败: userId={}, roleCode={}", userId, "user");
                throw new BusinessException("绑定默认角色失败");
            }

            log.info("绑定默认角色成功: userId={}, roleId={}, roleCode={}", userId, userRole.getRoleId(), "user");
        } catch (Exception e) {

            log.error("绑定默认角色失败: userId={}, 错误: {}", userId, e.getMessage(), e);
            throw new BusinessException("绑定默认角色失败: " + e.getMessage());
        }
    }

    @Override
    public String getUserRoleCode(Long userId) {

        try {
            // 查询用户角色关系
            LambdaQueryWrapper<UserRole> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(UserRole::getRoleId)
                    .eq(UserRole::getUserId, userId);
            UserRole userRole = userRoleMapper.selectOne(queryWrapper);

            if (userRole == null) {
                log.warn("用户未绑定任何角色: userId={}", userId);
                return "user";
            }

            // 查询角色信息
            Role role = roleMapper.selectById(userRole.getRoleId());
            if (role == null) {
                log.error("角色信息不存在: roleId={}", userRole.getRoleId());
                throw new BusinessException("角色信息不存在");
            }

            log.debug("查询用户角色码成功: userId={}, roleCode={}", userId, role.getRoleCode());
            return role.getRoleCode();

        } catch (Exception e) {
            log.error("查询用户角色码失败: userId={}, 错误: {}", userId, e.getMessage(), e);
            throw new BusinessException("查询用户角色码失败: " + e.getMessage());
        }
    }

    private void changeUserRole(Long userId, int roleId) {

            // 1. 检查用户是否存在角色绑定
            UserRole existingUserRole = validateUserRoleBinding(userId);
            // 3. 更新用户角色
            updateUserRoleToTarget(userId, roleId);

    }

    private UserRole validateUserRoleBinding(Long userId) {
        LambdaQueryWrapper<UserRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRole::getUserId, userId);
        UserRole userRole = userRoleMapper.selectOne(queryWrapper);

        if (userRole == null) {
            log.error("用户未绑定任何角色， userId={}", userId);
            throw new BusinessException("用户未绑定任何角色，请先绑定默认角色");
        }

        return userRole;
    }

    private void updateUserRoleToTarget(Long userId, int roleId) {
        LambdaUpdateWrapper<UserRole> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(UserRole::getRoleId, roleId)
                .eq(UserRole::getUserId, userId);

        int result = userRoleMapper.update(null, updateWrapper);
        if (result <= 0) {
            throw new BusinessException("更新用户角色失败");
        }
    }

    @Override
    public void upgradeToAdmin(Long userId) {
        changeUserRole(userId, 3);
    }

    @Override
    public void downgradeToUser(Long userId) {
        changeUserRole(userId, 2);
    }

}

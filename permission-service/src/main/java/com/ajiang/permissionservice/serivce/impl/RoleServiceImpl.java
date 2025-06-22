package com.ajiang.permissionservice.serivce.impl;

import com.ajiang.common.exception.BusinessException;
import com.ajiang.common.model.PageResult;
import com.ajiang.permissionservice.entity.Role;
import com.ajiang.permissionservice.entity.UserRole;
import com.ajiang.permissionservice.mapper.RoleMapper;
import com.ajiang.permissionservice.mapper.UserRoleMapper;
import com.ajiang.permissionservice.serivce.RoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        long startTime = System.currentTimeMillis();
        log.info("[业务开始] 绑定默认角色: userId={}", userId);

        try {
            // 检查用户是否已经有角色绑定
            long queryStartTime = System.currentTimeMillis();
            LambdaQueryWrapper<UserRole> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserRole::getUserId, userId);
            long existingCount = userRoleMapper.selectCount(queryWrapper);
            long queryTime = System.currentTimeMillis() - queryStartTime;
            log.debug("[数据库查询] 检查用户角色绑定: userId={}, count={}, 耗时={}ms",
                    userId, existingCount, queryTime);

            if (existingCount > 0) {
                long totalTime = System.currentTimeMillis() - startTime;
                log.info("[业务结束] 用户已存在角色绑定: userId={}, 总耗时={}ms", userId, totalTime);
                return;
            }

            // 插入默认角色绑定
            long insertStartTime = System.currentTimeMillis();
            UserRole userRole = UserRole.builder()
                    .userId(userId)
                    .roleId(2)
                    .build();

            int insertResult = userRoleMapper.insert(userRole);
            long insertTime = System.currentTimeMillis() - insertStartTime;

            if (insertResult <= 0) {
                log.error("[数据库操作] 绑定默认角色失败: userId={}, roleCode=user, insertResult={}",
                        userId, insertResult);
                throw new BusinessException("绑定默认角色失败");
            }

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("[业务结束] 绑定默认角色成功: userId={}, roleId={}, roleCode=user, 插入耗时={}ms, 总耗时={}ms",
                    userId, userRole.getRoleId(), insertTime, totalTime);

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("[业务异常] 绑定默认角色失败: userId={}, error={}, 总耗时={}ms",
                    userId, e.getMessage(), totalTime, e);
            throw new BusinessException("绑定默认角色失败: " + e.getMessage());
        }
    }

    @Override
    public String getUserRoleCode(Long userId) {
        long startTime = System.currentTimeMillis();
        log.debug("[业务开始] 查询用户角色码: userId={}", userId);

        try {
            // 查询用户角色关系
            long userRoleQueryStart = System.currentTimeMillis();
            LambdaQueryWrapper<UserRole> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(UserRole::getRoleId)
                    .eq(UserRole::getUserId, userId);
            UserRole userRole = userRoleMapper.selectOne(queryWrapper);
            long userRoleQueryTime = System.currentTimeMillis() - userRoleQueryStart;

            log.debug("[数据库查询] 用户角色关系: userId={}, roleId={}, 耗时={}ms",
                    userId, userRole != null ? userRole.getRoleId() : null, userRoleQueryTime);

            if (userRole == null) {
                long totalTime = System.currentTimeMillis() - startTime;
                log.warn("[业务结束] 用户未绑定任何角色，返回默认角色: userId={}, defaultRole=user, 总耗时={}ms",
                        userId, totalTime);
                return "user";
            }

            // 查询角色信息
            long roleQueryStart = System.currentTimeMillis();
            Role role = roleMapper.selectById(userRole.getRoleId());
            long roleQueryTime = System.currentTimeMillis() - roleQueryStart;

            log.debug("[数据库查询] 角色信息: roleId={}, roleCode={}, 耗时={}ms",
                    userRole.getRoleId(), role != null ? role.getRoleCode() : null, roleQueryTime);

            if (role == null) {
                log.error("[数据异常] 角色信息不存在: userId={}, roleId={}", userId, userRole.getRoleId());
                throw new BusinessException("角色信息不存在");
            }

            long totalTime = System.currentTimeMillis() - startTime;
            log.debug("[业务结束] 查询用户角色码成功: userId={}, roleCode={}, 总耗时={}ms",
                    userId, role.getRoleCode(), totalTime);
            return role.getRoleCode();

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("[业务异常] 查询用户角色码失败: userId={}, error={}, 总耗时={}ms",
                    userId, e.getMessage(), totalTime, e);
            throw new BusinessException("查询用户角色码失败: " + e.getMessage());
        }
    }

    private void changeUserRole(Long userId, int roleId) {

        LambdaQueryWrapper<UserRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRole::getUserId, userId);
        UserRole userRole = userRoleMapper.selectOne(queryWrapper);

        if (userRole == null) {
            log.error("用户未绑定任何角色， userId={}", userId);
            throw new BusinessException("用户未绑定任何角色，请先绑定默认角色");
        }

        updateUserRoleToTarget(userId, roleId);

    }

    private void updateUserRoleToTarget(Long userId, int roleId) {
        long startTime = System.currentTimeMillis();
        log.debug("[数据库操作] 更新用户角色: userId={}, targetRoleId={}", userId, roleId);

        LambdaUpdateWrapper<UserRole> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(UserRole::getRoleId, roleId)
                .eq(UserRole::getUserId, userId);

        int result = userRoleMapper.update(null, updateWrapper);
        long updateTime = System.currentTimeMillis() - startTime;

        if (result <= 0) {
            log.error("[数据库操作] 更新用户角色失败: userId={}, targetRoleId={}, updateResult={}, 耗时={}ms",
                    userId, roleId, result, updateTime);
            throw new BusinessException("更新用户角色失败");
        }

        log.debug("[数据库操作] 更新用户角色成功: userId={}, targetRoleId={}, 耗时={}ms",
                userId, roleId, updateTime);
    }

    @Override
    public void upgradeToAdmin(Long currentUserId, String currentUserRole, Long targetUserId) {
        long startTime = System.currentTimeMillis();
        log.info("[业务开始] 升级用户为管理员: currentUserId={}, currentUserRole={}, targetUserId={}",
                currentUserId, currentUserRole, targetUserId);

        try {
            // 1. 验证当前用户是否为超级管理员
            long permissionCheckStart = System.currentTimeMillis();
            validateSuperAdminPermission(currentUserId, currentUserRole);
            long permissionCheckTime = System.currentTimeMillis() - permissionCheckStart;
            log.debug("[权限校验] 超级管理员权限验证通过: currentUserId={}, 耗时={}ms",
                    currentUserId, permissionCheckTime);

            // 2. 验证目标用户当前角色是否为普通用户
            long roleCheckStart = System.currentTimeMillis();
            validateTargetUserRoleForUpgrade(targetUserId);
            long roleCheckTime = System.currentTimeMillis() - roleCheckStart;
            log.debug("[角色校验] 目标用户角色验证通过: targetUserId={}, 耗时={}ms",
                    targetUserId, roleCheckTime);

            // 3. 执行升级操作
            long upgradeStart = System.currentTimeMillis();
            changeUserRole(targetUserId, 3);
            long upgradeTime = System.currentTimeMillis() - upgradeStart;

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("[业务结束] 升级用户为管理员成功: targetUserId={}, 升级耗时={}ms, 总耗时={}ms",
                    targetUserId, upgradeTime, totalTime);

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("[业务异常] 升级用户为管理员失败: targetUserId={}, error={}, 总耗时={}ms",
                    targetUserId, e.getMessage(), totalTime, e);
            throw e;
        }
    }

    @Override
    public void downgradeToUser(Long currentUserId, String currentUserRole, Long targetUserId) {
        long startTime = System.currentTimeMillis();
        log.info("[业务开始] 降级用户为普通用户: currentUserId={}, currentUserRole={}, targetUserId={}",
                currentUserId, currentUserRole, targetUserId);

        try {
            // 1. 验证当前用户是否为超级管理员
            long permissionCheckStart = System.currentTimeMillis();
            validateSuperAdminPermission(currentUserId, currentUserRole);
            long permissionCheckTime = System.currentTimeMillis() - permissionCheckStart;
            log.debug("[权限校验] 超级管理员权限验证通过: currentUserId={}, 耗时={}ms",
                    currentUserId, permissionCheckTime);

            // 2. 验证目标用户当前角色是否为管理员
            long roleCheckStart = System.currentTimeMillis();
            validateTargetUserRoleForDowngrade(targetUserId);
            long roleCheckTime = System.currentTimeMillis() - roleCheckStart;
            log.debug("[角色校验] 目标用户角色验证通过: targetUserId={}, 耗时={}ms",
                    targetUserId, roleCheckTime);

            // 3. 执行降级操作
            long downgradeStart = System.currentTimeMillis();
            changeUserRole(targetUserId, 2);
            long downgradeTime = System.currentTimeMillis() - downgradeStart;

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("[业务结束] 降级用户为普通用户成功: targetUserId={}, 降级耗时={}ms, 总耗时={}ms",
                    targetUserId, downgradeTime, totalTime);

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("[业务异常] 降级用户为普通用户失败: targetUserId={}, error={}, 总耗时={}ms",
                    targetUserId, e.getMessage(), totalTime, e);
            throw e;
        }
    }

    /**
     * AI
     * @description:
     * @author: ajiang
     * @date: 2025/6/22 16:57
     * @param: [currentUserId, currentUserRole, pageNo, pageSize]
     * @return: com.ajiang.common.model.PageResult<java.lang.Long>
     **/
    @Override
    public PageResult<Long> getVisibleUserIds(Long currentUserId, String currentUserRole, int pageNo, int pageSize) {
        log.info("分页查询可见用户ID: currentUserId={}, role={}, pageNo={}, pageSize={}",
                currentUserId, currentUserRole, pageNo, pageSize);

        // 1. 构建基础查询条件
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(UserRole::getUserId); // 只查询userId字段

        // 2. 根据角色过滤
        if ("admin".equals(currentUserRole)) {
            // 管理员：可以查看普通用户(roleId=2)和自己(可能是roleId=3)
            wrapper.and(w -> w.eq(UserRole::getRoleId, 2)
                    .or().eq(UserRole::getUserId, currentUserId));
        } else if ("super_admin".equals(currentUserRole)) {
            // 超管：查看所有用户（无过滤条件）
            log.debug("超管查询所有用户");
        } else {
            // 普通用户：只能查看自己
            wrapper.eq(UserRole::getUserId, currentUserId);
        }

        // 3. 查询所有符合条件的用户ID并去重
        List<UserRole> allUserRoles = userRoleMapper.selectList(wrapper);
        List<Long> allDistinctUserIds = allUserRoles.stream()
                .map(UserRole::getUserId)
                .distinct()
                .sorted() // 排序保证分页的一致性
                .collect(Collectors.toList());

        long totalDistinctUsers = allDistinctUserIds.size();
        log.debug("去重后的总用户数: {}", totalDistinctUsers);

        // 4. 对去重后的用户ID列表进行内存分页
        int startIndex = (pageNo - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allDistinctUserIds.size());

        List<Long> pagedUserIds;
        if (startIndex >= allDistinctUserIds.size()) {
            // 超出范围，返回空列表
            pagedUserIds = new ArrayList<>();
        } else {
            // 获取当前页的用户ID
            pagedUserIds = allDistinctUserIds.subList(startIndex, endIndex);
        }

        // 5. 构建结果
        PageResult<Long> pageResult = new PageResult<>();
        pageResult.setItems(pagedUserIds);
        pageResult.setCounts(totalDistinctUsers);
        pageResult.setPage(pageNo);
        pageResult.setPageSize(pageSize);

        log.info("查询结果: 去重后总数={}, 当前页用户数={}, 页码={}/{}",
                totalDistinctUsers, pagedUserIds.size(), pageNo,
                (totalDistinctUsers + pageSize - 1) / pageSize);
        return pageResult;
    }

    /**
     * 验证超级管理员权限
     *
     * @param currentUserId   当前用户ID
     * @param currentUserRole 当前用户角色
     */
    private void validateSuperAdminPermission(Long currentUserId, String currentUserRole) {
        if (!"super_admin".equals(currentUserRole)) {
            log.error("权限不足，只有超级管理员可以执行此操作: currentUserId={}, currentUserRole={}",
                    currentUserId, currentUserRole);
            throw new BusinessException("权限不足，只有超级管理员可以执行此操作");
        }
    }

    /**
     * 验证目标用户角色是否可以升级（只有普通用户可以升级为管理员）
     *
     * @param targetUserId 目标用户ID
     */
    private void validateTargetUserRoleForUpgrade(Long targetUserId) {
        String targetUserRole = getUserRoleCode(targetUserId);
        if (!"user".equals(targetUserRole)) {
            log.error("目标用户角色不符合升级条件，只有普通用户可以升级为管理员: targetUserId={}, targetUserRole={}",
                    targetUserId, targetUserRole);
            throw new BusinessException("只有普通用户可以升级为管理员，当前用户角色为: " + targetUserRole);
        }
    }

    /**
     * 验证目标用户角色是否可以降级（只有管理员可以降级为普通用户）
     *
     * @param targetUserId 目标用户ID
     */
    private void validateTargetUserRoleForDowngrade(Long targetUserId) {
        String targetUserRole = getUserRoleCode(targetUserId);
        if (!"admin".equals(targetUserRole)) {
            log.error("目标用户角色不符合降级条件，只有管理员可以降级为普通用户: targetUserId={}, targetUserRole={}",
                    targetUserId, targetUserRole);
            throw new BusinessException("只有管理员可以降级为普通用户，当前用户角色为: " + targetUserRole);
        }
    }
}

package com.ajiang.permissionservice.serivce.impl;

import com.ajiang.common.RoleEnum;
import com.ajiang.permissionservice.entity.Role;
import com.ajiang.permissionservice.entity.UserRole;
import com.ajiang.permissionservice.mapper.RoleMapper;
import com.ajiang.permissionservice.mapper.UserRoleMapper;
import com.ajiang.permissionservice.serivce.RoleService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 *@BelongsProject: Demo
 *@BelongsPackage: com.ajiang.permissionservice.serivce.impl
 *@Author: ajiang
 *@CreateTime: 2025-06-17  22:44
 *@Description: TODO
 *@Version: 1.0
 */
@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    RoleMapper roleMapper;

    @Autowired
    UserRoleMapper userRoleMapper;

    @Override
    public void bindDefaultRole(Long userId) {
        LambdaQueryWrapper<UserRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId,2);
        if(userRoleMapper.selectCount(queryWrapper) > 0){
            return;
        }
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(2);
        userRoleMapper.insert(userRole);
    }

    @Override
    public String getUserRoleCode(Long userId) {
        LambdaQueryWrapper<UserRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(UserRole::getRoleId)
                .eq(UserRole::getUserId, userId);
        UserRole userRole = userRoleMapper.selectOne(queryWrapper);
        if(userRole == null){
            return null;
        }

        Role role = roleMapper.selectById(userRole.getRoleId());
        return role.getRoleCode();
    }

    @Override
    public void upgradeToAdmin(Long userId) {
        LambdaUpdateWrapper<UserRole> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(UserRole::getRoleId,3)
                .eq(UserRole::getUserId, userId);
        userRoleMapper.update(null, updateWrapper);
    }

    @Override
    public void downgradeToUser(Long userId) {
        LambdaUpdateWrapper<UserRole> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(UserRole::getRoleId,2)
                .eq(UserRole::getUserId, userId);
        userRoleMapper.update(null, updateWrapper);
    }

    @Override
    public boolean saveBatch(Collection<Role> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<Role> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean updateBatchById(Collection<Role> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean saveOrUpdate(Role entity) {
        return false;
    }

    @Override
    public Role getOne(Wrapper<Role> queryWrapper, boolean throwEx) {
        return null;
    }

    @Override
    public Map<String, Object> getMap(Wrapper<Role> queryWrapper) {
        return Map.of();
    }

    @Override
    public <V> V getObj(Wrapper<Role> queryWrapper, Function<? super Object, V> mapper) {
        return null;
    }

    @Override
    public BaseMapper<Role> getBaseMapper() {
        return null;
    }

    @Override
    public Class<Role> getEntityClass() {
        return null;
    }
}

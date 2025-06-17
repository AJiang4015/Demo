package com.ajiang.userservice.service.impl;

import com.ajiang.common.exception.BusinessException;
import com.ajiang.common.mq.LogMessage;
import com.ajiang.userservice.dto.UserRegisterDto;
import com.ajiang.userservice.entity.User;
import com.ajiang.userservice.feignclient.PermissionServiceClient;
import com.ajiang.userservice.mapper.UserMapper;
import com.ajiang.userservice.mq.LogProducer;
import com.ajiang.userservice.service.UserService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 *@BelongsProject: Demo
 *@BelongsPackage: com.ajiang.userservice.service.impl
 *@Author: ajiang
 *@CreateTime: 2025-06-17  15:54
 *@Description: TODO
 *@Version: 1.0
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;

    @Autowired
    LogProducer logProducer;

    @Autowired
    PermissionServiceClient permissionServiceClient;

    @Override
    @GlobalTransactional(name = "user")
    public Long register(UserRegisterDto registerDto, String ip) {
        log.info("用户注册: {}", registerDto.getUsername());
        long userId = IdWorker.getId();

        User user = new User();
        user.setUserId(userId);
        user.setUsername(registerDto.getUsername());
        user.setPassword(registerDto.getPassword());
        user.setEmail(registerDto.getEmail());
        user.setPhone(registerDto.getPhone());
        user.setGmtCreate(LocalDateTime.now());

        userMapper.insert(user);

        try {
            permissionServiceClient.bindDefaultRole(user.getUserId());
            log.info("用户绑定默认角色成功: {}", user.getUserId());
        } catch (Exception e){
            log.error("用户绑定默认角色失败: {}, 错误: {}", user.getUserId(), e.getMessage());
            throw new BusinessException("绑定默认角色失败：" + e.getMessage());
        }

        LogMessage logMessage = new LogMessage();
        logMessage.setUserId(user.getUserId());
        logMessage.setAction("REGISTER");
        logMessage.setIp(ip);
        logMessage.setDetail("用户注册");
        logProducer.sendRegisterLog(logMessage);

        return userId;
    }


    @Override
    public boolean saveBatch(Collection<User> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<User> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean updateBatchById(Collection<User> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean saveOrUpdate(User entity) {
        return false;
    }

    @Override
    public User getOne(Wrapper<User> queryWrapper, boolean throwEx) {
        return null;
    }

    @Override
    public Map<String, Object> getMap(Wrapper<User> queryWrapper) {
        return Collections.emptyMap();
    }

    @Override
    public <V> V getObj(Wrapper<User> queryWrapper, Function<? super Object, V> mapper) {
        return null;
    }

    @Override
    public BaseMapper<User> getBaseMapper() {
        return null;
    }

    @Override
    public Class<User> getEntityClass() {
        return null;
    }
}

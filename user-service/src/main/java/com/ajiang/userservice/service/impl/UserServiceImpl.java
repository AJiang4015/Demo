package com.ajiang.userservice.service.impl;

import com.ajiang.common.exception.BusinessException;
import com.ajiang.common.model.PageParams;
import com.ajiang.common.model.PageResult;
import com.ajiang.common.util.JwtUtil;
import com.ajiang.userservice.dto.PasswordResetDto;
import com.ajiang.userservice.dto.UserLoginDto;
import com.ajiang.userservice.dto.UserRegisterDto;
import com.ajiang.userservice.dto.UserResponseDto;
import com.ajiang.userservice.entity.User;
import com.ajiang.userservice.feignclient.PermissionServiceClient;
import com.ajiang.userservice.mapper.UserMapper;
import com.ajiang.userservice.mq.LogProducer;
import com.ajiang.userservice.service.UserService;
import com.ajiang.userservice.util.SeataTransactionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.ajiang.common.config.AppConfig.SimplePasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * 实现用户注册、登录、查询和修改等功能
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PermissionServiceClient permissionServiceClient;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SimplePasswordEncoder passwordEncoder;

    @Autowired
    private LogProducer logProducer;

    /**
     * @description: 用户注册
     * @author: ajiang
     * @date: 2025/6/18 15:09
     * @param: [registerDto, ip]
     * @return: 用户ID
     **/
    @Override
    @GlobalTransactional(timeoutMills = 300000, name = "user-register-tx", rollbackFor = Exception.class)
    public Long register(UserRegisterDto registerDto, String ip) {
        SeataTransactionUtil.logTransactionStart("用户注册");
        log.info("用户注册: {}", registerDto.getUsername());

        // 检查用户名是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, registerDto.getUsername());
        if (userMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 创建用户实体
        User user = User.builder()
                .username(registerDto.getUsername())
                .password(passwordEncoder.encode(registerDto.getPassword()))
                .email(registerDto.getEmail())
                .phone(registerDto.getPhone())
                .gmtCreate(LocalDateTime.now())
                .build();

        // 保存用户
        userMapper.insert(user);
        Long userId = user.getUserId();

        try {
            // RPC调用权限服务绑定默认角色
            SeataTransactionUtil.logCurrentXid("调用权限服务绑定角色");
            permissionServiceClient.bindDefaultRole(userId);
            log.info("用户绑定默认角色成功: {}", userId);
        } catch (Exception e) {
            log.error("用户绑定默认角色失败: {}, 错误: {}", userId, e.getMessage());
            SeataTransactionUtil.logTransactionFailure("用户注册", e);
            throw new BusinessException("绑定默认角色失败：" + e.getMessage());
        }

        // 发送用户注册日志到MQ
        logProducer.sendUserRegisterLog(userId, user.getUsername(), user.getEmail(), user.getPhone(), ip);

        SeataTransactionUtil.logTransactionSuccess("用户注册");
        log.info("用户注册成功: {}, 用户ID: {}", registerDto.getUsername(), userId);
        return userId;
    }

    /**
     * @description: 用户登录
     * @author: ajiang
     * @date: 2025/6/18 15:08
     * @param: [loginDto, ip]
     * @return: token
     **/
    @Override
    public String login(UserLoginDto loginDto, String ip) {
        log.info("用户登录: {}", loginDto.getUsername());

        // 查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginDto.getUsername());
        User user = userMapper.selectOne(queryWrapper);

        // 验证用户存在且密码正确
        if (user == null) {
            log.warn("用户不存在: {}", loginDto.getUsername());
            throw new BusinessException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            log.warn("密码错误: {}", loginDto.getUsername());
            throw new BusinessException("用户名或密码错误");
        }

        // 生成JWT Token
        String token = jwtUtil.generateToken(user.getUserId(), "user");

        // 发送用户登录日志到MQ
        logProducer.sendUserLoginLog(user.getUserId(), user.getUsername(), ip);

        log.info("用户登录成功: {}", user.getUsername());
        return token;
    }

    /**
     * 获取用户信息
     *
     * @param currentUserId 当前用户ID
     * @param userId        目标用户ID
     * @param ip            客户端IP
     * @return 用户信息
     */
    @Override
    public UserResponseDto getUserInfo(Long currentUserId, Long userId, String ip) {

        // 查询目标用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("用户不存在: {}", userId);
            throw new BusinessException("用户不存在");
        }

        // 查询当前用户的角色码
        String currentUserRole = permissionServiceClient.getUserRoleCode(currentUserId);

        // 权限校验
        switch (currentUserRole) {
            case "super_admin":
                // 超管直接通过
                break;
            case "admin":
                if (currentUserId.equals(userId)) {
                    break;
                }
                // 管理员只能查看普通用户
                String targetRole = permissionServiceClient.getUserRoleCode(userId);
                if (!"user".equals(targetRole)) {
                    log.warn("管理员无权查看非普通用户信息: currentUserId={}, targetUserId={}", currentUserId, userId);
                    throw new BusinessException("权限不足，无法查看该用户信息");
                }
                break;
            case "user":
                // 普通用户只能看自己
                if (!currentUserId.equals(userId)) {
                    log.warn("普通用户无权查看他人信息: currentUserId={}, targetUserId={}", currentUserId, userId);
                    throw new BusinessException("权限不足，无法查看该用户信息");
                }
                break;
            default:
                log.error("未知角色: {}", currentUserRole);
                throw new BusinessException("非法用户角色");
        }

        // 构建返回对象
        UserResponseDto dto = new UserResponseDto();
        BeanUtils.copyProperties(user, dto);

        // 查询目标用户的角色并设置
        String targetRoleCode = permissionServiceClient.getUserRoleCode(userId);
        dto.setRoleCode(targetRoleCode);

        // 发送查看用户信息日志到MQ
        logProducer.sendUserInfoViewLog(currentUserId, user.getUsername(), userId, ip);

        return dto;
    }

    /**
     * @description: 获取用户列表
     * @author: ajiang
     * @date: 2025/6/18 15:12
     * @param: [pageParams, currentUserId, ip]
     * @return: 分页用户列表
     **/
    @Override
    public PageResult<User> getUserList(PageParams pageParams, Long currentUserId, String ip) {
        // 1. 获取当前用户角色
        String currentUserRole = permissionServiceClient.getUserRoleCode(currentUserId);
        log.info("当前用户角色: {}, userId={}", currentUserRole, currentUserId);

        // 2. 通过当前用户ID查出其所在的表（通过 user_id 精确查询，自动路由）
        User currentUser = this.getById(currentUserId);
        if (currentUser == null) {
            throw new BusinessException("当前用户不存在");
        }

        // 3. 确定当前用户所在分表编号（与 ShardingSphere 分片规则一致）
        int userTableIndex = (int) (currentUserId % 2); // 假设按 user_id % 2 分片

        // 4. 构建分页参数
        Page<User> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());

        // 5. 构建分页查询条件
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 强制限定仅在“当前用户所在分表”进行分页
        wrapper.apply("MOD(user_id, 2) = {0}", userTableIndex);

        // 根据角色控制可见范围
        switch (currentUserRole) {
            case "user":
                // 普通用户仅查看自己
                wrapper.eq(User::getUserId, currentUserId);
                break;
            case "admin":
                // 管理员：仅查看本分表中其他普通用户 + 自己
                // 后处理过滤，仅保留 user 角色或自己
                break;
            case "super_admin":
                // 超级管理员：仅查看本分表中的所有人（无需过滤）
                break;
            default:
                throw new BusinessException("未知用户角色: " + currentUserRole);
        }

        // 6. 执行分页查询
        Page<User> userPage = this.page(page, wrapper);
        List<User> allUsers = userPage.getRecords();

        // 7. 管理员角色需要进行后置过滤
        List<User> filteredUsers;
        if ("admin".equals(currentUserRole)) {
            filteredUsers = allUsers.stream()
                    .filter(user -> {
                        // 自己总能查看
                        if (user.getUserId().equals(currentUserId)) {
                            return true;
                        }
                        try {
                            // 只允许查看普通用户
                            String role = permissionServiceClient.getUserRoleCode(user.getUserId());
                            return "user".equals(role);
                        } catch (Exception e) {
                            log.warn("获取用户 {} 角色失败: {}", user.getUserId(), e.getMessage());
                            return false;
                        }
                    }).collect(Collectors.toList());
        } else {
            filteredUsers = allUsers; // 普通用户和超级管理员直接用原数据
        }

        // 8. 构建分页响应结果
        PageResult<User> result = new PageResult<>();
        result.setItems(filteredUsers);
        result.setCounts(filteredUsers.size());
        result.setPage(userPage.getCurrent());
        result.setPageSize(userPage.getSize());

        // 9. 记录日志
        logProducer.sendUserListViewLog(currentUserId, currentUserRole, pageParams.getPageNo(),
                pageParams.getPageSize(), filteredUsers.size(), ip);

        return result;
    }


    /**
     * @description: 修改用户消息
     * @author: ajiang
     * @date: 2025/6/18 15:14
     * @param: [userId, user, currentUserId, ip]
     * @return: boolean
     **/
    @Override
    @Transactional
    public boolean updateUser(Long userId, User user, Long currentUserId, String ip) {
        // 检查用户是否存在
        User existingUser = this.getById(userId);
        if (existingUser == null) {
            throw new BusinessException("用户不存在");
        }

        // 获取当前用户角色
        String currentUserRoleCode = permissionServiceClient.getUserRoleCode(currentUserId);
        // 获取目标用户角色
        String targetUserRoleCode = permissionServiceClient.getUserRoleCode(userId);

        // 权限校验
        if (!hasPermissionToModify(currentUserRoleCode, targetUserRoleCode, currentUserId, userId)) {
            throw new BusinessException("权限不足，无法修改该用户信息");
        }

        // 记录修改前的信息，用于日志
        Map<String, Object> changes = new HashMap<>();
        if (!existingUser.getEmail().equals(user.getEmail())) {
            Map<String, String> emailChange = new HashMap<>();
            emailChange.put("old", existingUser.getEmail());
            emailChange.put("new", user.getEmail());
            changes.put("email", emailChange);
        }
        if (!existingUser.getPhone().equals(user.getPhone())) {
            Map<String, String> phoneChange = new HashMap<>();
            phoneChange.put("old", existingUser.getPhone());
            phoneChange.put("new", user.getPhone());
            changes.put("phone", phoneChange);
        }

        // 更新用户信息（只允许修改邮箱和手机号）
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUserId, userId)
                .set(User::getEmail, user.getEmail())
                .set(User::getPhone, user.getPhone());
        boolean result = userMapper.update(null, updateWrapper) > 0;

        // 发送操作日志
        if (result && !changes.isEmpty()) {
            logProducer.sendUserUpdateLog(currentUserId, existingUser.getUsername(), ip, changes);
        }

        return result;
    }

    /**
     * 重置密码
     * 普通用户重置自己，管理员重置普通用户，超管重置所有人
     *
     * @param passwordResetDto 密码重置信息
     * @param currentUserId    当前用户ID
     * @param ip               客户端IP
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean resetPassword(PasswordResetDto passwordResetDto, Long currentUserId, String ip) {
        Long targetUserId = passwordResetDto.getUserId();

        // 检查用户是否存在
        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException("用户不存在");
        }

        // 获取当前用户角色
        String currentUserRoleCode = permissionServiceClient.getUserRoleCode(currentUserId);
        // 获取目标用户角色
        String targetUserRoleCode = permissionServiceClient.getUserRoleCode(targetUserId);
        // 权限校验
        if (!hasPermissionToModify(currentUserRoleCode, targetUserRoleCode, currentUserId, targetUserId)) {
            throw new BusinessException("权限不足，无法重置该用户密码");
        }

        // 更新密码
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUserId, targetUserId)
                .set(User::getPassword, passwordEncoder.encode(passwordResetDto.getNewPassword()));
        boolean result = userMapper.update(null, updateWrapper) > 0;

        // 发送操作日志
        if (result) {
            logProducer.sendPasswordResetLog(currentUserId, targetUser.getUsername(), ip);
        }

        return result;
    }

    private boolean hasPermissionToModify(String currentUserRoleCode, String targetUserRoleCode,
                                          Long currentUserId, Long targetUserId) {
        if ("super_admin".equals(currentUserRoleCode)) {
            return true;
        }

        if ("admin".equals(currentUserRoleCode) && "user".equals(targetUserRoleCode)) {
            return true;
        }

        if ("user".equals(currentUserRoleCode) && currentUserId != null && currentUserId.equals(targetUserId)) {
            return true;
        }

        return false;
    }

}

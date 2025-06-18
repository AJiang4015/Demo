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
import com.ajiang.userservice.mapper.UserMapper;
import com.ajiang.userservice.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import com.ajiang.common.config.AppConfig.SimplePasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/**
 * 用户服务实现类
 * 实现用户注册、登录、查询和修改等功能
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    /*
     * @Autowired
     * private LogProducer logProducer;
     * 
     * @Autowired
     * private PermissionServiceClient permissionServiceClient;
     */

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SimplePasswordEncoder passwordEncoder;

    /**
     * @description: 用户注册
     * @author: ajiang
     * @date: 2025/6/18 15:09
     * @param: [registerDto, ip]
     * @return: 用户ID
     **/
    @Override
    public Long register(UserRegisterDto registerDto, String ip) {
        log.info("用户注册: {}", registerDto.getUsername());

        // 检查用户名是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, registerDto.getUsername());
        if (this.count(queryWrapper) > 0) {
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
        this.save(user);
        Long userId = user.getUserId();

        /*
         * try {
         * // RPC调用权限服务绑定默认角色
         * permissionServiceClient.bindDefaultRole(userId);
         * log.info("用户绑定默认角色成功: {}", userId);
         * } catch (Exception e) {
         * log.error("用户绑定默认角色失败: {}, 错误: {}", userId, e.getMessage());
         * throw new BusinessException("绑定默认角色失败：" + e.getMessage());
         * }
         * 
         * // 发送操作日志至MQ
         * Map<String, Object> detail = new HashMap<>();
         * detail.put("username", user.getUsername());
         * detail.put("email", user.getEmail());
         * detail.put("phone", user.getPhone());
         * 
         * LogMessage logMessage = new LogMessage(
         * userId,
         * "REGISTER",
         * ip,
         * JSON.toJSONString(detail));
         * logProducer.sendRegisterLog(logMessage);
         * 
         * log.info("用户注册成功: {}", user.getUsername());
         */
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
        User user = this.getOne(queryWrapper);

        // 验证用户存在且密码正确
        if (user == null) {
            log.warn("用户不存在: {}", loginDto.getUsername());
            throw new BusinessException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            log.warn("密码错误: {}", loginDto.getUsername());
            throw new BusinessException("用户名或密码错误");
        }

        /*
         * // 获取用户角色
         * String roleCode;
         * try {
         * roleCode = permissionServiceClient.getUserRoleCode(user.getUserId());
         * log.info("获取用户角色成功: {}, 角色: {}", user.getUsername(), roleCode);
         * } catch (Exception e) {
         * log.error("获取用户角色失败: {}, 错误: {}", user.getUsername(), e.getMessage());
         * throw new BusinessException("获取用户角色失败: " + e.getMessage());
         * }
         */

        // 生成JWT Token
        String token = jwtUtil.generateToken(user.getUserId(), "user");

        /*
         * // 发送登录日志
         * LogMessage logMessage = new LogMessage(
         * user.getUserId(),
         * "LOGIN",
         * ip,
         * "{\"username\":\"" + user.getUsername() + "\"}" // 简单记录登录用户名
         * );
         * logProducer.sendRegisterLog(logMessage);
         * 
         * log.info("用户登录成功: {}", user.getUsername());
         */
        return token;
    }

    /**
     * 获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @Override
    public UserResponseDto getUserInfo(Long userId) {
        // 查询用户
        User user = this.getById(userId);
        if (user == null) {
            log.warn("用户不存在: {}", userId);
            throw new BusinessException("用户不存在");
        }

        /*
         * // 获取用户角色
         * String roleCode;
         * try {
         * roleCode = permissionServiceClient.getUserRoleCode(userId);
         * log.info("获取用户角色码成功: {}, 角色码: {}", userId, roleCode);
         * } catch (Exception e) {
         * log.error("获取用户角色码失败: {}, 错误: {}", userId, e.getMessage());
         * throw new BusinessException("获取用户角色失败: " + e.getMessage());
         * }
         */

        // 转换为响应DTO
        return UserResponseDto.fromUser(user, "user");
    }

    /**
     * @description: 获取用户列表
     * @author: ajiang
     * @date: 2025/6/18 15:12
     * @param: [pageParams, currentUserId]
     * @return: 分页用户列表
     **/
    @Override
    public PageResult<User> getUserList(PageParams pageParams, Long currentUserId) {
        // 获取当前用户角色
        String roleCode;
        /*
         * try {
         * roleCode = permissionServiceClient.getUserRoleCode(currentUserId);
         * log.info("获取用户角色成功: {}, 角色: {}", currentUserId, roleCode);
         * } catch (Exception e) {
         * log.error("获取用户角色失败: {}, 错误: {}", currentUserId, e.getMessage());
         * throw new BusinessException("获取用户角色失败: " + e.getMessage());
         * }
         */

        // 创建分页对象
        Page<User> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        // 根据角色权限过滤数据
        /*
         * if ("super_admin".equals(roleCode)) {
         * // 超管可查看所有用户
         * log.info("超级管理员查询所有用户");
         * } else if ("admin".equals(roleCode)) {
         * // 管理员可查看所有普通用户，但不能查看超管
         * log.info("管理员查询普通用户");
         * queryWrapper.
         * notExists("SELECT 1 FROM user_roles ur JOIN roles r ON ur.role_id = r.role_id "
         * +
         * "WHERE ur.user_id = users.user_id AND r.role_code = 'super_admin'");
         * } else {
         * // 普通用户只能查看自己
         * log.info("普通用户只查询自己: {}", currentUserId);
         * queryWrapper.eq(User::getUserId, currentUserId);
         * }
         */

        // 执行分页查询
        Page<User> userPage = this.page(page, queryWrapper);

        // 转换为分页结果
        PageResult<User> pageResult = new PageResult<>();
        pageResult.setItems(userPage.getRecords());
        pageResult.setCounts(userPage.getTotal());
        pageResult.setPage(userPage.getCurrent());
        pageResult.setPageSize(userPage.getSize());

        return pageResult;
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

        /*
         * // 获取当前用户角色
         * String currentUserRoleCode =
         * permissionServiceClient.getUserRoleCode(currentUserId);
         * // 获取目标用户角色
         * String targetUserRoleCode = permissionServiceClient.getUserRoleCode(userId);
         * 
         * // 权限校验
         * if (!hasPermissionToModify(currentUserRoleCode, targetUserRoleCode,
         * currentUserId, userId)) {
         * throw new BusinessException("权限不足，无法修改该用户信息");
         * }
         */

        /*
         * // 记录修改前的信息，用于日志
         * Map<String, Object> detail = new HashMap<>();
         * if (!existingUser.getEmail().equals(user.getEmail())) {
         * Map<String, String> emailChange = new HashMap<>();
         * emailChange.put("old", existingUser.getEmail());
         * emailChange.put("new", user.getEmail());
         * detail.put("email", emailChange);
         * }
         * if (!existingUser.getPhone().equals(user.getPhone())) {
         * Map<String, String> phoneChange = new HashMap<>();
         * phoneChange.put("old", existingUser.getPhone());
         * phoneChange.put("new", user.getPhone());
         * detail.put("phone", phoneChange);
         * }
         */
        // 更新用户信息（只允许修改邮箱和手机号）
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUserId, userId)
                .set(User::getEmail, user.getEmail())
                .set(User::getPhone, user.getPhone());
        boolean result = this.update(updateWrapper);

        /*
         * // 发送操作日志
         * if (result && !detail.isEmpty()) {
         * LogMessage logMessage = new LogMessage(
         * currentUserId,
         * "UPDATE_USER",
         * ip,
         * JSON.toJSONString(detail));
         * logProducer.sendRegisterLog(logMessage);
         * }
         */

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
        User targetUser = this.getById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException("用户不存在");
        }

        /*
         * // 获取当前用户角色
         * String currentUserRoleCode =
         * permissionServiceClient.getUserRoleCode(currentUserId);
         * // 获取目标用户角色
         * String targetUserRoleCode =
         * permissionServiceClient.getUserRoleCode(targetUserId);
         * 
         * // 权限校验
         * if (!hasPermissionToModify(currentUserRoleCode, targetUserRoleCode,
         * currentUserId, targetUserId)) {
         * throw new BusinessException("权限不足，无法重置该用户密码");
         * }
         */
        // 更新密码
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUserId, targetUserId)
                .set(User::getPassword, passwordEncoder.encode(passwordResetDto.getNewPassword()));
        boolean result = this.update(updateWrapper);

        /*
         * // 发送操作日志
         * if (result) {
         * LogMessage logMessage = new LogMessage(
         * currentUserId,
         * "RESET_PASSWORD",
         * ip,
         * "{\"targetUserId\":\"" + targetUserId + "\"}" // 记录被重置密码的用户ID
         * );
         * logProducer.sendRegisterLog(logMessage);
         * }
         */
        return result;
    }
}

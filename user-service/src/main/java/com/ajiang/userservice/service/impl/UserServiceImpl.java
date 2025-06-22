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
import com.ajiang.userservice.service.TokenWhitelistService;
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
import java.util.*;
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

    @Autowired
    private TokenWhitelistService tokenWhitelistService;

    /**
     * @description: 用户注册
     * @author: ajiang
     * @date: 2025/6/18 15:09
     * @param: [registerDto, ip]
     * @return: 用户ID
     **/
    @Override
    @GlobalTransactional(timeoutMills = 10000, name = "user-register-tx", rollbackFor = Exception.class)
    public Long register(UserRegisterDto registerDto, String ip) {
        long startTime = System.currentTimeMillis();
        SeataTransactionUtil.logTransactionStart("用户注册");
        log.info("[业务开始] 用户注册: username={}, email={}, phone={}, ip={}",
                registerDto.getUsername(), registerDto.getEmail(), registerDto.getPhone(), ip);

        // 检查用户名是否已存在
        log.debug("[数据校验] 检查用户名是否存在: {}", registerDto.getUsername());
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, registerDto.getUsername());
        long dbStartTime = System.currentTimeMillis();
        long userCount = userMapper.selectCount(queryWrapper);
        long dbEndTime = System.currentTimeMillis();
        log.debug("[数据库操作] 用户名查询完成: username={}, count={}, 耗时={}ms",
                registerDto.getUsername(), userCount, (dbEndTime - dbStartTime));

        if (userCount > 0) {
            log.warn("[业务异常] 用户名已存在: username={}, ip={}", registerDto.getUsername(), ip);
            throw new BusinessException("用户名已存在");
        }

        // 创建用户
        log.debug("[业务处理] 开始创建用户对象: username={}", registerDto.getUsername());
        User user = new User();
        user.setUsername(registerDto.getUsername());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setEmail(registerDto.getEmail());
        user.setPhone(registerDto.getPhone());
        user.setGmtCreate(LocalDateTime.now());

        dbStartTime = System.currentTimeMillis();
        userMapper.insert(user);
        dbEndTime = System.currentTimeMillis();
        log.info("[数据库操作] 用户创建成功: userId={}, username={}, 耗时={}ms",
                user.getUserId(), user.getUsername(), (dbEndTime - dbStartTime));

        // 绑定默认角色
        log.debug("[远程调用] 开始绑定默认角色: userId={}", user.getUserId());
        try {
            long rpcStartTime = System.currentTimeMillis();
            permissionServiceClient.bindDefaultRole(user.getUserId());
            long rpcEndTime = System.currentTimeMillis();
            log.info("[远程调用] 绑定默认角色成功: userId={}, 耗时={}ms",
                    user.getUserId(), (rpcEndTime - rpcStartTime));
        } catch (Exception e) {
            log.error("[远程调用] 绑定默认角色失败: userId={}, error={}, cause={}",
                    user.getUserId(), e.getMessage(), e.getCause());
            throw new BusinessException("用户注册失败：角色绑定异常");
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("[业务完成] 用户注册成功: userId={}, username={}, 总耗时={}ms",
                user.getUserId(), user.getUsername(), totalTime);
        SeataTransactionUtil.logTransactionSuccess("用户注册");

        try {
            long mqStartTime = System.currentTimeMillis();
            logProducer.sendUserRegisterLog(user.getUserId(), user.getUsername(), user.getEmail(), user.getPhone(), ip);
            long mqEndTime = System.currentTimeMillis();
            log.debug("[消息队列] 注册日志发送完成: userId={}, 耗时={}ms",
                    user.getUserId(), (mqEndTime - mqStartTime));
        } catch (Exception e) {
            log.error("[消息队列] 注册日志发送失败: userId={}, error={}",
                    user.getUserId(), e.getMessage(), e);
        }
        return user.getUserId();
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
        long startTime = System.currentTimeMillis();
        log.info("[业务开始] 用户登录: username={}, ip={}", loginDto.getUsername(), ip);

        // 查询用户
        log.debug("[数据校验] 查询用户信息: username={}", loginDto.getUsername());
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginDto.getUsername());

        long dbStartTime = System.currentTimeMillis();
        User user = userMapper.selectOne(queryWrapper);
        long dbEndTime = System.currentTimeMillis();
        log.debug("[数据库操作] 用户查询完成: username={}, found={}, 耗时={}ms",
                loginDto.getUsername(), (user != null), (dbEndTime - dbStartTime));

        if (user == null) {
            log.warn("[业务异常] 用户不存在: username={}, ip={}", loginDto.getUsername(), ip);
            throw new BusinessException("用户名或密码错误");
        }

        // 验证密码
        log.debug("[业务处理] 验证用户密码: userId={}", user.getUserId());
        long pwdStartTime = System.currentTimeMillis();
        boolean passwordMatch = passwordEncoder.matches(loginDto.getPassword(), user.getPassword());
        long pwdEndTime = System.currentTimeMillis();
        log.debug("[业务处理] 密码验证完成: userId={}, match={}, 耗时={}ms",
                user.getUserId(), passwordMatch, (pwdEndTime - pwdStartTime));

        if (!passwordMatch) {
            log.warn("[业务异常] 密码错误: username={}, userId={}, ip={}",
                    loginDto.getUsername(), user.getUserId(), ip);
            throw new BusinessException("用户名或密码错误");
        }

        // 获取用户角色
        log.debug("[远程调用] 获取用户角色: userId={}", user.getUserId());
        long roleStartTime = System.currentTimeMillis();
        String roleCode = permissionServiceClient.getUserRoleCode(user.getUserId());
        long roleEndTime = System.currentTimeMillis();
        log.debug("[远程调用] 用户角色获取完成: userId={}, roleCode={}, 耗时={}ms",
                user.getUserId(), roleCode, (roleEndTime - roleStartTime));

        // 生成JWT Token
        log.debug("[业务处理] 生成JWT Token: userId={}", user.getUserId());
        long jwtStartTime = System.currentTimeMillis();
        String token = jwtUtil.generateToken(user.getUserId(), roleCode);
        long jwtEndTime = System.currentTimeMillis();
        log.debug("[业务处理] JWT Token生成完成: userId={}, tokenLength={}, 耗时={}ms",
                user.getUserId(), token.length(), (jwtEndTime - jwtStartTime));

        // 将token添加到Redis白名单，设置过期时间为半小时（1800秒）
        log.debug("[缓存操作] 添加Token到白名单: userId={}", user.getUserId());
        long cacheStartTime = System.currentTimeMillis();
        long expireSeconds = 30 * 60;
        tokenWhitelistService.addTokenToWhitelist(token, user.getUserId(), expireSeconds);
        long cacheEndTime = System.currentTimeMillis();
        log.debug("[缓存操作] Token白名单添加完成: userId={}, expireSeconds={}, 耗时={}ms",
                user.getUserId(), expireSeconds, (cacheEndTime - cacheStartTime));

        // 发送用户登录日志到MQ
        log.debug("[消息队列] 发送登录日志: userId={}", user.getUserId());
        try {
            long mqStartTime = System.currentTimeMillis();
            logProducer.sendUserLoginLog(user.getUserId(), user.getUsername(), ip);
            long mqEndTime = System.currentTimeMillis();
            log.debug("[消息队列] 登录日志发送完成: userId={}, 耗时={}ms",
                    user.getUserId(), (mqEndTime - mqStartTime));
        } catch (Exception e) {
            log.error("[消息队列] 登录日志发送失败: userId={}, error={}",
                    user.getUserId(), e.getMessage(), e);
            // 日志发送失败不影响登录流程
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("[业务完成] 用户登录成功: userId={}, username={}, 总耗时={}ms",
                user.getUserId(), user.getUsername(), totalTime);
        return token;
    }

    /**
     * @description: 用户登出
     * @author: ajiang
     * @date: 2025/1/27 10:30
     * @param: [token, ip]
     * @return: void
     **/
    @Override
    public void logout(String token, String ip) {
        long startTime = System.currentTimeMillis();
        log.info("[业务开始] 用户登出请求: ip={}", ip);

        if (token == null || token.trim().isEmpty()) {
            log.warn("[业务异常] 无效的token: ip={}", ip);
            throw new BusinessException("无效的token");
        }

        try {
            // 从token中解析用户信息
            log.debug("[业务处理] 解析Token信息");
            long parseStartTime = System.currentTimeMillis();
            Long userId = jwtUtil.getUserIdFromToken(token);
            long parseEndTime = System.currentTimeMillis();
            log.debug("[业务处理] Token解析完成: userId={}, 耗时={}ms",
                    userId, (parseEndTime - parseStartTime));

            // 检查token是否在白名单中
            log.debug("[缓存操作] 检查Token白名单: userId={}", userId);
            long checkStartTime = System.currentTimeMillis();
            if (!tokenWhitelistService.isTokenInWhitelist(token)) {
                log.warn("[业务异常] 尝试登出无效token: userId={}", userId);
                throw new BusinessException("无效的token");
            }
            long checkEndTime = System.currentTimeMillis();
            log.debug("[缓存操作] Token白名单检查完成: userId={}, 耗时={}ms",
                    userId, (checkEndTime - checkStartTime));

            // 从Redis白名单中移除token
            log.debug("[缓存操作] 从白名单移除Token: userId={}", userId);
            long cacheStartTime = System.currentTimeMillis();
            tokenWhitelistService.removeTokenFromWhitelist(token);
            long cacheEndTime = System.currentTimeMillis();
            log.debug("[缓存操作] Token移除完成: userId={}, 耗时={}ms",
                    userId, (cacheEndTime - cacheStartTime));

            // 查询用户信息用于日志记录
            log.debug("[数据库操作] 查询用户信息: userId={}", userId);
            long dbStartTime = System.currentTimeMillis();
            User user = userMapper.selectById(userId);
            String username = user != null ? user.getUsername() : "unknown";
            long dbEndTime = System.currentTimeMillis();
            log.debug("[数据库操作] 用户信息查询完成: userId={}, username={}, 耗时={}ms",
                    userId, username, (dbEndTime - dbStartTime));

            // 发送用户登出日志到MQ
            log.debug("[消息队列] 发送登出日志: userId={}", userId);
            try {
                long mqStartTime = System.currentTimeMillis();
                logProducer.sendUserLogoutLog(userId, username, ip);
                long mqEndTime = System.currentTimeMillis();
                log.debug("[消息队列] 登出日志发送完成: userId={}, 耗时={}ms",
                        userId, (mqEndTime - mqStartTime));
            } catch (Exception e) {
                log.error("[消息队列] 登出日志发送失败: userId={}, error={}",
                        userId, e.getMessage(), e);
                // 日志发送失败不影响登出流程
            }

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("[业务完成] 用户登出成功: userId={}, username={}, 总耗时={}ms",
                    userId, username, totalTime);
        } catch (Exception e) {
            log.error("[业务异常] 用户登出失败: error={}, ip={}", e.getMessage(), ip, e);
            throw new BusinessException("登出失败: " + e.getMessage());
        }
    }

    /**
     * @description: 获取用户信息
     * @author: ajiang
     * @date: 2025/6/22 16:52
     * @param: [currentUserId, userId, ip]
     * @return: com.ajiang.userservice.dto.UserResponseDto
     **/
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
     * AI
     * @description: 获取用户列表
     * @author: ajiang
     * @date: 2025/6/18 15:12
     * @param: [pageParams, currentUserId, ip]
     * @return: 分页用户列表
     **/
    @Override
    public PageResult<User> getUserList(PageParams pageParams, Long currentUserId, String ip) {
        long startTime = System.currentTimeMillis();
        log.info("[业务开始] 获取用户列表: pageNo={}, pageSize={}, currentUserId={}, ip={}",
                pageParams.getPageNo(), pageParams.getPageSize(), currentUserId, ip);

        // 参数校验
        log.debug("[参数校验] 分页参数: pageNo={}, pageSize={}",
                pageParams.getPageNo(), pageParams.getPageSize());
        if (pageParams.getPageNo() <= 0 || pageParams.getPageSize() <= 0) {
            log.warn("[业务异常] 分页参数无效: pageNo={}, pageSize={}, currentUserId={}",
                    pageParams.getPageNo(), pageParams.getPageSize(), currentUserId);
            throw new BusinessException("分页参数无效");
        }

        try {
            // 1. 获取当前用户角色
            log.debug("[远程调用] 获取当前用户角色: currentUserId={}", currentUserId);
            long rpcStartTime = System.currentTimeMillis();
            String currentUserRole = permissionServiceClient.getUserRoleCode(currentUserId);
            long rpcEndTime = System.currentTimeMillis();
            log.debug("[远程调用] 当前用户角色获取完成: currentUserId={}, roleCode={}, 耗时={}ms",
                    currentUserId, currentUserRole, (rpcEndTime - rpcStartTime));

            // 2. 普通用户直接查询自己
            if ("user".equals(currentUserRole)) {
                log.debug("[权限校验] 普通用户查询自己的信息");
                return handleNormalUser(currentUserId, pageParams, ip);
            }

            // 3. 管理员/超管：通过RPC获取分页ID
            log.debug("[权限校验] 管理员/超管通过权限服务获取可见用户ID列表");
            rpcStartTime = System.currentTimeMillis();
            PageResult<Long> idPageResult = permissionServiceClient.getVisibleUserIds(
                    currentUserId,
                    currentUserRole,
                    pageParams.getPageNo().intValue(),
                    pageParams.getPageSize().intValue());
            rpcEndTime = System.currentTimeMillis();
            log.debug("[远程调用] 可见用户ID列表获取完成: count={}, total={}, 耗时={}ms",
                    idPageResult.getItems().size(), idPageResult.getCounts(), (rpcEndTime - rpcStartTime));

            // 4. 没有数据直接返回
            if (idPageResult.getItems().isEmpty()) {
                log.info("[业务处理] 权限过滤后无可见用户，返回空结果");
                PageResult<User> emptyResult = PageResult.empty(pageParams.getPageNo(), pageParams.getPageSize());

                // 发送查看日志
                try {
                    long mqStartTime = System.currentTimeMillis();
                    logProducer.sendUserListViewLog(currentUserId, currentUserRole,
                            pageParams.getPageNo(), pageParams.getPageSize(), 0, ip);
                    long mqEndTime = System.currentTimeMillis();
                    log.debug("[消息队列] 用户列表查看日志发送完成: 耗时={}ms", (mqEndTime - mqStartTime));
                } catch (Exception e) {
                    log.error("[消息队列] 用户列表查看日志发送失败: error={}", e.getMessage(), e);
                }

                long totalTime = System.currentTimeMillis() - startTime;
                log.info("[业务完成] 用户列表查询完成(空结果): 总耗时={}ms", totalTime);
                return emptyResult;
            }

            // 5. 批量查询用户详情
            log.debug("[数据库操作] 批量查询用户详情，用户ID数量: {}", idPageResult.getItems().size());
            long dbStartTime = System.currentTimeMillis();
            List<User> users = userMapper.selectBatchIds(idPageResult.getItems());
            long dbEndTime = System.currentTimeMillis();
            log.debug("[数据库操作] 用户详情查询完成: expected={}, actual={}, 耗时={}ms",
                    idPageResult.getItems().size(), users.size(), (dbEndTime - dbStartTime));

            // 6. 验证查询结果的完整性
            if (users.size() != idPageResult.getItems().size()) {
                log.warn("[数据一致性] 用户详情查询不完整: 期望{}个用户，实际查到{}个用户",
                        idPageResult.getItems().size(), users.size());
            }

            // 7. 构建结果
            log.debug("[业务处理] 构建分页结果");
            PageResult<User> result = new PageResult<>();
            result.setItems(users);
            result.setCounts(idPageResult.getCounts());
            result.setPage(pageParams.getPageNo());
            result.setPageSize(pageParams.getPageSize());

            // 8. 记录日志
            try {
                long mqStartTime = System.currentTimeMillis();
                logProducer.sendUserListViewLog(currentUserId, currentUserRole,
                        pageParams.getPageNo(), pageParams.getPageSize(), users.size(), ip);
                long mqEndTime = System.currentTimeMillis();
                log.debug("[消息队列] 用户列表查看日志发送完成: 耗时={}ms", (mqEndTime - mqStartTime));
            } catch (Exception e) {
                log.error("[消息队列] 用户列表查看日志发送失败: error={}", e.getMessage(), e);
            }

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("[业务完成] 用户列表查询完成: 返回{}个用户，总数={}, 总耗时={}ms",
                    users.size(), idPageResult.getCounts(), totalTime);
            return result;

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("[业务异常] 用户列表查询失败: currentUserId={}, 错误: {}, 总耗时={}ms",
                    currentUserId, e.getMessage(), totalTime, e);
            throw new BusinessException("查询用户列表失败: " + e.getMessage());
        }
    }

    private PageResult<User> handleNormalUser(Long userId, PageParams pageParams, String ip) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        PageResult<User> result = new PageResult<>();
        result.setItems(Collections.singletonList(user));
        result.setCounts(1L);
        result.setPage(1);
        result.setPageSize(1);

        logProducer.sendUserListViewLog(userId, "user",
                pageParams.getPageNo(), pageParams.getPageSize(), 1, ip);
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
        long startTime = System.currentTimeMillis();
        log.info("[业务开始] 修改用户信息: targetUserId={}, currentUserId={}, ip={}",
                userId, currentUserId, ip);

        // 检查用户是否存在
        log.debug("[数据库操作] 查询目标用户信息: userId={}", userId);
        long dbStartTime = System.currentTimeMillis();
        User existingUser = this.getById(userId);
        long dbEndTime = System.currentTimeMillis();
        log.debug("[数据库操作] 目标用户查询完成: userId={}, found={}, 耗时={}ms",
                userId, (existingUser != null), (dbEndTime - dbStartTime));

        if (existingUser == null) {
            log.warn("[业务异常] 用户不存在: userId={}, currentUserId={}, ip={}",
                    userId, currentUserId, ip);
            throw new BusinessException("用户不存在");
        }

        // 获取当前用户角色
        log.debug("[远程调用] 获取当前用户角色: currentUserId={}", currentUserId);
        long rpcStartTime = System.currentTimeMillis();
        String currentUserRoleCode = permissionServiceClient.getUserRoleCode(currentUserId);
        long rpcEndTime = System.currentTimeMillis();
        log.debug("[远程调用] 当前用户角色获取完成: currentUserId={}, roleCode={}, 耗时={}ms",
                currentUserId, currentUserRoleCode, (rpcEndTime - rpcStartTime));

        // 获取目标用户角色
        log.debug("[远程调用] 获取目标用户角色: targetUserId={}", userId);
        rpcStartTime = System.currentTimeMillis();
        String targetUserRoleCode = permissionServiceClient.getUserRoleCode(userId);
        rpcEndTime = System.currentTimeMillis();
        log.debug("[远程调用] 目标用户角色获取完成: targetUserId={}, roleCode={}, 耗时={}ms",
                userId, targetUserRoleCode, (rpcEndTime - rpcStartTime));

        // 权限校验
        log.debug("[权限校验] 检查修改权限: currentRole={}, targetRole={}, currentUserId={}, targetUserId={}",
                currentUserRoleCode, targetUserRoleCode, currentUserId, userId);
        long permissionStartTime = System.currentTimeMillis();
        boolean hasPermission = hasPermissionToModify(currentUserRoleCode, targetUserRoleCode, currentUserId, userId);
        long permissionEndTime = System.currentTimeMillis();
        log.debug("[权限校验] 权限检查完成: hasPermission={}, 耗时={}ms",
                hasPermission, (permissionEndTime - permissionStartTime));

        if (!hasPermission) {
            log.warn("[业务异常] 权限不足: currentUserId={}, targetUserId={}, currentRole={}, targetRole={}, ip={}",
                    currentUserId, userId, currentUserRoleCode, targetUserRoleCode, ip);
            throw new BusinessException("权限不足，无法修改该用户信息");
        }

        // 记录修改前的信息，用于日志
        log.debug("[业务处理] 记录修改前信息: userId={}", userId);
        Map<String, Object> changes = new HashMap<>();
        if (!existingUser.getEmail().equals(user.getEmail())) {
            Map<String, String> emailChange = new HashMap<>();
            emailChange.put("old", existingUser.getEmail());
            emailChange.put("new", user.getEmail());
            changes.put("email", emailChange);
            log.debug("[业务处理] 邮箱变更: {} -> {}", existingUser.getEmail(), user.getEmail());
        }
        if (!existingUser.getPhone().equals(user.getPhone())) {
            Map<String, String> phoneChange = new HashMap<>();
            phoneChange.put("old", existingUser.getPhone());
            phoneChange.put("new", user.getPhone());
            changes.put("phone", phoneChange);
            log.debug("[业务处理] 手机号变更: {} -> {}", existingUser.getPhone(), user.getPhone());
        }

        // 更新用户信息（只允许修改邮箱和手机号）
        log.debug("[数据库操作] 更新用户信息: userId={}, changes={}", userId, changes.keySet());
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUserId, userId)
                .set(User::getEmail, user.getEmail())
                .set(User::getPhone, user.getPhone());

        dbStartTime = System.currentTimeMillis();
        int updateCount = userMapper.update(null, updateWrapper);
        boolean result = updateCount > 0;
        dbEndTime = System.currentTimeMillis();
        log.info("[数据库操作] 用户信息更新完成: userId={}, updateCount={}, result={}, 耗时={}ms",
                userId, updateCount, result, (dbEndTime - dbStartTime));

        // 发送操作日志
        if (result && !changes.isEmpty()) {
            log.debug("[消息队列] 发送用户更新日志: userId={}", userId);
            try {
                long mqStartTime = System.currentTimeMillis();
                logProducer.sendUserUpdateLog(currentUserId, existingUser.getUsername(), ip, changes);
                long mqEndTime = System.currentTimeMillis();
                log.debug("[消息队列] 用户更新日志发送完成: userId={}, 耗时={}ms",
                        userId, (mqEndTime - mqStartTime));
            } catch (Exception e) {
                log.error("[消息队列] 用户更新日志发送失败: userId={}, error={}",
                        userId, e.getMessage(), e);
                // 日志发送失败不影响更新流程
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("[业务完成] 用户信息修改成功: userId={}, username={}, 总耗时={}ms",
                userId, existingUser.getUsername(), totalTime);
        return result;
    }

    /**
     * @description: 重置密码
     * @author: ajiang
     * @date: 2025/6/22 16:53
     * @param: [passwordResetDto, currentUserId, ip]
     * @return: boolean
     **/
    @Override
    @Transactional
    public boolean resetPassword(PasswordResetDto passwordResetDto, Long currentUserId, String ip) {
        Long targetUserId = passwordResetDto.getUserId();
        long startTime = System.currentTimeMillis();
        log.info("[业务开始] 重置用户密码: targetUserId={}, currentUserId={}, ip={}",
                targetUserId, currentUserId, ip);

        // 检查用户是否存在
        log.debug("[数据库操作] 查询目标用户信息: userId={}", targetUserId);
        long dbStartTime = System.currentTimeMillis();
        User targetUser = userMapper.selectById(targetUserId);
        long dbEndTime = System.currentTimeMillis();
        log.debug("[数据库操作] 目标用户查询完成: userId={}, found={}, 耗时={}ms",
                targetUserId, (targetUser != null), (dbEndTime - dbStartTime));

        if (targetUser == null) {
            log.warn("[业务异常] 用户不存在: userId={}, currentUserId={}, ip={}",
                    targetUserId, currentUserId, ip);
            throw new BusinessException("用户不存在");
        }

        // 获取当前用户角色
        log.debug("[远程调用] 获取当前用户角色: currentUserId={}", currentUserId);
        long rpcStartTime = System.currentTimeMillis();
        String currentUserRoleCode = permissionServiceClient.getUserRoleCode(currentUserId);
        long rpcEndTime = System.currentTimeMillis();
        log.debug("[远程调用] 当前用户角色获取完成: currentUserId={}, roleCode={}, 耗时={}ms",
                currentUserId, currentUserRoleCode, (rpcEndTime - rpcStartTime));

        // 获取目标用户角色
        log.debug("[远程调用] 获取目标用户角色: targetUserId={}", targetUserId);
        rpcStartTime = System.currentTimeMillis();
        String targetUserRoleCode = permissionServiceClient.getUserRoleCode(targetUserId);
        rpcEndTime = System.currentTimeMillis();
        log.debug("[远程调用] 目标用户角色获取完成: targetUserId={}, roleCode={}, 耗时={}ms",
                targetUserId, targetUserRoleCode, (rpcEndTime - rpcStartTime));

        // 权限校验
        log.debug("[权限校验] 检查密码重置权限: currentRole={}, targetRole={}, currentUserId={}, targetUserId={}",
                currentUserRoleCode, targetUserRoleCode, currentUserId, targetUserId);
        long permissionStartTime = System.currentTimeMillis();
        boolean hasPermission = hasPermissionToModify(currentUserRoleCode, targetUserRoleCode, currentUserId,
                targetUserId);
        long permissionEndTime = System.currentTimeMillis();
        log.debug("[权限校验] 权限检查完成: hasPermission={}, 耗时={}ms",
                hasPermission, (permissionEndTime - permissionStartTime));

        if (!hasPermission) {
            log.warn("[业务异常] 权限不足: currentUserId={}, targetUserId={}, currentRole={}, targetRole={}, ip={}",
                    currentUserId, targetUserId, currentUserRoleCode, targetUserRoleCode, ip);
            throw new BusinessException("权限不足，无法重置该用户密码");
        }

        // 密码加密
        log.debug("[业务处理] 加密新密码: userId={}", targetUserId);
        long encodeStartTime = System.currentTimeMillis();
        String encodedPassword = passwordEncoder.encode(passwordResetDto.getNewPassword());
        long encodeEndTime = System.currentTimeMillis();
        log.debug("[业务处理] 密码加密完成: userId={}, 耗时={}ms",
                targetUserId, (encodeEndTime - encodeStartTime));

        // 更新密码
        log.debug("[数据库操作] 更新用户密码: userId={}", targetUserId);
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUserId, targetUserId)
                .set(User::getPassword, encodedPassword);

        dbStartTime = System.currentTimeMillis();
        int updateCount = userMapper.update(null, updateWrapper);
        boolean result = updateCount > 0;
        dbEndTime = System.currentTimeMillis();
        log.info("[数据库操作] 密码更新完成: userId={}, updateCount={}, result={}, 耗时={}ms",
                targetUserId, updateCount, result, (dbEndTime - dbStartTime));

        // 发送操作日志
        if (result) {
            log.debug("[消息队列] 发送密码重置日志: userId={}", targetUserId);
            try {
                long mqStartTime = System.currentTimeMillis();
                logProducer.sendPasswordResetLog(currentUserId, targetUser.getUsername(), ip);
                long mqEndTime = System.currentTimeMillis();
                log.debug("[消息队列] 密码重置日志发送完成: userId={}, 耗时={}ms",
                        targetUserId, (mqEndTime - mqStartTime));
            } catch (Exception e) {
                log.error("[消息队列] 密码重置日志发送失败: userId={}, error={}",
                        targetUserId, e.getMessage(), e);
                // 日志发送失败不影响重置流程
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("[业务完成] 密码重置成功: userId={}, username={}, 总耗时={}ms",
                targetUserId, targetUser.getUsername(), totalTime);
        return result;
    }

    /**
     * AI
     * 检查是否有权限修改目标用户
     * 权限规则：
     * 1. 超级管理员可以修改所有用户
     * 2. 管理员可以修改普通用户和自己
     * 3. 普通用户只能修改自己
     */
    private boolean hasPermissionToModify(String currentUserRoleCode, String targetUserRoleCode,
                                          Long currentUserId, Long targetUserId) {
        log.debug("[权限判断] 开始权限校验: currentRole={}, targetRole={}, currentUserId={}, targetUserId={}",
                currentUserRoleCode, targetUserRoleCode, currentUserId, targetUserId);

        // 超级管理员可以修改所有用户
        if ("super_admin".equals(currentUserRoleCode)) {
            log.debug("[权限判断] 超级管理员权限: 允许修改所有用户");
            return true;
        }

        // 管理员可以修改普通用户和自己
        if ("admin".equals(currentUserRoleCode)) {
            // 管理员修改自己
            if (currentUserId != null && currentUserId.equals(targetUserId)) {
                log.debug("[权限判断] 管理员权限: 修改自己的信息");
                return true;
            }
            // 管理员修改普通用户
            if ("user".equals(targetUserRoleCode)) {
                log.debug("[权限判断] 管理员权限: 修改普通用户信息");
                return true;
            }
            log.debug("[权限判断] 管理员权限: 无法修改其他管理员或超管信息");
        }

        // 普通用户只能修改自己
        if ("user".equals(currentUserRoleCode) && currentUserId != null && currentUserId.equals(targetUserId)) {
            log.debug("[权限判断] 普通用户权限: 修改自己的信息");
            return true;
        }

        log.warn("[权限判断] 权限不足: currentRole={}, targetRole={}, currentUserId={}, targetUserId={}",
                currentUserRoleCode, targetUserRoleCode, currentUserId, targetUserId);
        return false;
    }

}

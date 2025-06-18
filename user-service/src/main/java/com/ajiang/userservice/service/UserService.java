package com.ajiang.userservice.service;

import com.ajiang.common.model.PageParams;
import com.ajiang.common.model.PageResult;
import com.ajiang.userservice.dto.PasswordResetDto;
import com.ajiang.userservice.dto.UserLoginDto;
import com.ajiang.userservice.dto.UserRegisterDto;
import com.ajiang.userservice.dto.UserResponseDto;
import com.ajiang.userservice.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

/**
 * 用户服务接口
 * 定义用户注册、登录、查询和修改等功能
 */
@Service
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param registerDto 注册信息
     * @param ip          客户端IP
     * @return 用户ID
     */
    Long register(UserRegisterDto registerDto, String ip);

    /**
     * 用户登录
     *
     * @param loginDto 登录信息
     * @param ip       客户端IP
     * @return JWT Token
     */
    String login(UserLoginDto loginDto, String ip);

    /**
     * 获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    UserResponseDto getUserInfo(Long userId);

    /**
     * 获取用户列表
     *
     * @param pageParams    分页参数
     * @param currentUserId 当前用户ID
     * @return 分页用户列表
     */
    PageResult<User> getUserList(PageParams pageParams, Long currentUserId);

    /**
     * 修改用户信息
     *
     * @param userId        用户ID
     * @param user          用户信息
     * @param currentUserId 当前用户ID
     * @param ip            客户端IP
     * @return 是否成功
     */
    boolean updateUser(Long userId, User user, Long currentUserId, String ip);

    /**
     * 重置密码
     *
     * @param passwordResetDto 密码重置信息
     * @param currentUserId    当前用户ID
     * @param ip               客户端IP
     * @return 是否成功
     */
    boolean resetPassword(PasswordResetDto passwordResetDto, Long currentUserId, String ip);
}

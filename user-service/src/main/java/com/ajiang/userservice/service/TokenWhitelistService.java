package com.ajiang.userservice.service;

/**
 * AI
 * Token白名单服务接口
 * 基于Redis实现token白名单机制
 */
public interface TokenWhitelistService {

    /**
     * 将token添加到白名单
     *
     * @param token         JWT token
     * @param userId        用户ID
     * @param expireSeconds 过期时间（秒）
     */
    void addTokenToWhitelist(String token, Long userId, long expireSeconds);

    /**
     * 检查token是否在白名单中
     *
     * @param token JWT token
     * @return true-在白名单中，false-不在白名单中
     */
    boolean isTokenInWhitelist(String token);

    /**
     * 从白名单中移除token（登出时使用）
     *
     * @param token JWT token
     */
    void removeTokenFromWhitelist(String token);

    /**
     * 获取token对应的用户ID
     *
     * @param token JWT token
     * @return 用户ID，如果token不存在则返回null
     */
    Long getUserIdByToken(String token);

    /**
     * 移除用户的所有token（强制下线）
     *
     * @param userId 用户ID
     */
    void removeAllTokensByUserId(Long userId);
}
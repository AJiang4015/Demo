package com.ajiang.userservice.service.impl;

import com.ajiang.userservice.service.TokenWhitelistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Token白名单服务实现类
 * 基于Redis实现token白名单机制
 */
@Slf4j
@Service
public class TokenWhitelistServiceImpl implements TokenWhitelistService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Redis key前缀
    private static final String TOKEN_PREFIX = "token:whitelist:";
    private static final String USER_TOKEN_PREFIX = "user:tokens:";

    /**
     * 将token添加到白名单
     *
     * @param token         JWT token
     * @param userId        用户ID
     * @param expireSeconds 过期时间（秒）
     */
    @Override
    public void addTokenToWhitelist(String token, Long userId, long expireSeconds) {
        try {
            String tokenKey = TOKEN_PREFIX + token;
            String userTokenKey = USER_TOKEN_PREFIX + userId;

            // 存储token -> userId的映射，设置过期时间
            redisTemplate.opsForValue().set(tokenKey, userId, expireSeconds, TimeUnit.SECONDS);

            // 存储userId -> tokens的集合映射，用于批量删除用户的所有token
            redisTemplate.opsForSet().add(userTokenKey, token);
            redisTemplate.expire(userTokenKey, expireSeconds, TimeUnit.SECONDS);

            log.info("Token添加到白名单成功: userId={}, token={}", userId,
                    token.substring(0, Math.min(token.length(), 20)) + "...");
        } catch (Exception e) {
            log.error("Token添加到白名单失败: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("添加token到白名单失败", e);
        }
    }

    /**
     * 检查token是否在白名单中
     *
     * @param token JWT token
     * @return true-在白名单中，false-不在白名单中
     */
    @Override
    public boolean isTokenInWhitelist(String token) {
        try {
            String tokenKey = TOKEN_PREFIX + token;
            Boolean exists = redisTemplate.hasKey(tokenKey);
            log.debug("检查token白名单: token={}, exists={}", token.substring(0, Math.min(token.length(), 20)) + "...",
                    exists);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("检查token白名单失败: error={}", e.getMessage(), e);
            // 出现异常时，为了安全考虑，返回false
            return false;
        }
    }

    /**
     * 从白名单中移除token（登出时使用）
     *
     * @param token JWT token
     */
    @Override
    public void removeTokenFromWhitelist(String token) {
        try {
            String tokenKey = TOKEN_PREFIX + token;

            // 先获取userId，用于从用户token集合中移除
            Long userId = (Long) redisTemplate.opsForValue().get(tokenKey);

            // 删除token -> userId的映射
            redisTemplate.delete(tokenKey);

            // 从用户token集合中移除该token
            if (userId != null) {
                String userTokenKey = USER_TOKEN_PREFIX + userId;
                redisTemplate.opsForSet().remove(userTokenKey, token);
            }

            log.info("Token从白名单移除成功: userId={}, token={}", userId,
                    token.substring(0, Math.min(token.length(), 20)) + "...");
        } catch (Exception e) {
            log.error("Token从白名单移除失败: error={}", e.getMessage(), e);
            throw new RuntimeException("从白名单移除token失败", e);
        }
    }

    /**
     * 获取token对应的用户ID
     *
     * @param token JWT token
     * @return 用户ID，如果token不存在则返回null
     */
    @Override
    public Long getUserIdByToken(String token) {
        try {
            String tokenKey = TOKEN_PREFIX + token;
            Object userId = redisTemplate.opsForValue().get(tokenKey);
            return userId != null ? (Long) userId : null;
        } catch (Exception e) {
            log.error("获取token对应用户ID失败: error={}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 移除用户的所有token（强制下线）
     *
     * @param userId 用户ID
     */
    @Override
    public void removeAllTokensByUserId(Long userId) {
        try {
            String userTokenKey = USER_TOKEN_PREFIX + userId;

            // 获取用户的所有token
            Set<Object> tokens = redisTemplate.opsForSet().members(userTokenKey);

            if (tokens != null && !tokens.isEmpty()) {
                // 删除所有token -> userId的映射
                for (Object token : tokens) {
                    String tokenKey = TOKEN_PREFIX + token;
                    redisTemplate.delete(tokenKey);
                }

                // 删除用户token集合
                redisTemplate.delete(userTokenKey);

                log.info("用户所有token移除成功: userId={}, tokenCount={}", userId, tokens.size());
            } else {
                log.info("用户没有有效token: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("移除用户所有token失败: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("移除用户所有token失败", e);
        }
    }
}
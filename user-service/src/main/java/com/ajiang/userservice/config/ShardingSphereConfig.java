package com.ajiang.userservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * ShardingSphere 分库分表配置类
 * 提供分库分表的配置说明和监控
 *
 * @author AJiang
 * @date 2025/1/15
 */
@Slf4j
@Configuration
public class ShardingSphereConfig {

    @PostConstruct
    public void init() {
        log.info("==================== ShardingSphere 分库分表配置 ====================");
        log.info("数据库分片策略：(user_id % 4) >> 1");
        log.info("  - user_id % 4 = 0 → (0 >> 1) = 0 → ds0 (db_user0)");
        log.info("  - user_id % 4 = 1 → (1 >> 1) = 0 → ds0 (db_user0)");
        log.info("  - user_id % 4 = 2 → (2 >> 1) = 1 → ds1 (db_user1)");
        log.info("  - user_id % 4 = 3 → (3 >> 1) = 1 → ds1 (db_user1)");
        log.info("表分片策略：(user_id % 4) & 1");
        log.info("  - user_id % 4 = 0 → (0 & 1) = 0 → users_0");
        log.info("  - user_id % 4 = 1 → (1 & 1) = 1 → users_1");
        log.info("  - user_id % 4 = 2 → (2 & 1) = 0 → users_0");
        log.info("  - user_id % 4 = 3 → (3 & 1) = 1 → users_1");
        log.info("实际数据节点：");
        log.info("  - ds0: users_0, users_1");
        log.info("  - ds1: users_0, users_1");
        log.info("=====================================================================");
    }

    public static String calculateDatabase(Long userId) {
        return "ds" + ((userId % 4) >> 1);
    }

    public static String calculateTable(Long userId) {
        return "users_" + ((userId % 4) & 1);
    }

    /**
     * 获取完整的数据节点信息
     *
     * @param userId 用户ID
     * @return 数据节点信息
     */
    public static String getDataNode(Long userId) {
        return calculateDatabase(userId) + "." + calculateTable(userId);
    }

    /**
     * 打印用户数据路由信息（用于调试）
     *
     * @param userId 用户ID
     */
    public static void logUserRouting(Long userId) {
        if (log.isDebugEnabled()) {
            log.debug("用户ID: {} 路由信息 -> 数据库: {}, 表: {}, 完整节点: {}",
                    userId,
                    calculateDatabase(userId),
                    calculateTable(userId),
                    getDataNode(userId));
        }
    }
}
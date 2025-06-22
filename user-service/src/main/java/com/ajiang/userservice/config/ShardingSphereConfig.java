package com.ajiang.userservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * AI
 * ShardingSphere 分库分表配置类
 * 提供分库分表的配置说明和监控
 *
 * @author AJiang
 * @date 2025/1/15
 */
@Slf4j
@Configuration
public class ShardingSphereConfig {

    // 雪花算法相关常量
    private static final long SEQUENCE_MASK = 0xFFFL; // 序列号掩码（低12位）
    private static final long TIMESTAMP_SHIFT = 22L; // 时间戳右移位数
    private static final long TIMESTAMP_LOW_MASK = 0x3FFL; // 时间戳低位掩码（10位）

    @PostConstruct
    public void init() {
        log.info("==================== ShardingSphere 分库分表配置 ====================");
        log.info("数据库分片策略：序列号部分 % 2");
        log.info("  - 提取雪花ID的序列号部分（低12位）");
        log.info("  - 序列号 % 2 = 0 → ds0 (db_user0)");
        log.info("  - 序列号 % 2 = 1 → ds1 (db_user1)");
        log.info("表分片策略：时间戳低位部分 % 2");
        log.info("  - 提取雪花ID的时间戳低位（右移22位后取低10位）");
        log.info("  - 时间戳低位 % 2 = 0 → users_0");
        log.info("  - 时间戳低位 % 2 = 1 → users_1");
        log.info("实际数据节点：");
        log.info("  - ds0: users_0, users_1");
        log.info("  - ds1: users_0, users_1");
        log.info("=====================================================================");
    }

    /**
     * 计算数据库分片
     *
     * @param userId 用户ID（雪花算法生成）
     * @return 数据库分片名称
     */
    public static String calculateDatabase(Long userId) {
        // 提取序列号部分（低12位）并模2
        long sequence = userId & SEQUENCE_MASK;
        return "ds" + (sequence % 2);
    }

    /**
     * 计算表分片
     *
     * @param userId 用户ID（雪花算法生成）
     * @return 表分片名称
     */
    public static String calculateTable(Long userId) {
        // 提取时间戳低位部分并模2
        long timestampLow = (userId >> TIMESTAMP_SHIFT) & TIMESTAMP_LOW_MASK;
        return "users_" + (timestampLow % 2);
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
            // 提取详细信息用于调试
            long sequence = userId & SEQUENCE_MASK;
            long timestampLow = (userId >> TIMESTAMP_SHIFT) & TIMESTAMP_LOW_MASK;

            log.debug("用户ID: {} 路由信息 -> 序列号: {}, 时间戳低位: {}, 数据库: {}, 表: {}, 完整节点: {}",
                    userId,
                    sequence,
                    timestampLow,
                    calculateDatabase(userId),
                    calculateTable(userId),
                    getDataNode(userId));
        }
    }
}
package com.ajiang.userservice.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.seata.core.context.RootContext;
import org.springframework.stereotype.Component;

/**
 * AI
 * Seata分布式事务工具类
 * 用于监控和记录分布式事务状态
 *
 * @author AJiang
 * @date 2025/1/15
 */
@Component
@Slf4j
public class SeataTransactionUtil {

    /**
     * 记录事务开始
     */
    public static void logTransactionStart(String businessOperation) {
        String xid = RootContext.getXID();
        log.info("[分布式事务开始] 业务操作: {}, XID: {}", businessOperation, xid);
    }

    /**
     * 记录事务成功
     */
    public static void logTransactionSuccess(String businessOperation) {
        String xid = RootContext.getXID();
        log.info("[分布式事务成功] 业务操作: {}, XID: {}", businessOperation, xid);
    }

    /**
     * 记录事务失败
     */
    public static void logTransactionFailure(String businessOperation, Exception e) {
        String xid = RootContext.getXID();
        log.error("[分布式事务失败] 业务操作: {}, XID: {}, 错误: {}", businessOperation, xid, e.getMessage());
    }
}
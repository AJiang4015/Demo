package com.ajiang.loggingservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("operation_logs")
public class OperationLog {
    /**
     * 日志ID（分布式ID）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long logId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 操作类型
     */
    private String action;

    /**
     * 操作IP
     */
    private String ip;

    /**
     * 操作详情（JSON格式）
     */
    private String detail;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;
}
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

    private Long userId;

    private String action;

    private String ip;

    private String detail;
}
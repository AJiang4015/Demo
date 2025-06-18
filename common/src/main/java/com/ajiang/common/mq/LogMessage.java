package com.ajiang.common.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * 操作日志消息实体
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogMessage implements Serializable {
    private Long userId;
    private String action;
    private String ip;
    private String detail;

    @Override
    public String toString() {
        return "LogMessage{" +
                "userId=" + userId +
                ", action='" + action + '\'' +
                ", ip='" + ip + '\'' +
                ", detail='" + detail + '\'' +
                '}';
    }
}
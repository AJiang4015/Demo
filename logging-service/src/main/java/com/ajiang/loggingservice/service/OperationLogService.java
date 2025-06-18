package com.ajiang.loggingservice.service;

import com.ajiang.loggingservice.entity.OperationLog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 操作日志服务接口
 */
public interface OperationLogService extends IService<OperationLog> {

    void saveOperationLog(OperationLog operationLog);
}
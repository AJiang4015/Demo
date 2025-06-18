package com.ajiang.loggingservice.controller;

import com.ajiang.loggingservice.entity.OperationLog;
import com.ajiang.loggingservice.service.OperationLogService;
import com.ajiang.common.model.ApiResponse;
import com.ajiang.common.model.ErrorApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/logs")
public class OperationLogController {

}
package com.ajiang.common.exception;

/**
 * AI
 * 业务异常类
 * 用于处理业务逻辑异常
 */
public class BusinessException extends RuntimeException {

    /**
     * 构造函数
     *
     * @param message 异常信息
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * 构造函数
     *
     * @param message 异常信息
     * @param cause   异常原因
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
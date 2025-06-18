package com.ajiang.common.model;

/**
 * 错误响应工具类
 * 用于创建带泛型的错误响应
 */
public class ErrorApiResponse {

    /**
     * 创建带泛型的错误响应
     *
     * @param code    响应码
     * @param message 响应消息
     * @param <T>     数据类型
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }

    /**
     * 创建服务器错误响应
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> serverError(String message) {
        return error(500, message);
    }
}
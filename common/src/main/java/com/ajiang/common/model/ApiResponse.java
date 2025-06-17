package com.ajiang.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用API响应对象
 * 用于统一接口返回格式
 *
 * @param <T> 响应数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 创建成功响应
     *
     * @param data 响应数据
     * @param <T>  响应数据类型
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 创建成功响应（无数据）
     *
     * @return ApiResponse
     */
    public static ApiResponse<Void> success() {
        return ApiResponse.<Void>builder()
                .code(200)
                .message("操作成功")
                .build();
    }

    /**
     * 创建失败响应
     *
     * @param code    响应码
     * @param message 响应消息
     * @return ApiResponse
     */
    public static ApiResponse<Void> error(Integer code, String message) {
        return ApiResponse.<Void>builder()
                .code(code)
                .message(message)
                .build();
    }

    /**
     * 创建客户端错误响应
     *
     * @param message 响应消息
     * @return ApiResponse
     */
    public static ApiResponse<Void> badRequest(String message) {
        return error(400, message);
    }

    /**
     * 创建未授权响应
     *
     * @return ApiResponse
     */
    public static ApiResponse<Void> unauthorized() {
        return error(401, "未授权，请先登录");
    }

    /**
     * 创建禁止访问响应
     *
     * @return ApiResponse
     */
    public static ApiResponse<Void> forbidden() {
        return error(403, "权限不足，禁止访问");
    }

    /**
     * 创建资源不存在响应
     *
     * @return ApiResponse
     */
    public static ApiResponse<Void> notFound() {
        return error(404, "资源不存在");
    }

    /**
     * 创建服务器错误响应
     *
     * @param message 错误消息
     * @return ApiResponse
     */
    public static ApiResponse<Void> serverError(String message) {
        return error(500, message);
    }
}
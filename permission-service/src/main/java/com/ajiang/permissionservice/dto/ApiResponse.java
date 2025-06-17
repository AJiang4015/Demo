package com.ajiang.permissionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API响应对象
 * 用于统一接口返回格式
 *
 * @param <T> 响应数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * 响应码
     */
    private String code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 创建成功响应（无数据）
     *
     * @param <T> 响应数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>("200", "操作成功", null);
    }

    /**
     * 创建成功响应（有数据）
     *
     * @param data 响应数据
     * @param <T>  响应数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("200", "操作成功", data);
    }

    /**
     * 创建成功响应（有消息和数据）
     *
     * @param message 响应消息
     * @param data    响应数据
     * @param <T>     响应数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("200", message, data);
    }

    /**
     * 创建失败响应
     *
     * @param <T> 响应数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> error() {
        return new ApiResponse<>("500", "操作失败", null);
    }

    /**
     * 创建失败响应（有消息）
     *
     * @param message 响应消息
     * @param <T>     响应数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("500", message, null);
    }

    /**
     * 创建失败响应（有错误码和消息）
     *
     * @param code    错误码
     * @param message 响应消息
     * @param <T>     响应数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /**
     * 创建客户端错误响应
     *
     * @param message 响应消息
     * @param <T>     响应数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> clientError(String message) {
        return new ApiResponse<>("400", message, null);
    }

    /**
     * 创建客户端错误响应（有数据）
     *
     * @param message 响应消息
     * @param data    响应数据
     * @param <T>     响应数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> clientError(String message, T data) {
        return new ApiResponse<>("400", message, data);
    }

    /**
     * 创建未授权响应
     *
     * @param <T> 响应数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> unauthorized() {
        return new ApiResponse<>("401", "未授权", null);
    }

    /**
     * 创建禁止访问响应
     *
     * @param <T> 响应数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> forbidden() {
        return new ApiResponse<>("403", "禁止访问", null);
    }

    /**
     * 创建资源不存在响应
     *
     * @param <T> 响应数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> notFound() {
        return new ApiResponse<>("404", "资源不存在", null);
    }

    /**
     * 创建服务器错误响应
     *
     * @param message 响应消息
     * @param <T>     响应数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> serverError(String message) {
        return new ApiResponse<>("500", message, null);
    }
}
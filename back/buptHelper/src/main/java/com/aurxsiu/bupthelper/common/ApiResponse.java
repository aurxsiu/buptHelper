package com.aurxsiu.bupthelper.common;

/**
 * 统一响应结构 {code, message, data}。
 */
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return ok(ApiCode.SUCCESS.getMessage(), data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(ApiCode.SUCCESS.getCode(), message, data);
    }

    public static <T> ApiResponse<T> error(ApiCode apiCode, String message, T data) {
        return new ApiResponse<>(apiCode.getCode(), message, data);
    }
}

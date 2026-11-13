package com.aurxsiu.bupthelper.common;

import org.springframework.http.HttpStatus;

/**
 * 业务状态码，与 doc/接口设计-登录模块.md 1.3 保持一致。
 */
public enum ApiCode {

    SUCCESS(0, HttpStatus.OK, "success"),
    PARAM_INVALID(40001, HttpStatus.BAD_REQUEST, "参数校验失败"),
    BAD_CREDENTIALS(40101, HttpStatus.UNAUTHORIZED, "用户名或密码错误"),
    UNAUTHORIZED(40102, HttpStatus.UNAUTHORIZED, "请先登录"),
    OLD_PASSWORD_INCORRECT(40103, HttpStatus.UNAUTHORIZED, "原密码错误"),
    FORBIDDEN(40301, HttpStatus.FORBIDDEN, "无权限访问"),
    USER_NOT_FOUND(40401, HttpStatus.NOT_FOUND, "用户不存在"),
    USERNAME_EXISTS(40901, HttpStatus.CONFLICT, "用户名已存在"),
    LOGIN_LOCKED(42901, HttpStatus.TOO_MANY_REQUESTS, "登录失败次数过多，请 10 分钟后再试"),
    SERVER_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");

    private final int code;
    private final HttpStatus status;
    private final String message;

    ApiCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}

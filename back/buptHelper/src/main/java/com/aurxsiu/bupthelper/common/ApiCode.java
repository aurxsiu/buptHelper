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
    // ---------- “请帮忙”模块（doc/接口设计-请帮忙模块.md 1.3） ----------
    FILE_INVALID(40002, HttpStatus.BAD_REQUEST, "文件类型或大小不符合要求"),
    NOT_REQUEST_OWNER(40302, HttpStatus.FORBIDDEN, "只能操作自己发布的需求"),
    NOT_RESPONSE_OWNER(40303, HttpStatus.FORBIDDEN, "只能操作自己提交的响应"),
    REQUEST_NOT_FOUND(40402, HttpStatus.NOT_FOUND, "需求不存在"),
    RESPONSE_NOT_FOUND(40403, HttpStatus.NOT_FOUND, "响应信息不存在"),
    FILE_NOT_FOUND(40404, HttpStatus.NOT_FOUND, "文件不存在"),
    REQUEST_CANNOT_MODIFY(40902, HttpStatus.CONFLICT, "该需求已有“我来也”响应，不能修改"),
    REQUEST_CANNOT_DELETE(40903, HttpStatus.CONFLICT, "该需求已有“我来也”响应，不能删除"),
    RESPONSE_STATUS_INVALID(40904, HttpStatus.CONFLICT, "当前响应状态不允许该操作"),
    REQUEST_ALREADY_ACCEPTED(40905, HttpStatus.CONFLICT, "该需求已接受其他“我来也”响应"),
    REQUEST_CANCELLED(40906, HttpStatus.CONFLICT, "该需求已取消，不能进行该操作"),
    FILE_REFERENCED(40907, HttpStatus.CONFLICT, "当前文件已被需求引用，不能删除"),
    // ---------- “我来也”模块（doc/接口设计-我来也模块.md 1.3） ----------
    RESPOND_OWN_REQUEST(40908, HttpStatus.CONFLICT, "不能响应自己发布的需求"),
    REQUEST_ALREADY_HELPED(40909, HttpStatus.CONFLICT, "该需求已有人提供帮助，不能再响应"),
    RESPONSE_EXISTS(40910, HttpStatus.CONFLICT, "你已对该需求提交过响应"),
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

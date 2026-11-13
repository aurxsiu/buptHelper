package com.aurxsiu.bupthelper.auth.dto;

/**
 * 登录请求，见 doc/接口设计-登录模块.md 4.1。
 */
public record LoginRequest(String username, String password, Boolean rememberMe) {
}

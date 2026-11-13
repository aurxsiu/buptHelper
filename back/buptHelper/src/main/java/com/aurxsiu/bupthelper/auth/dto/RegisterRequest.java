package com.aurxsiu.bupthelper.auth.dto;

/**
 * 注册请求，见 doc/接口设计-登录模块.md 4.4（confirmPassword 仅由前端校验）。
 */
public record RegisterRequest(String username, String password, String realName, String phone, String bio) {
}

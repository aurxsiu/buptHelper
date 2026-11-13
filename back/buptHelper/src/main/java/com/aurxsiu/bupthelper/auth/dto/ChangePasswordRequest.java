package com.aurxsiu.bupthelper.auth.dto;

/**
 * 修改登录密码请求，见 doc/接口设计-登录模块.md 4.5。
 */
public record ChangePasswordRequest(String oldPassword, String newPassword) {
}

package com.aurxsiu.bupthelper.auth.dto;

/**
 * 修改个人联系信息与简介请求，见 doc/接口设计-登录模块.md 4.7。
 */
public record UpdateProfileRequest(String phone, String bio) {
}

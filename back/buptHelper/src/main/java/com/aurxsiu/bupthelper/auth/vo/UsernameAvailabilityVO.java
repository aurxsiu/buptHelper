package com.aurxsiu.bupthelper.auth.vo;

/**
 * 用户名可用性，见 doc/接口设计-登录模块.md 4.6。
 */
public record UsernameAvailabilityVO(String username, boolean available) {
}

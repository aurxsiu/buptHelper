package com.aurxsiu.bupthelper.user.vo;

import com.aurxsiu.bupthelper.user.entity.User;

import java.time.format.DateTimeFormatter;

/**
 * 用户信息视图对象（不含密码），见 doc/接口设计-登录模块.md 2.1。
 */
public record UserVO(
        Long userId,
        String username,
        String userType,
        String userTypeName,
        String realName,
        String phone,
        String bio,
        String registerTime,
        String updateTime) {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static UserVO from(User user) {
        return new UserVO(
                user.getUserId(),
                user.getUsername(),
                user.getUserType().name(),
                user.getUserType().getDisplayName(),
                user.getRealName(),
                user.getPhone(),
                user.getBio(),
                user.getRegisterTime() == null ? null : user.getRegisterTime().format(DATE_TIME_FORMATTER),
                user.getUpdateTime() == null ? null : user.getUpdateTime().format(DATE_TIME_FORMATTER));
    }
}

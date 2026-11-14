package com.aurxsiu.bupthelper.helprequest.vo;

import com.aurxsiu.bupthelper.user.entity.User;

/**
 * 用户简介信息（发布者 / 响应者），见 doc/接口设计-请帮忙模块.md 2.7。
 */
public record UserBriefVO(
        Long userId,
        String username,
        String realName,
        String phone,
        String bio,
        String registerTime) {

    public static UserBriefVO from(User user) {
        return new UserBriefVO(
                user.getUserId(),
                user.getUsername(),
                user.getRealName(),
                user.getPhone(),
                user.getBio(),
                VoUtils.format(user.getRegisterTime()));
    }
}

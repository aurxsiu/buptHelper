package com.aurxsiu.bupthelper.helprequest.vo;

import com.aurxsiu.bupthelper.user.entity.User;

/**
 * 用户公开信息，见 doc/接口设计-请帮忙模块.md 2.8。
 */
public record UserProfileVO(
        Long userId,
        String username,
        String userType,
        String userTypeName,
        String realName,
        String phone,
        String bio,
        String registerTime) {

    public static UserProfileVO from(User user) {
        return new UserProfileVO(
                user.getUserId(),
                user.getUsername(),
                user.getUserType().name(),
                user.getUserType().getDisplayName(),
                user.getRealName(),
                user.getPhone(),
                user.getBio(),
                VoUtils.format(user.getRegisterTime()));
    }
}

package com.aurxsiu.bupthelper.auth.vo;

import com.aurxsiu.bupthelper.user.vo.UserVO;

/**
 * 登录结果，见 doc/接口设计-登录模块.md 2.2。
 */
public record LoginResultVO(String token, String tokenType, long expiresIn, UserVO user) {
}

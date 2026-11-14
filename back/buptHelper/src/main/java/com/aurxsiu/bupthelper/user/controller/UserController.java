package com.aurxsiu.bupthelper.user.controller;

import com.aurxsiu.bupthelper.common.ApiCode;
import com.aurxsiu.bupthelper.common.ApiResponse;
import com.aurxsiu.bupthelper.common.BusinessException;
import com.aurxsiu.bupthelper.helprequest.vo.UserProfileVO;
import com.aurxsiu.bupthelper.user.entity.User;
import com.aurxsiu.bupthelper.user.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户公开信息接口，见 doc/接口设计-请帮忙模块.md 4.11。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/{userId}/profile")
    public ApiResponse<UserProfileVO> profile(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));
        return ApiResponse.ok(UserProfileVO.from(user));
    }
}

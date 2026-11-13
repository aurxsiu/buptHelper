package com.aurxsiu.bupthelper.auth.controller;

import com.aurxsiu.bupthelper.auth.dto.ChangePasswordRequest;
import com.aurxsiu.bupthelper.auth.dto.LoginRequest;
import com.aurxsiu.bupthelper.auth.dto.RegisterRequest;
import com.aurxsiu.bupthelper.auth.dto.UpdateProfileRequest;
import com.aurxsiu.bupthelper.auth.service.AuthService;
import com.aurxsiu.bupthelper.auth.vo.LoginResultVO;
import com.aurxsiu.bupthelper.auth.vo.UsernameAvailabilityVO;
import com.aurxsiu.bupthelper.common.ApiResponse;
import com.aurxsiu.bupthelper.security.AuthInterceptor;
import com.aurxsiu.bupthelper.security.AuthUser;
import com.aurxsiu.bupthelper.user.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录 / 认证接口，见 doc/接口设计-登录模块.md 第 4 章。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResultVO> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok("登录成功", authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestAttribute(AuthInterceptor.ATTR_USER) AuthUser currentUser) {
        authService.logout(currentUser);
        return ApiResponse.ok("已退出登录", null);
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> me(@RequestAttribute(AuthInterceptor.ATTR_USER) AuthUser currentUser) {
        return ApiResponse.ok(authService.currentUser(currentUser.userId()));
    }

    @PostMapping("/register")
    public ApiResponse<UserVO> register(@RequestBody RegisterRequest request) {
        return ApiResponse.ok("注册成功", authService.register(request));
    }

    @PutMapping("/profile")
    public ApiResponse<UserVO> updateProfile(@RequestAttribute(AuthInterceptor.ATTR_USER) AuthUser currentUser,
                                             @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok("个人信息修改成功", authService.updateProfile(currentUser.userId(), request));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@RequestAttribute(AuthInterceptor.ATTR_USER) AuthUser currentUser,
                                            @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUser.userId(), request);
        return ApiResponse.ok("密码修改成功，请重新登录", null);
    }

    @GetMapping("/username-available")
    public ApiResponse<UsernameAvailabilityVO> usernameAvailable(@RequestParam(name = "username") String username) {
        return ApiResponse.ok(authService.checkUsername(username));
    }
}

package com.aurxsiu.bupthelper.auth.service;

import com.aurxsiu.bupthelper.auth.dto.ChangePasswordRequest;
import com.aurxsiu.bupthelper.auth.dto.LoginRequest;
import com.aurxsiu.bupthelper.auth.dto.RegisterRequest;
import com.aurxsiu.bupthelper.auth.dto.UpdateProfileRequest;
import com.aurxsiu.bupthelper.auth.support.LoginAttemptTracker;
import com.aurxsiu.bupthelper.auth.vo.LoginResultVO;
import com.aurxsiu.bupthelper.auth.vo.UsernameAvailabilityVO;
import com.aurxsiu.bupthelper.common.ApiCode;
import com.aurxsiu.bupthelper.common.BusinessException;
import com.aurxsiu.bupthelper.common.ValidationUtils;
import com.aurxsiu.bupthelper.config.JwtProperties;
import com.aurxsiu.bupthelper.security.AuthUser;
import com.aurxsiu.bupthelper.security.JwtTokenProvider;
import com.aurxsiu.bupthelper.security.TokenRevocationStore;
import com.aurxsiu.bupthelper.user.entity.User;
import com.aurxsiu.bupthelper.user.entity.UserType;
import com.aurxsiu.bupthelper.user.repository.UserRepository;
import com.aurxsiu.bupthelper.user.vo.UserVO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 登录 / 注册 / 注销 / 修改个人信息 / 修改密码等认证业务。
 */
@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final int USERNAME_MAX_LENGTH = 50;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final TokenRevocationStore revocationStore;
    private final LoginAttemptTracker loginAttemptTracker;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       JwtProperties jwtProperties,
                       TokenRevocationStore revocationStore,
                       LoginAttemptTracker loginAttemptTracker) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.jwtProperties = jwtProperties;
        this.revocationStore = revocationStore;
        this.loginAttemptTracker = loginAttemptTracker;
    }

    @Transactional(readOnly = true)
    public LoginResultVO login(LoginRequest request) {
        String username = trimToEmpty(request.username());
        String password = request.password() == null ? "" : request.password();

        Map<String, String> errors = new LinkedHashMap<>();
        if (username.isEmpty()) {
            errors.put("username", "用户名不能为空");
        } else if (username.length() > USERNAME_MAX_LENGTH) {
            errors.put("username", "用户名长度不能超过 50 个字符");
        }
        if (password.isEmpty()) {
            errors.put("password", "密码不能为空");
        }
        requireNoFieldErrors(errors);

        loginAttemptTracker.checkNotLocked(username);

        User user = userRepository.findByUsername(username).orElse(null);
        // 用户名不存在与密码错误返回同一提示，避免用户枚举
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            if (loginAttemptTracker.recordFailure(username)) {
                throw new BusinessException(ApiCode.LOGIN_LOCKED, ApiCode.LOGIN_LOCKED.getMessage());
            }
            throw new BusinessException(ApiCode.BAD_CREDENTIALS);
        }

        loginAttemptTracker.reset(username);

        long expiresIn = Boolean.TRUE.equals(request.rememberMe())
                ? jwtProperties.getRememberMeExpireSeconds()
                : jwtProperties.getExpireSeconds();
        String token = tokenProvider.createToken(user, expiresIn, revocationStore.currentVersion(user.getUserId()));
        return new LoginResultVO(token, TOKEN_TYPE, expiresIn, UserVO.from(user));
    }

    @Transactional
    public UserVO register(RegisterRequest request) {
        String username = trimToEmpty(request.username());
        String bio = trimToEmpty(request.bio());

        Map<String, String> errors = new LinkedHashMap<>();
        putIfPresent(errors, "username", ValidationUtils.validateUsername(username));
        putIfPresent(errors, "password", ValidationUtils.validatePassword(request.password()));
        putIfPresent(errors, "realName", ValidationUtils.validateRealName(trimToEmpty(request.realName())));
        putIfPresent(errors, "phone", ValidationUtils.validatePhone(trimToEmpty(request.phone())));
        putIfPresent(errors, "bio", ValidationUtils.validateBio(bio));
        requireNoFieldErrors(errors);

        if (userRepository.existsByUsername(username)) {
            throw usernameExists();
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setUserType(UserType.USER);
        user.setRealName(request.realName().trim());
        user.setPhone(request.phone().trim());
        user.setBio(bio.isEmpty() ? null : bio);
        user.setRegisterTime(now);
        user.setUpdateTime(now);

        try {
            return UserVO.from(userRepository.save(user));
        } catch (DataIntegrityViolationException e) {
            throw usernameExists();
        }
    }

    @Transactional(readOnly = true)
    public UserVO currentUser(Long userId) {
        return UserVO.from(requireUser(userId));
    }

    public void logout(AuthUser currentUser) {
        revocationStore.revokeToken(currentUser.tokenId(), currentUser.tokenExpiresAt());
    }

    @Transactional
    public UserVO updateProfile(Long userId, UpdateProfileRequest request) {
        String phone = trimToEmpty(request.phone());
        String bio = trimToEmpty(request.bio());

        Map<String, String> errors = new LinkedHashMap<>();
        putIfPresent(errors, "phone", ValidationUtils.validatePhone(phone));
        putIfPresent(errors, "bio", ValidationUtils.validateBio(bio));
        requireNoFieldErrors(errors);

        User user = requireUser(userId);
        user.setPhone(phone);
        user.setBio(bio.isEmpty() ? null : bio);
        user.setUpdateTime(LocalDateTime.now());
        return UserVO.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        String oldPassword = request.oldPassword() == null ? "" : request.oldPassword();
        String newPassword = request.newPassword() == null ? "" : request.newPassword();

        Map<String, String> errors = new LinkedHashMap<>();
        if (oldPassword.isEmpty()) {
            errors.put("oldPassword", "原密码不能为空");
        }
        if (!newPassword.isEmpty() && newPassword.equals(oldPassword)) {
            errors.put("newPassword", "新密码不能与原密码相同");
        } else {
            putIfPresent(errors, "newPassword", ValidationUtils.validatePassword(newPassword));
        }
        requireNoFieldErrors(errors);

        User user = requireUser(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ApiCode.OLD_PASSWORD_INCORRECT, ApiCode.OLD_PASSWORD_INCORRECT.getMessage(),
                    Map.of("oldPassword", "原密码错误"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userRepository.save(user);

        // 旧令牌一并失效，前端需重新登录
        revocationStore.bumpVersion(userId);
        loginAttemptTracker.reset(user.getUsername());
    }

    @Transactional(readOnly = true)
    public UsernameAvailabilityVO checkUsername(String username) {
        String normalized = trimToEmpty(username);
        if (normalized.isEmpty()) {
            return new UsernameAvailabilityVO("", false);
        }
        return new UsernameAvailabilityVO(normalized, !userRepository.existsByUsername(normalized));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));
    }

    private BusinessException usernameExists() {
        return new BusinessException(ApiCode.USERNAME_EXISTS, ApiCode.USERNAME_EXISTS.getMessage(),
                Map.of("username", "用户名已存在"));
    }

    private void requireNoFieldErrors(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new BusinessException(ApiCode.PARAM_INVALID, ApiCode.PARAM_INVALID.getMessage(), errors);
        }
    }

    private void putIfPresent(Map<String, String> errors, String field, String message) {
        if (message != null) {
            errors.put(field, message);
        }
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

package com.aurxsiu.bupthelper.common;

/**
 * 服务端校验规则（对应 doc/接口设计-登录模块.md 1.4、1.5），不能只依赖前端校验。
 * 各方法返回 {@code null} 表示校验通过，否则返回可直接展示的中文错误信息。
 */
public final class ValidationUtils {

    private static final int USERNAME_MIN_LENGTH = 3;
    private static final int USERNAME_MAX_LENGTH = 20;
    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final int PASSWORD_MAX_LENGTH = 20;
    private static final int BIO_MAX_LENGTH = 200;
    private static final int REAL_NAME_MAX_LENGTH = 20;

    private ValidationUtils() {
    }

    public static String validateUsername(String username) {
        if (username == null || username.isEmpty()) {
            return "用户名不能为空";
        }
        if (username.length() < USERNAME_MIN_LENGTH || username.length() > USERNAME_MAX_LENGTH
                || !username.chars().allMatch(ValidationUtils::isUsernameChar)) {
            return "用户名为 3~20 位字母、数字或下划线";
        }
        return null;
    }

    public static String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "密码不能为空";
        }
        if (password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
            return "密码长度需为 6~20 位";
        }
        boolean hasLower = false;
        boolean hasUpper = false;
        int digits = 0;
        for (char c : password.toCharArray()) {
            if (c >= '0' && c <= '9') {
                digits++;
            } else if (c >= 'a' && c <= 'z') {
                hasLower = true;
            } else if (c >= 'A' && c <= 'Z') {
                hasUpper = true;
            } else {
                return "密码只能包含字母和数字";
            }
        }
        if (digits < 2) {
            return "密码至少包含 2 个数字";
        }
        if (!hasLower || !hasUpper) {
            return "密码不能全部为大写或全部为小写";
        }
        return null;
    }

    public static String validatePhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return "手机号码不能为空";
        }
        if (phone.length() != 11 || phone.charAt(0) != '1' || phone.charAt(1) < '3' || phone.charAt(1) > '9') {
            return "请输入正确的 11 位手机号码";
        }
        for (char c : phone.toCharArray()) {
            if (c < '0' || c > '9') {
                return "请输入正确的 11 位手机号码";
            }
        }
        return null;
    }

    public static String validateRealName(String realName) {
        if (realName == null || realName.isEmpty()) {
            return "用户姓名不能为空";
        }
        if (realName.length() > REAL_NAME_MAX_LENGTH) {
            return "用户姓名不能超过 20 个字符";
        }
        return null;
    }

    public static String validateBio(String bio) {
        if (bio != null && bio.length() > BIO_MAX_LENGTH) {
            return "用户简介不能超过 200 个字符";
        }
        return null;
    }

    private static boolean isUsernameChar(int c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }
}

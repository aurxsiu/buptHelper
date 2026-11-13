package com.aurxsiu.bupthelper.user.entity;

/**
 * 用户类型：系统管理员 / 普通用户（《需求说明.md》三.1）。
 */
public enum UserType {

    ADMIN("系统管理员"),
    USER("普通用户");

    private final String displayName;

    UserType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

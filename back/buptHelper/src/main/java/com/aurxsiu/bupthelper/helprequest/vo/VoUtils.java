package com.aurxsiu.bupthelper.helprequest.vo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * VO 通用工具：时间格式化，与 doc/接口设计-登录模块.md 1.1 的时间格式一致。
 */
public final class VoUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private VoUtils() {
    }

    public static String format(LocalDateTime value) {
        return value == null ? null : value.format(FORMATTER);
    }
}

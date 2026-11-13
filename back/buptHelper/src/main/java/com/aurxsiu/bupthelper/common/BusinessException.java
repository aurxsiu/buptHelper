package com.aurxsiu.bupthelper.common;

/**
 * 可预期的业务异常，由 {@link GlobalExceptionHandler} 统一转换为统一响应结构。
 * {@code data} 通常为“字段名 -> 错误信息”的映射，便于前端逐字段标红。
 */
public class BusinessException extends RuntimeException {

    private final ApiCode apiCode;
    private final transient Object data;

    public BusinessException(ApiCode apiCode) {
        this(apiCode, apiCode.getMessage(), null);
    }

    public BusinessException(ApiCode apiCode, String message) {
        this(apiCode, message, null);
    }

    public BusinessException(ApiCode apiCode, String message, Object data) {
        super(message);
        this.apiCode = apiCode;
        this.data = data;
    }

    public ApiCode getApiCode() {
        return apiCode;
    }

    public Object getData() {
        return data;
    }
}

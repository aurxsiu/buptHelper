package com.aurxsiu.bupthelper.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 全局异常处理：把所有异常转换为 doc/接口设计-登录模块.md 约定的统一响应结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException ex) {
        ApiCode apiCode = ex.getApiCode();
        return ResponseEntity.status(apiCode.getStatus())
                .body(ApiResponse.error(apiCode, ex.getMessage(), ex.getData()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return badRequest("请求体格式错误");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(ApiCode.PARAM_INVALID.getStatus())
                .body(ApiResponse.error(ApiCode.PARAM_INVALID, ApiCode.PARAM_INVALID.getMessage(),
                        Map.of(ex.getParameterName(), "不能为空")));
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ApiResponse<Object>> handleUnsupportedRequest(Exception ex) {
        return badRequest("请求方式或数据格式不正确");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex) {
        log.error("服务器内部错误", ex);
        return ResponseEntity.status(ApiCode.SERVER_ERROR.getStatus())
                .body(ApiResponse.error(ApiCode.SERVER_ERROR, ApiCode.SERVER_ERROR.getMessage(), null));
    }

    private ResponseEntity<ApiResponse<Object>> badRequest(String message) {
        return ResponseEntity.status(ApiCode.PARAM_INVALID.getStatus())
                .body(ApiResponse.error(ApiCode.PARAM_INVALID, message, null));
    }
}

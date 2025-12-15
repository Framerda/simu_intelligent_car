// src/main/java/com/intelligentcar/exception/GlobalExceptionHandler.java
package com.intelligentcar.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 用于统一处理应用程序中的异常
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Map<String, Object>> handleNotFoundException(NoHandlerFoundException e) {
        return createErrorResponse(HttpStatus.NOT_FOUND,
                "资源未找到",
                e.getMessage());
    }

    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception e) {
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "服务器内部错误",
                e.getMessage());
    }

    /**
     * 创建统一的错误响应
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(
            HttpStatus status, String error, String message) {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("path", "智能小车控制平台");

        return new ResponseEntity<>(errorResponse, status);
    }
}
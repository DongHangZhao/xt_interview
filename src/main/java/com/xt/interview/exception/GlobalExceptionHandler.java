package com.xt.interview.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器。
 *
 * 背景：原项目 Service 层直接抛 RuntimeException，Spring 默认会返回 HTML 错误页，
 * 前端 axios 拦截器读不到 message 字段，只能显示"请求失败"，
 * 导致"部门编码已存在"这类校验信息无法呈现给用户。
 * 这里统一收敛为 JSON 结构 { success, message }，与前端约定一致。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务规则校验失败：返回 400 + 具体提示 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(e.getCode()).body(buildResponse(false, e.getMessage()));
    }
    /** @Valid 参数校验失败：返回 400 + 首个字段错误提示 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return ResponseEntity.badRequest().body(buildResponse(false, message));
    }

    /**
     * 数据库约束冲突兜底。
     *
     * 正常情况下唯一性校验已在 Service 层前置拦截，但并发提交时
     * 两个请求可能同时通过校验，最终仍会被数据库唯一索引拦下。
     * 这里翻译成可读提示，避免用户看到"服务器内部错误"。
     *
     * 注意：部门表的编码/名称唯一索引已在 V4 迁移中降级为普通索引
     * （改为应用层校验），因此这里主要服务于仍保留唯一索引的账号表。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (message.contains("uk_username")) {
            return ResponseEntity.badRequest().body(buildResponse(false, "用户名已存在"));
        }
        log.warn("数据库约束冲突", e);
        return ResponseEntity.badRequest().body(buildResponse(false, "数据冲突，请检查输入内容"));
    }

    /** 兜底：未预期的系统异常，返回 500，不泄露堆栈细节 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildResponse(false, "服务器内部错误"));
    }

    private Map<String, Object> buildResponse(boolean success, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        return response;
    }
}

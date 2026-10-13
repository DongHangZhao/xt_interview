package com.xt.interview.exception;

/**
 * 业务异常：由业务规则校验失败触发（如部门编码重复、账号不存在等）。
 * 与系统异常区分开，全局处理器会对两者返回不同的 HTTP 状态码。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(message, 400);
    }

    public BusinessException(String message, int code) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

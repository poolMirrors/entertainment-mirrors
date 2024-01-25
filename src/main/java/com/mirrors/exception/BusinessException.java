package com.mirrors.exception;

/**
 * 自定义异常（统一）
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/9 15:56
 */
public class BusinessException extends RuntimeException {

    /**
     * 错误信息
     */
    private String message;

    public BusinessException() {
        super();
    }

    public BusinessException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 抛出一般异常
     *
     * @param message
     */
    public static void throwException(String message) {
        throw new BusinessException(message);
    }

    /**
     * 抛出通用异常
     *
     * @param commonException
     */
    public static void throwException(CommonException commonException) {
        throw new BusinessException(commonException.getMessage());
    }

    /**
     * 不写入堆栈信息，提高性能
     *
     * @return
     */
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}

package com.mirrors.exception;

/**
 * 通用异常
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/15 20:17
 */
public enum CommonException {

    UNKNOWN_ERROR("执行过程异常，请重试"),
    PARAMS_ERROR("非法参数"),
    OBJECT_NULL("对象为空"),
    QUERY_NULL("查询结果为空"),
    REQUEST_NULL("请求参数为空");

    private final String message;

    public String getMessage() {
        return message;
    }

    CommonException(String message) {
        this.message = message;
    }
}

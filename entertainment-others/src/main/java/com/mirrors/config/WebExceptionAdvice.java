package com.mirrors.config;

import com.mirrors.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 处理异常，返回Json格式
 */
@Slf4j
@RestControllerAdvice
public class WebExceptionAdvice {

    /**
     * 捕获RuntimeException，统一异常处理
     *
     * @param e
     * @return
     */
    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        log.error(e.toString(), e);
        return Result.fail("服务器异常：" + e.getMessage());
    }
}

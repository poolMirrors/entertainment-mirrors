package com.mirrors.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 全局异常处理器
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/15 20:25
 */
@Slf4j
@RestControllerAdvice // 包括 @ControllerAdvice 和 @ResponseBody
public class BusinessExceptionHandler {

    /**
     * 捕获用户自定义异常
     *
     * @param exception
     * @return
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ExceptionMsgResponse customException(BusinessException exception) {
        log.error("【自定义异常】{}", exception.getMessage(), exception);
        return new ExceptionMsgResponse(exception.getMessage());
    }

    /**
     * 捕获系统异常
     *
     * @param exception
     * @return
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ExceptionMsgResponse exception(Exception exception) {
        log.error("【系统异常】{}", exception.getMessage(), exception);
        if (exception.getMessage().equals("不允许访问")) {
            return new ExceptionMsgResponse("没有操作此功能的权限");
        }
        return new ExceptionMsgResponse(CommonException.UNKNOWN_ERROR.getMessage());
    }

    /**
     * 处理 @Validate 异常信息
     *
     * @param exception 异常信息
     * @return 返回异常响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ExceptionMsgResponse methodArgumentValidException(MethodArgumentNotValidException exception) {
        // 实体类校验信息返回结果绑定
        BindingResult bindingResult = exception.getBindingResult();
        // 校验的错误信息
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        // 记录
        StringBuffer errors = new StringBuffer();
        fieldErrors.forEach((error) -> {
            errors.append(error.getDefaultMessage()).append(",");
        });

        log.error("【参数校验异常】{}", errors, exception);
        return new ExceptionMsgResponse(errors.toString());
    }

}

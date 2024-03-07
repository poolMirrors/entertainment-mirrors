package com.mirrors.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 返回前端的异常信息类
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/15 20:36
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExceptionMsgResponse {

    /**
     * 异常信息
     */
    private String message;

}

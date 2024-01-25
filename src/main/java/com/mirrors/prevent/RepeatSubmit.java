package com.mirrors.prevent;

import java.lang.annotation.*;

/**
 * 自定义 防重提交 注解
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/9 22:01
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RepeatSubmit {

    /**
     * 防重提交，支持 方法参数 和 令牌；
     * 枚举类型，共2种
     */
    enum Type {PARAM, TOKEN}

    /**
     * 默认防重提交策略，方法参数
     *
     * @return
     */
    Type limitType() default Type.PARAM;

    /**
     * 加锁过期时间，默认5秒
     *
     * @return 过期时间
     */
    long lockTime() default 5;
}

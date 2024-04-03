package com.mirrors.prevent;

import java.lang.annotation.*;

/**
 * 接口防刷注解（控制 相同ip和用户 再规定时间内的 最大访问次数）
 * <p>
 * 参考<a href="https://juejin.cn/post/7265565809823760441">接口防刷</a>
 * </p>
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/9 22:07
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RestrictRequest {

    /**
     * 时间间隔，默认5秒
     *
     * @return
     */
    long interval() default 5;

    /**
     * 最大访问次数，默认5000次
     *
     * @return
     */
    int count() default 5000;

    /**
     * 提示信息
     *
     * @return
     */
    String message() default "";
}

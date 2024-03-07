package com.mirrors.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用作逻辑过期
 */
@Data
public class RedisData {

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * Shop 内部数据
     */
    private Object data;
}

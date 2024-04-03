package com.mirrors.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 基于Redis的订单id生成工具类
 */
@Component
public class RedisIDCreator {

    /**
     * 2023年1月1日0时0分0秒 的 toEpochSecond（开始时间戳）
     */
    private static final long BEGIN_TIMESTAMP = 1672531200L;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成id
     *
     * @param keyPrefix
     * @return
     */
    public long nextId(String keyPrefix) {
        // 1.符号位0

        // 2.当前时间戳 - BEGIN_TIMESTAMP
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        // 3.序列号（32位）
        // 获取当前日期，每一天都有新的起点【重点】
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long increment = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        return timestamp << 32 | increment;
    }
    public static void main(String[] args) {
        LocalDateTime begin = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
        long second = begin.toEpochSecond(ZoneOffset.UTC);
        // 1672531200
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long increment = 99999L;

        System.out.println( timestamp << 32 | increment );
    }
}

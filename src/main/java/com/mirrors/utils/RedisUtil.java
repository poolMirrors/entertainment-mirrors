package com.mirrors.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mirrors.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * redis工具类【重点】
 */
@Slf4j
@Component
public class RedisUtil {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 获取锁
     *
     * @param key
     * @return
     */
    private boolean tryLock(String key) {
        // setnx key value
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        // 不要直接return flag，会有自动拆箱，出现空指针异常
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 【缓存击穿】释放锁
     *
     * @param key
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }


    /**
     * redis过期时间
     *
     * @param key
     * @param data
     * @param time
     * @param timeUnit
     */
    public void setWithExpire(String key, Object data, Long time, TimeUnit timeUnit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(data), time, timeUnit);
    }

    /**
     * 逻辑过期，redis永不过期
     *
     * @param key
     * @param data
     * @param time
     * @param timeUnit
     */
    public void setWithLogicExpire(String key, Object data, Long time, TimeUnit timeUnit) {
        // 设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(data);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));
        // 写入Redis缓存
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 【缓存穿透】；
     * 通过 id 查询实体，使用 【返回空对象】 解决 【缓存穿透】 问题
     *
     * @param keyPrefix
     * @param id
     * @param tClass
     * @param queryDB
     * @param time
     * @param timeUnit
     * @param <T>       定义泛型，数据类型
     * @param <Tid>     定义泛型，id类型
     * @return
     */
    public <T, Tid> T queryWithPassThrough(String keyPrefix, Tid id, Class<T> tClass, Function<Tid, T> queryDB, Long time, TimeUnit timeUnit) {
        // 从redis查缓存
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, tClass);
        }

        //【缓存穿透】判断命中是否为空值，""空字符串
        if (json != null) {
            return null;
        }

        // 不存在，查询数据库
        T t = queryDB.apply(id);
        if (t == null) {
            //【缓存穿透】空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL_MINUTES, TimeUnit.MINUTES);
            return null;
        }

        // 存在，写入redis，并设置过期时间
        this.setWithExpire(key, t, time, timeUnit);

        // 返回
        return t;
    }

    /**
     * 通过id查询实体 Bean 信息，使用【逻辑过期】策略 解决【缓存击穿】；
     * 使用逻辑过期策略一般需要提前将热点 Key 导入数据库中
     *
     * @param keyPrefix
     * @param id
     * @param tClass
     * @param queryDB
     * @param time
     * @param timeUnit
     * @param <T>
     * @param <Tid>
     * @return
     */
    public <T, Tid> T queryWithLogicExpire(String keyPrefix, Tid id, Class<T> tClass, Function<Tid, T> queryDB, Long time, TimeUnit timeUnit) {
        // 从redis查店铺缓存，是否存在
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(json)) {
            return null;
        }

        // 存在，判断是否过期
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        JSONObject jsonObject = (JSONObject) redisData.getData();
        T t = JSONUtil.toBean(jsonObject, tClass);

        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())) { // 如果过期时间在now之后，说明还没过期
            return t;
        }

        // 过期，需要缓存重建，获取锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean lock = tryLock(lockKey);
        if (lock == false) {
            return t;
        }

        // 获取锁成功，再次查询redis，进行Double Check
        //【线程A和B同时判断是过期】 A重建缓存后释放锁，但是B由于网络原因，在A释放锁后才开始获取锁
        json = stringRedisTemplate.opsForValue().get(key);
        redisData = JSONUtil.toBean(json, RedisData.class);
        jsonObject = (JSONObject) redisData.getData();
        t = JSONUtil.toBean(jsonObject, tClass);

        expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())) { // 如果过期时间在now之后，说明还没过期
            return t;
        }

        // Double Check后，开启线程缓存重建
        CACHE_REBUILD_EXECUTOR.submit(() -> {
            try {
                // 查询数据库
                T t1 = queryDB.apply(id);
                // 写入redis，逻辑过期
                this.setWithLogicExpire(key, t1, time, timeUnit);

            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                // 释放锁
                unlock(lockKey);
            }
        });

        return t;
    }

}

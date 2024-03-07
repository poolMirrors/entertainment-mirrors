package com.mirrors.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的分布式锁
 */
public class SimpleRedisLock implements ILock {

    /**
     * 锁的名称，不同业务不同名字
     */
    private String name;

    private StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "lock:";

    /**
     * 线程id前缀，确保不同的JVM的线程一定存在不一样的线程id
     */
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    /**
     * 脚本的初始化，声明为static final，避免每次获取锁都加载，避免重复IO
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    /**
     * 静态代码块，负责初始化脚本
     */
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 尝试获取锁
     *
     * @param timeoutSec 锁过期时间
     * @return
     */
    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取锁
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        Boolean success = stringRedisTemplate
                .opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        // 对于自动拆箱，要判断是否为null
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放锁，确保当前id只能释放自己获取的锁，避免并发时，阻塞导致释放其他线程的锁
     */
    @Override
    public void unlock() {
        // 调用Lua脚本的释放锁；
        // 使得释放锁的两个操作（1、获取线程标识；2、释放锁）成为原子操作，避免高并发异常情况
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId()
        );
    }

    //@Override
    //public void unlock() {
    //    String threadId = ID_PREFIX + Thread.currentThread().getId();
    //    // 拿到锁的值（线程唯一标识）
    //    String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
    //
    //    // 标识一致就释放锁
    //    if(threadId.equals(id)) {
    //        stringRedisTemplate.delete(KEY_PREFIX + name);
    //    }
    //}
}

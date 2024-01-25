package com.mirrors.utils;

/**
 * 锁接口
 */
public interface ILock {

    /**
     * 尝试获得锁
     *
     * @param timeoutSec 锁过期时间
     * @return 获取不到返回false
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unlock();
}

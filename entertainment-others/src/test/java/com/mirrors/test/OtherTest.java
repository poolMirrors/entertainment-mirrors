package com.mirrors.test;

import org.apache.logging.log4j.spi.CopyOnWrite;
import org.junit.Test;

import java.util.concurrent.*;

/**
 * @author mirrors
 * @version 1.0
 * @date 2024/3/5 17:17
 */
public class OtherTest {

    @Test
    public void test() {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10, 10, 0, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
        threadPoolExecutor.execute(() -> {
            System.out.println("999");
        });

        ThreadLocal<Object> threadLocal = new ThreadLocal<>();
        Thread thread = new Thread();

        
    }
}

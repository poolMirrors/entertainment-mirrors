package com.mirrors.test;

import com.mirrors.entity.Shop;
import com.mirrors.entity.TimeTaskMessage;
import com.mirrors.mapper.TimeTaskMessageMapper;
import com.mirrors.service.impl.TimeTaskMessageServiceImpl;
import com.mirrors.service.impl.ShopServiceImpl;
import com.mirrors.utils.RedisConstants;
import com.mirrors.utils.RedisIDCreator;
import com.mirrors.utils.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class RedisTest {

    @Resource
    private ShopServiceImpl shopService;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private RedisIDCreator redisIDCreator;

    @Autowired
    private TimeTaskMessageServiceImpl service;

    private ExecutorService executorService = Executors.newFixedThreadPool(500);

    /**
     * 测试TimeTaskMessage的sql语句
     */
    @Test
    public void testTimeTaskMessageSql() {
        TimeTaskMessageMapper baseMapper = service.getBaseMapper();
        List<TimeTaskMessage> messages = baseMapper.selectListByShardIndex(1, 0, "blog", 5);
        System.out.println(messages);
    }

    /**
     * 测试全局唯一id
     */
    @Test
    public void testGlobalID() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIDCreator.nextId("order");
                System.out.println(id);
            }
            countDownLatch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            executorService.submit(task);
        }
        countDownLatch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }


    /**
     * 店铺预热
     */
    @Test
    public void preheat() throws InterruptedException {
        for (long i = 1L; i <= 14; i++) {
            Shop shop = shopService.getById(i);
            redisUtil.setWithLogicExpire(RedisConstants.CACHE_SHOP_KEY + shop.getId(), shop, 10L, TimeUnit.SECONDS);
        }
    }


}

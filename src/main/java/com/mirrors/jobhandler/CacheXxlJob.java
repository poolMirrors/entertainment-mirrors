package com.mirrors.jobhandler;

import com.mirrors.entity.Shop;
import com.mirrors.service.IShopService;
import com.mirrors.utils.RedisConstants;
import com.mirrors.utils.RedisUtil;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 与redis相关的任务调度
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/17 17:33
 */
@Slf4j
@Component
public class CacheXxlJob {

    @Autowired
    IShopService shopService;

    @Autowired
    RedisUtil redisUtil;

    /**
     * 缓存预热
     * <p>
     * 路由策略：第一个
     *
     * @throws InterruptedException
     */
    //@XxlJob("preheat")
    @Deprecated
    public void preheatOne() throws InterruptedException {
        for (long i = 1L; i <= 14; i++) {
            Shop shop = shopService.getById(i);
            redisUtil.setWithLogicExpire(RedisConstants.CACHE_SHOP_KEY + shop.getId(), shop, 10L, TimeUnit.SECONDS);
        }
    }

    /**
     * 缓存预热；需要本项目部署两个节点以上 (-Dserver.port=8082 -Dxxl-job.executor.port=9998)
     * <p>
     * 路由策略：分片广播
     *
     * @throws InterruptedException
     */
    @XxlJob("preheat")
    //@Deprecated
    public void preheatFragment() throws InterruptedException {
        log.info("缓存预热；路由策略：分片广播");
        // TODO 分片广播进行缓存预热
    }
}

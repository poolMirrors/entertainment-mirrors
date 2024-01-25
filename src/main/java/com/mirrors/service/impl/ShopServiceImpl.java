package com.mirrors.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mirrors.dto.Result;
import com.mirrors.entity.Shop;
import com.mirrors.mapper.ShopMapper;
import com.mirrors.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mirrors.utils.RedisConstants;
import com.mirrors.utils.RedisData;
import com.mirrors.utils.RedisUtil;
import com.mirrors.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 店铺 服务实现类
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisUtil redisUtil;

    /**
     * 【缓存击穿】获取锁
     *
     * @param key
     * @return
     */
    @Deprecated
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
    @Deprecated
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 【缓存穿透】
     *
     * @param id
     * @return
     */
    @Deprecated
    private Shop queryWithPassThrough(Long id) {
        // 从redis查缓存
        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        if (StrUtil.isNotBlank(shopJson)) {
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        //【缓存穿透】判断命中是否为空值，""空字符串
        if (shopJson != null) {
            return null;
        }

        // 不存在，查询数据库
        Shop shop = getById(id);
        if (shop == null) {
            //【缓存穿透】空值写入redis
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, "", RedisConstants.CACHE_NULL_TTL_MINUTES, TimeUnit.MINUTES);
            return null;
        }

        // 存在，写入redis，并设置过期时间
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL_MINUTES, TimeUnit.MINUTES);

        return shop;
    }

    /**
     * 【缓存穿透 + 击穿】锁机制
     *
     * @param id
     * @return
     */
    @Deprecated
    private Shop queryWithMutex(Long id) {
        // 从redis查缓存
        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //【缓存穿透】判断命中是否为空值，""空字符串
        if (shopJson != null) {
            return null;
        }

        //【缓存击穿】
        // 获取互斥锁
        Shop shop;
        try {
            boolean lock = tryLock(RedisConstants.LOCK_SHOP_KEY + id);
            if (lock == false) {
                Thread.sleep(50);
                // 递归？重新查询
                queryWithMutex(id);
            }

            // 成功获取锁，再次查询redis；存在就返回，不存在就查询数据库
            shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
            if (StrUtil.isNotBlank(shopJson)) {
                return JSONUtil.toBean(shopJson, Shop.class);
            }

            // 不存在，查询数据库
            shop = getById(id);
            Thread.sleep(200); // 模拟重建延迟
            if (shop == null) {
                //【缓存穿透】空值写入redis
                stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, "", RedisConstants.CACHE_NULL_TTL_MINUTES, TimeUnit.MINUTES);
                return null;
            }

            // 存在，写入redis，并设置过期时间
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL_MINUTES, TimeUnit.MINUTES);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 释放互斥锁
            unlock(RedisConstants.LOCK_SHOP_KEY + id);
        }

        return shop;
    }

    /**
     * 封装逻辑过期时间，预热，单元测试、xxl-job
     *
     * @param id
     * @param expireSeconds
     */
    @Deprecated
    public void saveShop2RedisByLogicExpire(Long id, Long expireSeconds) throws InterruptedException {
        // 查询店铺数据
        Shop shop = getById(id);
        Thread.sleep(50); // 模拟延迟

        // 封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));

        // 写入redis，不设置redis的过期时间
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 线程池
     */
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 【缓存击穿】逻辑过期时间，前提要有redis预热所有店铺（不用考虑穿透）
     *
     * @param id
     * @return
     */
    @Deprecated
    private Shop queryWithLogicExpire(Long id) {
        // 从redis查店铺缓存，是否存在
        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        if (StrUtil.isBlank(shopJson)) {
            return null;
        }

        // 存在，判断是否过期
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        JSONObject jsonObject = (JSONObject) redisData.getData();
        Shop shop = JSONUtil.toBean(jsonObject, Shop.class);

        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())) { // 如果过期时间在now之后，说明还没过期
            return shop;
        }

        // 过期，需要缓存重建，获取锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean lock = tryLock(lockKey);
        if (lock == false) {
            return shop;
        }

        // 获取锁成功，再次查询redis，进行Double Check
        //【线程A和B同时判断是过期】 A重建缓存后释放锁，但是B由于网络原因，在A释放锁后才开始获取锁
        shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        redisData = JSONUtil.toBean(shopJson, RedisData.class);
        jsonObject = (JSONObject) redisData.getData();
        shop = JSONUtil.toBean(jsonObject, Shop.class);

        expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())) { // 如果过期时间在now之后，说明还没过期
            return shop;
        }

        // Double Check后，开启线程缓存重建
        CACHE_REBUILD_EXECUTOR.submit(() -> {
            try {
                // 缓存重建，逻辑10s过期，为了方便测试
                this.saveShop2RedisByLogicExpire(id, 10L);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                // 释放锁
                unlock(lockKey);
            }
        });

        return shop;
    }


    /**
     * 根据id查询数据库 商铺信息
     *
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        //【缓存穿透】
        //Shop shop = redisUtil.queryWithPassThrough(
        //        RedisConstants.CACHE_SHOP_KEY,
        //        id,
        //        Shop.class,
        //        id2 -> getById(id2),
        //        RedisConstants.CACHE_SHOP_TTL_MINUTES,
        //        TimeUnit.MINUTES
        //);

        //【缓存穿透 + 击穿】
        // Shop shop = queryWithMutex(id);

        //【缓存击穿】逻辑过期时间，要预热
        Shop shop = redisUtil.queryWithLogicExpire(
                RedisConstants.CACHE_SHOP_KEY,
                id,
                Shop.class,
                id2 -> getById(id2),
                20L, // 10s为了测试
                TimeUnit.SECONDS // 10s为了测试
        );

        if (shop == null) {
            return Result.fail("店铺不存在");
        }

        return Result.ok(shop);
    }

    /**
     * 更新redis和数据库
     *
     * @param shop
     * @return
     */
    @Override
    @Transactional // 添加事务注解控制原子性操作，数据库回滚
    public Result updateData(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }

        // 先更新数据库
        updateById(shop);
        // 再删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);

        return Result.ok();
    }

    /**
     * 根据类型查店铺
     *
     * @param typeId
     * @param current
     * @param x
     * @param y
     * @return
     */
    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 判断是否根据坐标查询
        if (x == null || y == null) {
            Page<Shop> page = query().eq("type_id", typeId).page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }

        // 计算分页参数，给redis用
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        // 查询redis，按照距离排序，分页
        String key = RedisConstants.SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().search(
                key,
                GeoReference.fromCoordinate(x, y),
                new Distance(5000, Metrics.MILES),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
        );

        // 解析id
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if (list.size() < from) {
            return Result.ok(Collections.emptyList()); // 没有下一页
        }

        // 截取 from ~ end
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>();

        list.stream().skip(from).forEach(res -> {

            String shopId = res.getContent().getName();
            ids.add(Long.valueOf(shopId));

            Distance distance = res.getDistance();
            distanceMap.put(shopId, distance);
        });

        // 查询店铺
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("order by field (id, " + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }

        // 返回数据
        return Result.ok(shops);
    }
}

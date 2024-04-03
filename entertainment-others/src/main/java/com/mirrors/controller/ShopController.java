package com.mirrors.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mirrors.dto.Result;
import com.mirrors.entity.Shop;
import com.mirrors.service.IShopService;
import com.mirrors.utils.RedisConstants;
import com.mirrors.utils.RedisUtil;
import com.mirrors.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 店铺管理
 */
@Slf4j
@RestController
@RequestMapping("/shop")
public class ShopController {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private IShopService shopService;

    // --------------------------------------主要实现--------------------------------------------------

    /**
     * 根据id查询商铺信息【经过缓存redis】
     *
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        Result result = shopService.queryById(id);
        log.info(result.toString());
        return result;
    }

    /**
     * 新增商铺信息
     *
     * @param shop 商铺数据
     * @return 商铺id
     */
    @PostMapping
    public Result saveShop(@RequestBody Shop shop) {
        // 新增数据库
        shopService.save(shop);
        // 由于是新增，保证写入Redis一定成功即可
        redisUtil.setWithLogicExpire(RedisConstants.CACHE_SHOP_KEY + shop.getId(), shop, 6L, TimeUnit.HOURS);
        return Result.ok(shop.getId());
    }

    /**
     * 更新商铺信息
     *
     * @param shop 商铺数据
     * @return 无
     */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        // 更新数据库
        Result result = shopService.updateData(shop);
        // 删除Redis
        stringRedisTemplate.opsForValue().getAndDelete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
        log.info(result.toString());
        return result;
    }

    // --------------------------------------舍弃功能--------------------------------------------------

    /**
     * 根据 商铺类型 和 距离远近 分页查询商铺信息
     *
     * @param typeId  商铺类型
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/type")
    public Result queryShopByTypeAndPos(@RequestParam("typeId") Integer typeId, @RequestParam(value = "current", defaultValue = "1") Integer current, @RequestParam(value = "x", required = false) Double x, @RequestParam(value = "y", required = false) Double y) {
        Result result = shopService.queryShopByTypeAndPos(typeId, current, x, y);
        System.out.println(result);
        return result;
    }

    /**
     * 根据商铺名称关键字分页查询商铺信息
     *
     * @param name    商铺名称关键字
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/name")
    public Result queryShopByName(@RequestParam(value = "name", required = false) String name, @RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 根据类型分页查询
        Page<Shop> page = shopService
                .query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }
}

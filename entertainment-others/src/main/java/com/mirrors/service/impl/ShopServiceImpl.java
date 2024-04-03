package com.mirrors.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mirrors.dto.Result;
import com.mirrors.entity.Shop;
import com.mirrors.mapper.ShopMapper;
import com.mirrors.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mirrors.utils.RedisConstants;
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
import java.util.*;
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
     * 根据id查询数据库 商铺信息
     *
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        // 逻辑过期时间，要预热
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

        // 先更新数据库，再删除缓存，适用于对数据一致性要求较高，且写操作相对较少的场景
        updateById(shop);
        // TODO 保证两个操作成功执行：(1)重试机制，将要删除的数据传入MQ，从MQ中获取数据进行删除（2）订阅 binlog 日志，拿到具体要操作的数据，然后再执行缓存删除
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);

        // Canal 模拟 MySQL 主从复制的交互协议，把自己伪装成一个 MySQL 的从节点，向 MySQL 主节点发送 dump 请求，
        // MySQL 收到请求后，就会开始推送 Binlog 给 Canal，Canal 解析 Binlog 字节流之后，转换为便于读取的结构化数据，供下游程序订阅使用

        return Result.ok();
    }

    /**
     * 根据 商铺类型 和 距离远近 分页查询商铺信息
     *
     * @param typeId
     * @param current
     * @param x
     * @param y
     * @return
     */
    @Override
    public Result queryShopByTypeAndPos(Integer typeId, Integer current, Double x, Double y) {
        // 判断是否根据坐标查询
        if (x == null || y == null) {
            Page<Shop> page = query().eq("type_id", typeId).page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }

        // 坐标都不为空，要判断远近

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

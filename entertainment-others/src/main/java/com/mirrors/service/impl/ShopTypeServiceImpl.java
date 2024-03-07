package com.mirrors.service.impl;

import cn.hutool.json.JSONUtil;
import com.mirrors.dto.Result;
import com.mirrors.entity.ShopType;
import com.mirrors.mapper.ShopTypeMapper;
import com.mirrors.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mirrors.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 店铺类型 服务实现类
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询店铺分类
     *
     * @return
     */
    @Override
    public Result queryTypeList() {
        String key = RedisConstants.CACHE_SHOP_TYPE_KEY;
        List<String> strings = stringRedisTemplate.opsForList().range(key, 0, -1);// 以 -1 表示列表的最后一个元素
        List<ShopType> shopTypes = new ArrayList<>();
        // 判断是否存在
        if (strings != null && strings.size() > 0) {
            for (String string : strings) {
                shopTypes.add(JSONUtil.toBean(string, ShopType.class));
            }
            return Result.ok(shopTypes);
        }
        // 不存在，查询数据库
        shopTypes = query().orderByAsc("sort").list();

        strings = new ArrayList<>();
        for (ShopType shopType : shopTypes) {
            strings.add(JSONUtil.toJsonStr(shopType));
        }

        // 写入redis
        stringRedisTemplate.opsForList().rightPushAll(key, strings);

        return Result.ok(shopTypes);
    }
}

package com.mirrors.service;

import com.mirrors.dto.Result;
import com.mirrors.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 店铺服务类
 */
public interface IShopService extends IService<Shop> {

    /**
     * 根据id查询店铺
     *
     * @param id
     * @return
     */
    Result queryById(Long id);

    /**
     * 更新
     *
     * @param shop
     * @return
     */
    Result updateData(Shop shop);

    /**
     * 根据 商铺类型 和 距离远近 分页查询商铺信息
     *
     * @param typeId
     * @param current
     * @param x
     * @param y
     * @return
     */
    Result queryShopByTypeAndPos(Integer typeId, Integer current, Double x, Double y);
}

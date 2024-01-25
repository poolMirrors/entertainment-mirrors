package com.mirrors.service;

import com.mirrors.dto.Result;
import com.mirrors.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 商品类型服务类
 */
public interface IShopTypeService extends IService<ShopType> {

    /**
     * 查询商铺类型列表
     *
     * @return
     */
    Result queryTypeList();
}

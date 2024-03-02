package com.mirrors.controller;


import com.mirrors.dto.Result;
import com.mirrors.entity.ShopType;
import com.mirrors.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 店铺类型管理
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    /**
     * 查询店铺类型
     *
     * @return
     */
    @GetMapping("list")
    public Result queryTypeList() {
        // 1.直接访问数据库
        //List<ShopType> typeList = typeService.query().orderByAsc("sort").list();
        //return Result.ok(typeList);

        // 2.经过redis
        Result result = typeService.queryTypeList();
        System.out.println(result);
        return result;
    }
}

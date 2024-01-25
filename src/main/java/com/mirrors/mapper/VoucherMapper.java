package com.mirrors.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mirrors.entity.Voucher;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface VoucherMapper extends BaseMapper<Voucher> {

    /**
     * 根据店铺id，查询优惠券
     *
     * @param shopId
     * @return
     */
    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
}

package com.mirrors.service;

import com.mirrors.dto.Result;
import com.mirrors.entity.Voucher;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 优惠券服务类
 */
public interface IVoucherService extends IService<Voucher> {

    /**
     * 查询商品优惠券
     *
     * @param shopId
     * @return
     */
    Result queryVoucherOfShop(Long shopId);

    /**
     * 添加秒杀券
     *
     * @param voucher
     */
    void addSeckillVoucher(Voucher voucher);
}

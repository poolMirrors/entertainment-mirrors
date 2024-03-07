package com.mirrors.controller;


import com.mirrors.dto.Result;
import com.mirrors.prevent.RestrictRequest;
import com.mirrors.service.IVoucherOrderService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 订单管理
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Resource
    private IVoucherOrderService voucherOrderService;

    /**
     * 秒杀下单
     *
     * @param voucherId
     * @return
     */
    @PostMapping("/seckill/{id}")
    @RestrictRequest(interval = 3, count = 3000) // TODO 限制接口访问（限流）
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        Result result = voucherOrderService.seckillVoucherRabbitMQ(voucherId);
        System.out.println(result);
        return result;
    }
}

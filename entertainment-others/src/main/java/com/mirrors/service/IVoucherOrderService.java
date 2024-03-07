package com.mirrors.service;

import com.mirrors.dto.Result;
import com.mirrors.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 优惠券订单服务类接口
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    //--------------------------------------------同步-----------------------------------------------

    /**
     * 同步下单抢购
     *
     * @param voucherId
     * @return
     */
    Result seckillVoucherSync(Long voucherId);

    /**
     * 同步下单，创建订单，写入数据库
     *
     * @param voucherId
     * @param userId
     * @return
     */
    Result createVoucherOrderSync(Long voucherId, Long userId);

    //--------------------------------------------异步，阻塞队列-----------------------------------------------

    /**
     * 异步下单抢购，阻塞队列
     *
     * @param voucherId
     * @return
     */
    Result seckillVoucherAsync(Long voucherId);

    /**
     * 异步下单，创建订单
     *
     * @param voucherOrder
     */
    void createVoucherOrderAsync(VoucherOrder voucherOrder);

    //--------------------------------------------基于redis的Stream消息队列-----------------------------------------------


    /**
     * 基于redis的Stream，消息队列
     *
     * @param voucherId
     * @return
     */
    Result seckillVoucherStream(Long voucherId);

    /**
     * 基于redis的Stream消息队列，下单
     *
     * @param voucherOrder
     */
    void createVoucherOrderStream(VoucherOrder voucherOrder);

    //--------------------------------------------基于 RabbitMQ-----------------------------------------------

    /**
     * 基于 RabbitMQ 实现
     *
     * @param voucherId
     * @return
     */
    Result seckillVoucherRabbitMQ(Long voucherId);

    /**
     * 基于 RabbitMQ，下单
     *
     * @param voucherOrder
     */
    void createVoucherOrderRabbitMQ(VoucherOrder voucherOrder);

}

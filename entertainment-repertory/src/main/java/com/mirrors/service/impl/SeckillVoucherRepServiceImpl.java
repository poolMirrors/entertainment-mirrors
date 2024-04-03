package com.mirrors.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mirrors.mapper.SeckillVoucherMapper;
import com.mirrors.pojo.SeckillVoucher;
import com.mirrors.repertory.ISeckillVoucherRepService;
import com.mirrors.service.ITimeTaskMessageService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author mirrors
 * @version 1.0
 * @date 2024/3/7 15:15
 */
@Service
//@DubboService
public class SeckillVoucherRepServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherRepService {

    //@DubboReference
    //private ITimeTaskMessageService timeTaskMessageService;

    /**
     * 如果引入mq；库存服务，接收MQ消息，修改库存表，需要保证幂等操作
     *
     * @param voucherId
     * @return
     */
    @Override
    @Transactional
    public boolean reduceSeckillVoucherRep(Long voucherId) {
        // 修改
        boolean update = update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0) //【乐观锁】只要库存大于0就可以秒杀成功（超卖问题），优化需要比较version
                .update();

        // TODO 使用MQ发送消息，给订单服务，删除消息（需要消息id）

        return update;
    }
}

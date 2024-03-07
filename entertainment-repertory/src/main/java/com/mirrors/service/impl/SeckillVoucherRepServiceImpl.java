package com.mirrors.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mirrors.mapper.SeckillVoucherMapper;
import com.mirrors.pojo.SeckillVoucher;
import com.mirrors.repertory.ISeckillVoucherRepService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author mirrors
 * @version 1.0
 * @date 2024/3/7 15:15
 */
@DubboService
public class SeckillVoucherRepServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherRepService {

    @Override
    @Transactional
    public boolean reduceSeckillVoucherRep(Long voucherId) {
        boolean update = update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0) //【乐观锁】只要库存大于0就可以秒杀成功（超卖问题），优化需要比较version
                .update();

        return update;
    }
}

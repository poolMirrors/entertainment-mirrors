package com.mirrors.test;

import com.mirrors.service.impl.SeckillVoucherRepServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * @author mirrors
 * @version 1.0
 * @date 2024/3/7 19:51
 */
@SpringBootTest
public class VoucherTest {

    @Resource
    private SeckillVoucherRepServiceImpl voucherRepService;

    @Test
    public void test() {
        //Voucher byId = voucherRepService.getById(13L);

        voucherRepService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", 13L)
                .gt("stock", 0) //【乐观锁】只要库存大于0就可以秒杀成功（超卖问题），优化需要比较version
                .update();

    }
}

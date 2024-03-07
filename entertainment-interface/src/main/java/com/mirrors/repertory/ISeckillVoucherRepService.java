package com.mirrors.repertory;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mirrors.pojo.SeckillVoucher;
import com.mirrors.pojo.Voucher;

/**
 * @author mirrors
 * @version 1.0
 * @date 2024/3/7 15:15
 */
public interface ISeckillVoucherRepService extends IService<SeckillVoucher> {

    /**
     * 扣减库存
     * @param voucherId
     * @return
     */
    boolean reduceSeckillVoucherRep(Long voucherId);
}

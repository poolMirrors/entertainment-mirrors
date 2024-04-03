package com.mirrors.jobhandler;

import com.mirrors.jobhandler.abs.TimeTaskMessageAbstract;
import com.mirrors.pojo.SeckillVoucher;
import com.mirrors.pojo.TimeTaskMessage;
import com.mirrors.repertory.ISeckillVoucherRepService;
import com.mirrors.service.ITimeTaskMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 扫描信息表，分布式事务，调用库存服务
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/3/13 9:18
 */
@Slf4j
@Component
@Deprecated
public class RepertoryXxlJob extends TimeTaskMessageAbstract {

    @Autowired
    private ISeckillVoucherRepService seckillVoucherRepService;

    @Autowired
    private ITimeTaskMessageService timeTaskMessageService;

    /**
     * 优惠券库存消息类型
     */
    public static final String MESSAGE_TYPE = "voucherRepertory";

    /**
     * 任务调度入口，可以设置为每1秒进行调用
     */
    //@XxlJob("voucherRepertory")
    public void voucherRepertory() {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.info("shardIndex = {}, shardTotal = {}.", shardIndex, shardTotal);
        // 参数：分片序号，分片总数，消息类型，一次最多取到的任务数，一次任务调度执行的超时时间(秒)
        process(shardIndex, shardTotal, MESSAGE_TYPE, 5, 60);
    }

    @Override
    @Transactional
    public boolean execute(TimeTaskMessage timeTaskMessage) {
        // 获取相关的业务信息
        String businessKey1 = timeTaskMessage.getBusinessKey1();
        Long voucherId = Long.parseLong(businessKey1);

        // 消息id
        Long id = timeTaskMessage.getId();
        // 拿到服务
        ITimeTaskMessageService timeTaskMessageService = getTimeTaskMessageService();
        // 保证消息幂等性（这里采用数据库增加一个字段实现判断）
        int stageOne = timeTaskMessageService.getStageOne(id);
        if (stageOne == 1) {
            log.warn("已扣减库存，直接返回，voucherId：{}", voucherId);
            return true;
        }

        // 扣减库存
        seckillVoucherRepService.reduceSeckillVoucherRep(voucherId);

        // 删除消息表
        timeTaskMessageService.removeById(timeTaskMessage.getId());

        return true;
    }
}

package com.mirrors.jobhandler;

import com.mirrors.entity.SeckillVoucher;
import com.mirrors.entity.TimeTaskMessage;
import com.mirrors.jobhandler.abs.TimeTaskMessageAbstract;
import com.mirrors.mapper.BlogMapper;
import com.mirrors.mq.MqSender;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author mirrors
 * @version 1.0
 * @date 2024/4/2 9:28
 */
@Slf4j
@Component
public class RepertoryXxjJob extends TimeTaskMessageAbstract {

    /**
     * 发布博客 消息类型
     */
    public static final String MESSAGE_TYPE = "voucherRepertory";

    @Resource
    private MqSender mqSender;

    @Resource
    private RestHighLevelClient restHighLevelClient;

    /**
     * 任务调度入口，可以设置为每1秒进行调用
     */
    @XxlJob("repertory")
    public void repertory() {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.info("shardIndex = {}, shardTotal = {}.", shardIndex, shardTotal);
        // 参数：分片序号，分片总数，消息类型，一次最多取到的任务数，一次任务调度执行的超时时间(秒)
        process(shardIndex, shardTotal, MESSAGE_TYPE, 5, 60);
    }

    // -----------------------------------业务流程-----------------------------------------------

    @Override
    public boolean execute(TimeTaskMessage timeTaskMessage) {
        Long voucherId = Long.valueOf(timeTaskMessage.getBusinessKey1());

        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucherId);

        mqSender.sendRepertoryMessage(seckillVoucher, true);
        // 返回false，等待库存服务进行调用删除消息
        return false;
    }

}

package com.mirrors.mq;

import com.mirrors.pojo.SeckillVoucher;
import com.mirrors.pojo.TimeTaskMessage;
import com.mirrors.service.impl.SeckillVoucherRepServiceImpl;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RabbitMQ 消息接收器类
 * <p>
 * 注解 @RabbitListener 标注在类上面表示当有收到消息的时候，就交给 @RabbitHandler 的方法处理，根据接受的参数类型进入具体的方法中；
 * 参考<a href="https://blog.csdn.net/sliver1836/article/details/119734239">@RabbitListener与@RabbitHandler/a>
 * </p>
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/9 21:35
 */
@Slf4j
@Component
public class MqReceiver {

    @Resource
    private SeckillVoucherRepServiceImpl seckillVoucherRepService;

    /**
     * 线程池大小为16，同时处理16个
     */
    private final ExecutorService threadPool = Executors.newFixedThreadPool(16);


    /**
     * 推模式？接收到消息 <a href="https://blog.csdn.net/liqinglonguo/article/details/134029011">channel.basicAck</a>
     * <p>
     * 注解 @Payload吗，队列中的json字符串变成对象的注解
     * </P>
     *
     * @param seckillVoucher
     * @param channel
     * @param message
     */
    @RabbitListener(queues = "repertory.queue", ackMode = "MANUAL") // queues 指定监听的队列名称；手动 ack
    public void receiveRepertory(@Payload SeckillVoucher seckillVoucher, Channel channel, Message message) {

        log.info("接收到的订单消息：" + seckillVoucher);
        // 线程池执行
        threadPool.submit(() -> {
            try {
                // 扣减库存
                seckillVoucherRepService.reduceSeckillVoucherRep(seckillVoucher.getVoucherId());

            } catch (Exception e1) {
                // 先本地catch异常，再抛出
                log.warn("订单处理异常，重新尝试。");
                try {
                    seckillVoucherRepService.reduceSeckillVoucherRep(seckillVoucher.getVoucherId());
                } catch (Exception e2) {
                    log.error("订单处理失败：", e2);
                    throw new RuntimeException();
                    // TODO 第二次处理失败，则更改 Redis 中的数据（也可以将消息放入 异常订单数据库 或 队列 中特殊处理）-如回滚库存等操作
                }
            }
            // 手动确认消费完成
            try {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

}

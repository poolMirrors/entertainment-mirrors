package com.mirrors.mq;

import com.mirrors.entity.MultiDelayMessage;
import com.mirrors.entity.VoucherOrder;
import com.mirrors.utils.MQConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 消息发送器
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/9 21:17
 */
@Slf4j
@Component
public class MqSender {

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * 发送延时消息给MQ，死信交换机
     *
     * @param delayMessage
     * @param <T>
     */
    public <T> void sendDelayOrderMessage(MultiDelayMessage<T> delayMessage) {
        log.info("发送延迟消息：" + delayMessage);
        Long delay = delayMessage.removeNextDelay();
        rabbitTemplate.convertAndSend(MQConstants.DELAY_ORDER_EXCHANGE, MQConstants.DELAY_ORDER_ROUTING_KEY, delayMessage, message -> {
            message.getMessageProperties().setDelay(delay.intValue());
            return message;
        });
    }

    /**
     * 发送秒杀订单信息，需保证可靠传递性，失败重传；消息发送到队列失败，消息回退
     *
     * @param voucherOrder 秒杀订单信息
     * @param reliable     是否保证可靠传输模式
     */
    public void sendSeckillMessage(VoucherOrder voucherOrder, boolean reliable) {
        log.info("发送消息：" + voucherOrder);
        // 1.若要保证可靠传递
        if (reliable) {
            // 设置交换机处理消息的模式，交换器无法根据自身类型和路由键找到一个符合条件的队列时的处理方式。true：RabbitMQ会调用Basic.Return命令将消息返回给生产者
            rabbitTemplate.setMandatory(true);
            //（1）定义确认回调，当 publisher 将消息发送到 broker(exchange) 失败，则重新发送一次【broker包括exchange和queue】
            rabbitTemplate.setConfirmCallback(((correlationData, ack, cause) -> {
                // 如果发送失败就重新发送
                if (!ack) {
                    log.error("消息发送失败，错误原因：{}，再次发送。", cause);
                    rabbitTemplate.convertAndSend(MQConstants.SECKILL_EXCHANGE, MQConstants.SECKILL_ROUTING_KEY, voucherOrder);
                }
            }));
            //（2）设置退回函数，当 exchange 将消息发送到 queue 失败时，自动将消息退回给 publisher
            rabbitTemplate.setReturnsCallback((returnedMessage -> {
                // 如果消息未从 路由exchange 成功发送到队列，会走这个回调
                log.error("交换机发送消息到队列失败，错误原因：{}，执行将消息退回到 publisher 操作。", returnedMessage.getReplyText());
            }));
        }
        // 2.发送消息，默认消息持久化
        rabbitTemplate.convertAndSend(MQConstants.SECKILL_EXCHANGE, MQConstants.SECKILL_ROUTING_KEY, voucherOrder);
    }
}

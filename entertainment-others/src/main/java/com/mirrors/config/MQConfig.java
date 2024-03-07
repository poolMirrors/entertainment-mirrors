package com.mirrors.config;

import com.mirrors.utils.MQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息队列配置类：
 * <p>
 * 实际上，RabbitMQ 支持两种模式：
 *     <ol>
 *         <li>工作队列模式：即只有 Provider、Consumer 和 Queue，此时绑定的是默认的 Exchange </li>
 *         <li>
 *             Pub/Sub模式：即有 Provider、Consumer、Queue 和 Exchange，其中又分为：
 *             <ul>
 *                 <li>Fanout模式：将消息队列与交换机进行绑定（RoutingKey设为默认），Provider 将消息发送到 Exchange，然后 Exchange 会将消息发送到所有绑定的消息队列中。</li>
 *                 <li>Direct模式：将消息队列与交换机进行绑定并指定 RoutingKey，Provider 将消息发送到 Exchange，然后 Exchange 会将消息发送到所有指定 RoutingKey 的消息队列中。</li>
 *                 <li>Topic模式：将消息队列与交换机进行绑定并指定 RoutingKey（可以使用通配符），Provider 将消息发送到 Exchange，然后 Exchange 会将消息发送到所有符合 RoutingKey 规则的消息队列中。</li>
 *             </ul>
 *         </li>
 *     </ol>
 *     本质上都一样，主要角色都有 Provider、Consumer、Queue、Exchange、RoutingKey。
 * </p>
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/9 9:40
 */
@Configuration
public class MQConfig {

    /**
     * RabbitTemplate的多例化
     * <ol>spring中bean的scope属性，有如下5种类型：
     *     <li>singleton 表示在spring容器中的单例，通过spring容器获得该bean时总是返回唯一的实例</li>
     *     <li>prototype表示每次获得bean都会生成一个新的对象</li>
     *     <li>request表示在一次http请求内有效（只适用于web应用）</li>
     *     <li>session表示在一个用户会话内有效（只适用于web应用）</li>
     *     <li>globalSession表示在全局会话内有效（只适用于web应用）</li></li>
     * </ol>
     * 开启confirm和return要进行多例化？
     *
     * @param connectionFactory
     * @return
     */
    @Bean
    //@Scope("prototype")
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        template.setMessageConverter(messageConverter());
        return template;
    }

    /**
     * 消息转换器
     *
     * @return
     */
    @Bean
    public MessageConverter messageConverter() {
        // 配置自动创建消息id，用于识别不同消息，也可以在业务中基于ID判断是否是重复消息
        // jackson2JsonMessageConverter.setCreateMessageIds(true);

        return new Jackson2JsonMessageConverter();
    }

    //----------------------------------抢购业务相关-----------------------------------------------------

    /**
     * 创建持久化的队列
     *
     * @return
     */
    @Bean
    public Queue seckillQueue() {
        return QueueBuilder.durable(MQConstants.SECKILL_QUEUE).build();
    }

    /**
     * 交换机
     *
     * @return
     */
    @Bean
    public Exchange seckillExchange() {
        return ExchangeBuilder.directExchange(MQConstants.SECKILL_EXCHANGE).durable(true).build();
    }

    /**
     * 绑定通道
     *
     * @param seckillQueue
     * @param seckillExchange
     * @return
     */
    @Bean
    public Binding bindingSeckill(Queue seckillQueue, Exchange seckillExchange) {
        return BindingBuilder
                .bind(seckillQueue)
                .to(seckillExchange)
                .with(MQConstants.SECKILL_ROUTING_KEY).noargs();
    }

    //----------------------------------超时订单业务相关-----------------------------------------------------

    /**
     * 没有消费者的队列
     *
     * @return
     */
    @Bean
    public Queue delayOrderQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MQConstants.DELAY_ORDER_DL_EXCHANGE);
        args.put("x-dead-letter-routing-key", MQConstants.DELAY_ORDER_ROUTING_KEY);
        args.put("x-message-ttl", 5000);
        return QueueBuilder.durable(MQConstants.DELAY_ORDER_QUEUE)
                .withArguments(args)
                .build();
    }

    /**
     * 死信队列
     *
     * @return
     */
    @Bean
    public Queue delayOrderDLQueue() {
        return QueueBuilder.durable(MQConstants.DELAY_ORDER_DL_QUEUE).build();
    }

    /**
     * 交换机
     *
     * @return
     */
    @Bean
    public Exchange delayOrderExchange() {
        return ExchangeBuilder.directExchange(MQConstants.DELAY_ORDER_EXCHANGE).durable(true).build();
    }

    /**
     * 死信交换机
     *
     * @return
     */
    @Bean
    public Exchange delayOrderDLExchange() {
        return ExchangeBuilder.directExchange(MQConstants.DELAY_ORDER_DL_EXCHANGE).durable(true).build();
    }

    /**
     * 绑定：delayOrderExchange() -> delayOrderQueue() -> delayOrderDLExchange() -> delayOrderDLQueue()
     *
     * @return
     */
    @Bean
    public Binding bindingDelayOrder1() {
        return BindingBuilder
                .bind(delayOrderQueue())
                .to(delayOrderExchange())
                .with(MQConstants.DELAY_ORDER_ROUTING_KEY).noargs();
    }

    /**
     * 绑定：delayOrderExchange() -> delayOrderQueue() -> delayOrderDLExchange() -> delayOrderDLQueue()
     *
     * @return
     */
    @Bean
    public Binding bindingDelayOrder2() {
        return BindingBuilder
                .bind(delayOrderDLQueue())
                .to(delayOrderDLExchange())
                .with(MQConstants.DELAY_ORDER_ROUTING_KEY).noargs();
    }
}

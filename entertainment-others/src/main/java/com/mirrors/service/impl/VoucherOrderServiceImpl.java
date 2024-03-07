package com.mirrors.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.mirrors.dto.Result;
import com.mirrors.dto.UserDTO;
import com.mirrors.entity.MultiDelayMessage;
import com.mirrors.entity.SeckillVoucher;
import com.mirrors.entity.VoucherOrder;
import com.mirrors.mapper.VoucherOrderMapper;
import com.mirrors.mq.MqSender;
import com.mirrors.repertory.ISeckillVoucherRepService;
import com.mirrors.service.ISeckillVoucherService;
import com.mirrors.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mirrors.utils.RedisIDCreator;
import com.mirrors.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 优惠券订单 服务实现类
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    /**
     * 远程调用
     */
    @DubboReference
    private ISeckillVoucherRepService seckillVoucherRepService;

    @Resource
    private RedisIDCreator redisIDCreator;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private MqSender mqSender;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Lua脚本，静态代码块加载
     */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckillMQ.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /**
     * 单线程池，【避免频繁创建线程开销】
     */
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    /**
     * 代理对象，声明为一个成员变量，在异步线程中也可以获取到
     */
    private IVoucherOrderService proxy;


    // -----------------------------------------------rabbitMq 消息队列-----------------------------------------------


    /**
     * 使用RabbitMQ作为消息队列【本方法不涉及 数据库操作！】
     *
     * @param voucherId
     * @return
     */
    @Override
    public Result seckillVoucherRabbitMQ(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        long orderId = redisIDCreator.nextId("order"); // 随机生成订单id

        // 执行lua脚本
        int result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString(),
                String.valueOf(orderId)
        ).intValue();

        // 判断结果是否为0
        if (result != 0) {
            return Result.fail(result == 1 ? "库存不足" : "不允许重复购买");
        }

        // 为0，有购买资格，创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);

        // 发送到消息队列【同时削峰】
        mqSender.sendSeckillMessage(voucherOrder, false);

        // 返回订单id
        return Result.ok(orderId);
    }

    /**
     * 基于RabbitMQ的创建订单【本方法设计 数据库；由消息接收方执行】
     * <p>
     *     TODO 分布式事务，本地消息表+MQ+任务调度
     * <P>
     * <a href="https://javabetter.cn/sidebar/sanfene/fenbushi.html#%E5%88%86%E5%B8%83%E5%BC%8F%E7%90%86%E8%AE%BA">分布式事务解决方法</a>
     *
     * @param voucherOrder
     */
    @Transactional
    @Override
    public void createVoucherOrderRabbitMQ(VoucherOrder voucherOrder) {
        //（1）-----------加锁-------------

        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();
        // 创建分布式锁
        RLock lock = redissonClient.getLock("order:" + userId);
        boolean isLock = lock.tryLock();
        if (!isLock) {
            // 获取锁失败，直接返回失败
            log.error("不允许重复下单！");
            return;
        }
        // 获取锁成功
        try {
            // 查询订单
            int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
            if (count > 0) {
                log.error("该用户已经购买过一次！");
                return;
            }
            // 扣减库存
            //boolean success = seckillVoucherService.update()
            //        .setSql("stock = stock - 1")
            //        .eq("voucher_id", voucherId)
            //        .gt("stock", 0) //【乐观锁】只要库存大于0就可以秒杀成功（超卖问题），优化需要比较version
            //        .update();

            //（1）保存订单
            save(voucherOrder);
            //（2）TODO 保存消息表

            //（3）RPC调用库存服务，扣减库存
            boolean success = seckillVoucherRepService.reduceSeckillVoucherRep(voucherId);

            // 同时发送延时消息给MQ，死信交换机
            mqSender.sendDelayOrderMessage(
                    MultiDelayMessage.builder()
                            .data(voucherOrder.getId())
                            .delayMillis(CollUtil.newArrayList(10000L, 10000L, 10000L))
                            .build()
            );

        } finally {
            // 释放锁
            lock.unlock();
        }


        //（2）---------不加锁-------------

        //Long userId = voucherOrder.getUserId();
        //Long voucherId = voucherOrder.getVoucherId();
        //// 查询数据库，是否一人一单（redis判断过了，MySQL还要判断？）
        //int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        //if (count > 0) {
        //    log.error("不能重复购买");
        //    return;
        //}
        //
        //// 扣减库存
        //boolean success = seckillVoucherService.update()
        //        .setSql("stock = stock - 1")
        //        .eq("voucher_id", voucherOrder.getVoucherId())
        //        .gt("stock", 0) //【乐观锁】库存大于0（超卖问题）
        //        .update();
        //if (!success) {
        //    log.error("不能重复购买");
        //    return;
        //}
        //
        //// 创建订单，写入数据库
        //save(voucherOrder);
        //// 同时发送延时消息给MQ，死信交换机
        //mqSender.sendDelayOrderMessage(
        //        MultiDelayMessage.builder()
        //                .data(voucherOrder.getId())
        //                .delayMillis(CollUtil.newArrayList(10000L, 10000L, 10000L))
        //                .build()
        //);
    }


    // ----------------------------------------基于redis的 stream 消息队列--------------------------------------------


    /**
     * 基于redis的Stream，消息队列
     */
    //@PostConstruct
    @Deprecated
    public void initStream() {
        SECKILL_ORDER_EXECUTOR.submit(() -> {
            String queueName = "stream.orders";
            while (true) {
                try {
                    // 从消息队列获取订单，这里每次只读 1 个消息
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)), //  读1个，阻塞2秒
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );

                    // 判断消息获取是否成功
                    if (list == null || list.isEmpty()) {
                        continue; // 不成功，continue，再次循环读取
                    }

                    // 解析消息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> map = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(map, new VoucherOrder(), false);

                    // 下单到数据库
                    createVoucherOrderStream(voucherOrder);

                    // ack确认
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());

                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    // 有异常，进入pending list
                    while (true) {
                        try {
                            // 从pending list获取订单，这里每次只读 1 个消息
                            List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                                    Consumer.from("g1", "c1"),
                                    StreamReadOptions.empty().count(1),
                                    StreamOffset.create(queueName, ReadOffset.from("0"))
                            );

                            // 没有异常消息，结束
                            if (list == null || list.isEmpty()) {
                                break;
                            }

                            // 解析消息
                            MapRecord<String, Object, Object> record = list.get(0);
                            Map<Object, Object> map = record.getValue();
                            VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(map, new VoucherOrder(), false);

                            // 下单到数据库
                            createVoucherOrderStream(voucherOrder);

                            // ack确认
                            stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());

                        } catch (Exception e2) {
                            log.error("处理pending-list异常", e2);
                        }
                    }
                }
            }
        });
    }


    /**
     * 基于redis的Stream，消息队列
     *
     * @param voucherId
     * @return
     */
    @Override
    @Deprecated
    public Result seckillVoucherStream(Long voucherId) {
        // 获取用户id，订单id
        UserDTO user = UserHolder.getUser();

        Long userId = user.getId();
        long orderId = redisIDCreator.nextId("order");

        // 执行Lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString(),
                String.valueOf(orderId)
        );

        // 结果为0，才有下单资格
        int res = result.intValue();
        if (res != 0) {
            return Result.fail(res == 1 ? "库存不足" : "不允许重复购买");
        }

        // 保存事务的代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();

        // 返回订单id
        return Result.ok(orderId);
    }

    /**
     * 基于Stream消息队列，下单写入数据库
     *
     * @param voucherOrder
     */
    @Deprecated
    @Transactional
    @Override
    public void createVoucherOrderStream(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();
        // 查询数据库，是否一人一单【redis判断过了，MySQL还要判断？】
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            log.error("不能重复购买");
            return;
        }

        // 扣减库存【redis判断过了，MySQL还要判断？】
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .gt("stock", 0) //【乐观锁】库存大于0
                .update();
        if (!success) {
            log.error("不能重复购买");
            return;
        }

        // 创建订单，写入数据库
        save(voucherOrder);
    }

    // -------------------------------------------异步，阻塞队列-------------------------------------------------

    /**
     * 阻塞队列
     */
    private BlockingQueue<VoucherOrder> orderBlockingQueue = new ArrayBlockingQueue<>(1024 * 1024);

    /**
     * 【异步下单】在该类执行完构造器后执行，启动时加载；读取阻塞队列
     */
    @Deprecated
    //@PostConstruct
    public void initAsync() {
        // 也可以使用lambda表达式
        SECKILL_ORDER_EXECUTOR.submit(() -> {
            while (true) {
                try {
                    // 从阻塞队列获取订单
                    VoucherOrder order = orderBlockingQueue.take();
                    Long userId = order.getUserId();

                    RLock lock = redissonClient.getLock("lock:order:" + userId);
                    // redisson无参锁；获取失败直接返回且锁不会超时【这里单线程有必要加锁吗？而且redis也判断了】
                    boolean isLock = lock.tryLock();
                    if (!isLock) {
                        log.error("不允许重复购买");
                        return;
                    }

                    // 获取锁成功
                    try {
                        proxy.createVoucherOrderAsync(order); // 事务，代理对象
                    } finally {
                        // 释放
                        lock.unlock();
                    }

                } catch (InterruptedException e) {
                    log.error("处理订单异常", e);
                }
            }
        });
    }


    /**
     * 异步下单，阻塞队列，修改数据库
     *
     * @param voucherId
     * @return
     */
    @Override
    @Deprecated
    public Result seckillVoucherAsync(Long voucherId) {
        // 获取用户id
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();

        // 执行Lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString()
        );

        // 结果为0，才有下单资格
        int res = result.intValue();
        if (res != 0) {
            return Result.fail(res == 1 ? "库存不足" : "不允许重复购买");
        }

        // 获取orderId，创建订单
        long orderId = redisIDCreator.nextId("order");

        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);

        // 事务的代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();

        //【异步】放入阻塞队列
        orderBlockingQueue.add(voucherOrder);

        // 返回订单id
        return Result.ok(orderId);
    }

    /**
     * 异步下单，阻塞队列，创建订单写入数据库
     *
     * @param voucherOrder
     */
    @Deprecated
    @Transactional
    @Override
    public void createVoucherOrderAsync(VoucherOrder voucherOrder) {
        // 实现一人一单（redis判断过了，MySQL还要判断？）
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();

        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            log.error("不能重复购买");
            return;
        }

        // 扣减库存（redis判断过了，MySQL还要判断？）
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .gt("stock", 0) //【乐观锁】库存大于0
                .update();
        if (!success) {
            log.error("不能重复购买");
            return;
        }

        // 创建订单，写入数据库
        save(voucherOrder);
    }


    // -------------------------------------------同步-------------------------------------------------

    /**
     * 同步下单，抢购
     *
     * @param voucherId
     * @return
     */
    @Deprecated
    @Override
    public Result seckillVoucherSync(Long voucherId) {
        // 查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);

        // 判断秒杀是否开始和结束
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀未开始");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已结束");
        }

        // 判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足");
        }

        // 同一个用户需要锁控制；不同用户可以争夺
        Long userId = UserHolder.getUser().getId();

        //（1）--------下面是单体项目的锁方式

        ////【锁住每一个用户】利用intern去常量池找userId，不同用户不会被锁定（单体锁）
        //synchronized (userId.toString().intern()) {
        //    // return this.createVoucherOrder(voucherId, userId); // 事务失效，因为seckillVoucher方法没加事务，使用的是this对象，不是spring的代理对象
        //
        //    //【获取代理对象，启动事务；确保 @Transactional事务 执行后，再释放锁】
        //    IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
        //    return proxy.createVoucherOrderSync(voucherId, userId);
        //}


        //（2）--------下面是redisson的锁方式

        // 分布式锁 SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order:" + userId);

        // redisson 无参锁；获取失败直接返回且锁不会超时
        boolean isLock = lock.tryLock();
        if (!isLock) {
            // 避免一个用户反复抢，这里采用返回错误（返回错误 或 重试，看业务）
            return Result.fail("不允许重复抢购");
        }

        // 获取锁成功
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrderSync(voucherId, userId);
        } finally {
            // 释放
            lock.unlock();
        }
    }


    /**
     * 同步下单，创建订单，写入数据库
     *
     * @param voucherId
     * @param userId
     * @return
     */
    @Override
    @Deprecated
    @Transactional
    public Result createVoucherOrderSync(Long voucherId, Long userId) {
        // 实现一人一单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            return Result.fail("不允许重复下单");
        }

        // 扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0) //【乐观锁】库存大于0
                .update();
        if (!success) {
            return Result.fail("库存不足");
        }

        // 创建订单，写入数据库
        long orderId = redisIDCreator.nextId("order");

        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);

        save(voucherOrder);

        // 返回订单id
        return Result.ok(orderId);
    }
}

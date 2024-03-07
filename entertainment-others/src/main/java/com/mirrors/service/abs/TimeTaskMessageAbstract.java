package com.mirrors.service.abs;

import com.mirrors.entity.TimeTaskMessage;
import com.mirrors.service.ITimeTaskMessageService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务抽象类
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/18 11:02
 */
@Slf4j
@Data
@Component
public abstract class TimeTaskMessageAbstract {

    @Autowired
    private ITimeTaskMessageService timeTaskMessageService;

    /**
     * 任务处理流程，由 实现类 完成
     *
     * @param timeTaskMessage
     * @return
     */
    public abstract boolean execute(TimeTaskMessage timeTaskMessage);

    /**
     * 扫描消息表多线程执行任务
     *
     * @param shardIndex
     * @param shardTotal
     * @param messageType
     * @param count
     * @param timeout
     */
    public void process(int shardIndex, int shardTotal, String messageType, int count, long timeout) {
        try {
            // 扫描消息表获取任务清单
            List<TimeTaskMessage> messageList = timeTaskMessageService.getMessageList(shardIndex, shardTotal, messageType, count);
            // 任务个数
            int size = messageList.size();
            log.info("取出待处理消息" + size + "条");
            if (size <= 0) {
                return;
            }
            // 创建线程池
            ExecutorService threadPool = Executors.newFixedThreadPool(size);
            // 计数器
            CountDownLatch countDownLatch = new CountDownLatch(size);
            // 遍历消息
            for (TimeTaskMessage message : messageList) {
                threadPool.execute(() -> {
                    log.info("开始任务:{}", message);
                    // 处理任务
                    try {
                        boolean execute = execute(message);
                        if (execute) {
                            log.info("任务执行成功:{})", message);
                            int completed = timeTaskMessageService.completed(message.getId());
                            if (completed > 0) {
                                log.debug("任务删除成功:{}", message);
                            } else {
                                log.debug("任务删除失败:{}", message);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.debug("任务出现异常:{},任务:{}", e.getMessage(), message);
                    }
                    // 计数
                    countDownLatch.countDown();
                    log.debug("结束任务:{}", message);
                });
            }
            // 等待所有任务线程完成，给一个充裕的超时时间，防止无限等待，到达超时时间还没有处理完成则结束任务
            countDownLatch.await(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

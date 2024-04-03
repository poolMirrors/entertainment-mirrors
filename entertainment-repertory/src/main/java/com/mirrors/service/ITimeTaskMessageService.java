package com.mirrors.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mirrors.pojo.TimeTaskMessage;

import java.util.List;

/**
 * 任务调度，消息表服务
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/18 10:48
 */
public interface ITimeTaskMessageService extends IService<TimeTaskMessage> {

    /**
     * 扫描消息表记录（分片广播）
     *
     * @param shardIndex
     * @param shardTotal
     * @param messageType
     * @param count
     * @return
     */
    List<TimeTaskMessage> getMessageList(int shardIndex, int shardTotal, String messageType, int count);

    /**
     * 添加消息到数据库
     *
     * @param messageType
     * @param businessKey1
     * @param businessKey2
     * @param businessKey3
     * @return
     */
    TimeTaskMessage addMessage(String messageType, String businessKey1, String businessKey2, String businessKey3);

    /**
     * 完成任务
     *
     * @param id
     * @return
     */
    int completed(long id);

    /**
     * 完成 阶段一 任务
     *
     * @param id
     * @return
     */
    int completedStageOne(long id);

    /**
     * 完成 阶段二 任务
     *
     * @param id
     * @return
     */
    int completedStageTwo(long id);

    /**
     * 完成 阶段三 任务
     *
     * @param id
     * @return
     */
    int completedStageThree(long id);

    /**
     * 完成 阶段四 任务
     *
     * @param id
     * @return
     */
    int completedStageFour(long id);

    /**
     * 查询 阶段一 任务状态
     *
     * @param id
     * @return
     */
    int getStageOne(long id);

    /**
     * 查询 阶段二 任务状态
     *
     * @param id
     * @return
     */
    int getStageTwo(long id);

    /**
     * 查询 阶段三 任务状态
     *
     * @param id
     * @return
     */
    int getStageThree(long id);

    /**
     * 查询 阶段四 状态
     *
     * @param id
     * @return
     */
    int getStageFour(long id);
}

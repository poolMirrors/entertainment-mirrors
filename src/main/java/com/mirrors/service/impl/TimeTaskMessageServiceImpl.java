package com.mirrors.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mirrors.entity.TimeTaskMessage;
import com.mirrors.mapper.TimeTaskMessageMapper;
import com.mirrors.service.ITimeTaskMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

/**
 * 任务调度实现类
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/18 15:02
 */
@Slf4j
@Component
public class TimeTaskMessageServiceImpl extends ServiceImpl<TimeTaskMessageMapper, TimeTaskMessage> implements ITimeTaskMessageService {

    /**
     * 扫描消息表记录（分片广播）
     *
     * @param shardIndex
     * @param shardTotal
     * @param messageType
     * @param count
     * @return
     */
    @Override
    public List<TimeTaskMessage> getMessageList(int shardIndex, int shardTotal, String messageType, int count) {
        List<TimeTaskMessage> timeTaskMessages = baseMapper.selectListByShardIndex(shardTotal, shardIndex, messageType, count);
        System.out.println(timeTaskMessages);
        return timeTaskMessages;
    }

    /**
     * 添加消息到数据库
     *
     * @param messageType
     * @param businessKey1
     * @param businessKey2
     * @param businessKey3
     * @return
     */
    @Override
    public TimeTaskMessage addMessage(String messageType, String businessKey1, String businessKey2, String businessKey3) {
        TimeTaskMessage timeTaskMessage = new TimeTaskMessage();
        timeTaskMessage.setMessageType(messageType);
        timeTaskMessage.setBusinessKey1(businessKey1);
        timeTaskMessage.setBusinessKey2(businessKey2);
        timeTaskMessage.setBusinessKey3(businessKey3);
        // 保存到数据库
        int insert = baseMapper.insert(timeTaskMessage);
        if (insert > 0) {
            return timeTaskMessage;
        } else {
            return null;
        }
    }

    /**
     * 完成任务
     *
     * @param id
     * @return
     */
    @Override
    public int completed(long id) {
        TimeTaskMessage timeTaskMessage = new TimeTaskMessage();
        // 完成任务，设置状态
        timeTaskMessage.setState("1");
        // 更新到数据库
        int update = baseMapper.update(timeTaskMessage, new LambdaQueryWrapper<TimeTaskMessage>().eq(TimeTaskMessage::getId, id));
        if (update > 0) {
            TimeTaskMessage message = baseMapper.selectById(id);
            // TODO 完成任务后，将消息添加到历史消息表

            // 删除
            baseMapper.deleteById(id);
            return 1;
        }
        return 0;
    }

    /**
     * 完成 阶段一 任务
     *
     * @param id
     * @return
     */
    @Override
    public int completedStageOne(long id) {
        TimeTaskMessage timeTaskMessage = new TimeTaskMessage();
        timeTaskMessage.setStageState1("1");

        int update = baseMapper.update(timeTaskMessage, new LambdaQueryWrapper<TimeTaskMessage>().eq(TimeTaskMessage::getId, id));
        System.out.println(update);
        return update;
    }

    /**
     * 完成 阶段二 任务
     *
     * @param id
     * @return
     */
    @Override
    public int completedStageTwo(long id) {
        TimeTaskMessage timeTaskMessage = new TimeTaskMessage();
        timeTaskMessage.setStageState2("1");
        return baseMapper.update(timeTaskMessage, new LambdaQueryWrapper<TimeTaskMessage>().eq(TimeTaskMessage::getId, id));
    }

    /**
     * 完成 阶段三 任务
     *
     * @param id
     * @return
     */
    @Override
    public int completedStageThree(long id) {
        TimeTaskMessage timeTaskMessage = new TimeTaskMessage();
        timeTaskMessage.setStageState3("1");
        return baseMapper.update(timeTaskMessage, new LambdaQueryWrapper<TimeTaskMessage>().eq(TimeTaskMessage::getId, id));
    }

    /**
     * 完成 阶段四 任务
     *
     * @param id
     * @return
     */
    @Override
    public int completedStageFour(long id) {
        TimeTaskMessage timeTaskMessage = new TimeTaskMessage();
        timeTaskMessage.setStageState4("1");
        return baseMapper.update(timeTaskMessage, new LambdaQueryWrapper<TimeTaskMessage>().eq(TimeTaskMessage::getId, id));
    }

    /**
     * 查询 阶段一 任务状态
     *
     * @param id
     * @return
     */
    @Override
    public int getStageOne(long id) {
        return Integer.parseInt(baseMapper.selectById(id).getStageState1());
    }

    /**
     * 查询 阶段二 任务状态
     *
     * @param id
     * @return
     */
    @Override
    public int getStageTwo(long id) {
        return Integer.parseInt(baseMapper.selectById(id).getStageState2());
    }

    /**
     * 查询 阶段三 任务状态
     *
     * @param id
     * @return
     */
    @Override
    public int getStageThree(long id) {
        return Integer.parseInt(baseMapper.selectById(id).getStageState3());
    }

    /**
     * 查询 阶段四 任务状态
     *
     * @param id
     * @return
     */
    @Override
    public int getStageFour(long id) {
        return Integer.parseInt(baseMapper.selectById(id).getStageState4());
    }
}

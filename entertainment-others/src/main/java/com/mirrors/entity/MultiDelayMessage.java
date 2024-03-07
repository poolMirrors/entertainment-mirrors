package com.mirrors.entity;

import cn.hutool.core.collection.CollUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 延时消息，处理超时订单
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/23 22:03
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MultiDelayMessage<T> {

    /**
     * 消息体
     */
    private T data;

    /**
     * 记录延迟时间的集合
     */
    private List<Long> delayMillis;

    /**
     * 获取并移除下一个延迟时间
     *
     * @return
     */
    public Long removeNextDelay() {
        return delayMillis.remove(0);
    }

    /**
     * 是否还有下一个延迟时间
     *
     * @return
     */
    public boolean hasNextDelay() {
        return !delayMillis.isEmpty();
    }

}

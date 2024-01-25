package com.mirrors.jobhandler;

import com.alibaba.fastjson.JSON;
import com.mirrors.entity.Blog;
import com.mirrors.entity.TimeTaskMessage;
import com.mirrors.mapper.BlogMapper;
import com.mirrors.service.ITimeTaskMessageService;
import com.mirrors.service.abs.TimeTaskMessageAbstract;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 分布式事务，将blog保证数据库与Elasticsearch数据一致性
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/18 10:38
 */
@Slf4j
@Component
public class BlogConsistencyXxlJob extends TimeTaskMessageAbstract {

    /**
     * 发布博客 消息类型
     */
    public static final String MESSAGE_TYPE = "blog";

    @Resource
    private BlogMapper blogMapper;

    @Resource
    private RestHighLevelClient restHighLevelClient;

    /**
     * 任务调度入口
     */
    @XxlJob("consistency")
    public void BlogConsistency() {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.info("shardIndex = {}, shardTotal = {}.", shardIndex, shardTotal);
        // 参数：分片序号，分片总数，消息类型，一次最多取到的任务数，一次任务调度执行的超时时间(秒)
        process(shardIndex, shardTotal, MESSAGE_TYPE, 5, 60);
    }

    /**
     * 完成 任务处理的流程（这里采用一次处理一个消息）
     *
     * @param timeTaskMessage
     * @return
     */
    @Override
    public boolean execute(TimeTaskMessage timeTaskMessage) {
        // 获取相关的业务信息
        String businessKey1 = timeTaskMessage.getBusinessKey1();
        Long blogId = Long.parseLong(businessKey1);
        // 将博客缓存到 索引库es
        saveBlogIndex(timeTaskMessage, blogId);
        return true;
    }

    /**
     * 将博客缓存到 索引库es
     *
     * @param timeTaskMessage
     * @param blogId
     */
    private void saveBlogIndex(TimeTaskMessage timeTaskMessage, Long blogId) {
        log.info("将课程索引信息存入es，blogId：{}", blogId);
        // 消息id
        Long id = timeTaskMessage.getId();
        // 拿到服务
        ITimeTaskMessageService timeTaskMessageService = getTimeTaskMessageService();
        // 保证消息幂等性（这里采用数据库增加一个字段实现判断）
        int stageOne = timeTaskMessageService.getStageOne(id);
        if (stageOne == 1) {
            log.warn("blog索引已处理，直接返回，blogId：{}", blogId);
            return;
        }
        // 保存blog索引到es
        try {
            // 从数据库查出blog
            Blog blog = blogMapper.selectById(blogId);
            String json = JSON.toJSONString(blog);
            // 发送请求给es
            IndexRequest indexRequest = new IndexRequest("blog").id(blog.getId().toString());
            indexRequest.source(json, XContentType.JSON);
            restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

package com.mirrors.service;

import com.mirrors.dto.Result;
import com.mirrors.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 博客相关服务
 */
public interface IBlogService extends IService<Blog> {

    /**
     * 热门博客
     *
     * @param current
     * @return
     */
    Result queryHotBlog(Integer current);

    /**
     * 点击blog详情页；根据id查blog
     *
     * @param id
     * @return
     */
    Result queryById(Long id);

    /**
     * 点赞博客
     *
     * @param id
     * @return
     */
    Result likeBlog(Long id);

    /**
     * 查询 博客点赞top5 的用户信息
     *
     * @param id
     * @return
     */
    Result queryBlogLikes(Long id);

    /**
     * 保存博客：
     * <p>
     * （1）Feed流图推送
     * <p>
     * （2）存入信息表，分布式事务，同步到Elasticsearch
     *
     * @param blog
     * @return
     */
    Result saveBlog(Blog blog);

    /**
     * Feed流推模式下，信息的滚动分页
     *
     * @param max
     * @param offset 注意第一次查询offset为0，设置默认值，避免空指针
     * @return
     */
    Result queryBlogOfFollow(Long max, Integer offset);
}

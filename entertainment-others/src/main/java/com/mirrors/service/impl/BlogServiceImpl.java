package com.mirrors.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mirrors.dto.Result;
import com.mirrors.dto.ScrollResult;
import com.mirrors.dto.UserDTO;
import com.mirrors.entity.Blog;
import com.mirrors.entity.Follow;
import com.mirrors.entity.TimeTaskMessage;
import com.mirrors.entity.User;
import com.mirrors.exception.BusinessException;
import com.mirrors.mapper.BlogMapper;
import com.mirrors.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mirrors.service.IFollowService;
import com.mirrors.service.ITimeTaskMessageService;
import com.mirrors.service.IUserService;
import com.mirrors.utils.RedisConstants;
import com.mirrors.utils.SystemConstants;
import com.mirrors.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 博客 服务实现类
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IFollowService followService;

    @Autowired
    private ITimeTaskMessageService timeTaskMessageService;

    /**
     * blog是否点赞
     *
     * @param blog
     */
    private void isBlogLiked(Blog blog) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            //用户未登录，无需查询点赞
            return;
        }
        String key = RedisConstants.BLOG_LIKED_KEY + blog.getId();
        // 获取有序集合中指定成员的分数
        Double score = stringRedisTemplate.opsForZSet().score(key, user.getId().toString());
        blog.setIsLike(score != null);
    }

    /**
     * 热门博客
     *
     * @param current
     * @return
     */
    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询，获取当前页数据
        Page<Blog> page = query().orderByDesc("liked").page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Blog> records = page.getRecords();

        // 查询用户
        records.forEach(blog -> {
            Long userId = blog.getUserId();
            // 每一篇blog要显示对于 发布blog的用户 的信息
            User user = userService.getById(userId);
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
            // blog是否点赞
            isBlogLiked(blog);
        });

        return Result.ok(records);
    }

    /**
     * 点击blog详情页；根据id查blog
     *
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        // 查询blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在");
        }
        // 查询用户
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
        // 查询blog是否点赞
        isBlogLiked(blog);

        return Result.ok(blog);
    }

    /**
     * 点赞博客
     *
     * @param id
     * @return
     */
    @Override
    public Result likeBlog(Long id) {
        // 获取当前用户
        Long userId = UserHolder.getUser().getId();

        // 判断当前用户是否点赞
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString()); // 点赞排行榜，先点赞先展示

        if (score == null) {
            // 还没有点赞，可以点赞，数据库加1
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            // 保存用户id到redis的Zset集合，【当前时间戳当做score】
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            // 已经点赞，数据库减1
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            // 删除redis的set集合中用户id
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }

        return Result.ok();
    }

    /**
     * 查询 博客点赞top5 的用户信息
     *
     * @param id
     * @return
     */
    @Override
    public Result queryBlogLikes(Long id) {
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        // 查询top5的点赞用户
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5 == null || top5.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        // 解析用户id
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",", ids);

        // 根据用户id查询用户
        List<UserDTO> users = userService.query()
                .in("id", ids)
                .last("ORDER BY FIELD(id," + idStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        // 返回
        return Result.ok(users);
    }

    /**
     * 将blog保存到 消息表 和 blog表
     *
     * @param blog
     * @return
     */
    @Override
    @Transactional
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 【本地事务】保存探店博文到数据库
        boolean isSuccess = save(blog);
        if (!isSuccess) {
            return Result.ok("发布动态失败");
        }
        // 【本地事务】写入本地消息表，以后由任务调度，写入Elasticsearch
        TimeTaskMessage message = timeTaskMessageService.addMessage("blog", String.valueOf(blog.getId()), null, null);
        if (message == null) {
            throw new BusinessException("添加消息记录失败");
        }

        // 查询所有粉丝，推送笔记id
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        for (Follow follow : follows) {
            // 获取粉丝id，推送
            Long userId = follow.getUserId();
            // 推送到redis的SortedSet，score为时间戳
            String key = "feed:" + userId;
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
        }

        // 返回id
        return Result.ok(blog.getId());
    }

    /**
     * Feed流推模式下，信息的滚动分页
     *
     * @param max
     * @param offset 注意第一次查询offset为0，设置默认值，避免空指针
     * @return
     */
    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 获取当前用户
        Long userId = UserHolder.getUser().getId();

        // 查询收件箱【滚动分页查询】
        String key = "feed:" + userId;
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate
                .opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2); // 4个关键参数

        if (tuples == null || tuples.isEmpty()) {
            return Result.ok();
        }

        // 解析数据
        List<Long> ids = new ArrayList<>(tuples.size()); // 指定大小，避免扩容影响性能
        long minTime = 0;
        int minCount = 1; // 最小元素个数，也就是offset

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            // blogId
            String blogId = tuple.getValue();
            ids.add(Long.valueOf(blogId));
            // minTime时间戳，最后一个元素最小（已排序）
            long time = tuple.getScore().longValue();
            if (time == minTime) {
                // offset最小元素的个数
                minCount++;
            } else {
                minTime = time;
                minCount = 0;
            }
        }

        // 根据id查询blog
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogList = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        // 查询blog点赞信息
        blogList.forEach(blog -> {
            Long id = blog.getUserId();
            User user = userService.getById(id);
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
            // 查询blog是否点赞
            isBlogLiked(blog);
        });

        // 返回
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogList);
        scrollResult.setOffset(minCount);
        scrollResult.setMinTime(minTime);

        return Result.ok(scrollResult);
    }
}

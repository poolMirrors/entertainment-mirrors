package com.mirrors.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mirrors.dto.Result;
import com.mirrors.dto.UserDTO;
import com.mirrors.entity.*;
import com.mirrors.service.*;
import com.mirrors.utils.SystemConstants;
import com.mirrors.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.*;

/**
 * 博客管理
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;

    @Resource
    private IShopService shopService;

    @Resource
    private IUserInfoService userInfoService;

    @Resource
    private IVoucherService voucherService;

    // --------------------------------------主要实现-------------------------------------------------------

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
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        Result result = blogService.saveBlog(blog);
        System.out.println(result);
        return result;
    }

    /**
     * Feed流推模式下，信息的滚动分页；
     * 用户博客动态功能（关注的UP更新博客时，进行推送）
     *
     * @param max
     * @param offset 注意第一次查询offset为0，设置默认值，避免空指针
     * @return
     */
    @GetMapping("/of/follow")
    public Result queryBlogFollow(@RequestParam("lastId") Long max, @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        Result result = blogService.queryBlogOfFollow(max, offset);
        System.out.println(result);
        return result;
    }

    /**
     * 异步编排，查询博客，返回blog涉及的信息（用户信息、博客信息，店铺信息，优惠券信息）；优化串行；
     * 根据blogId查询blog，得到userId和shopId -> 再查用户信息、商铺信息、优惠券信息 ->返回；
     * TODO 完善返回方式
     *
     * @param blogId
     * @return
     */
    @GetMapping("/async/{blogId}")
    public Result getDetailByCF(@PathVariable("blogId") Long blogId) {
        // 线程池
        ExecutorService threadPool = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(100));
        Object[] ret = new Object[4];

        // 1.查询blog
        CompletableFuture<long[]> cf1 = CompletableFuture.supplyAsync(() -> {
            Blog blog = blogService.getById(blogId);
            ret[0] = blog;
            // {用户id，店铺id}
            return new long[]{blog.getUserId(), blog.getShopId()};
        }, threadPool);

        // 1.1.并行查用户详细信息
        CompletableFuture<Void> cf2 = cf1.thenAcceptAsync((arr) -> {
            long userId = arr[0];
            UserInfo userInfo = userInfoService.getById(userId);
            ret[1] = userInfo;
        }, threadPool);

        // 1.2.并行查优惠券
        CompletableFuture<Void> cf3 = cf1.thenAcceptAsync((arr) -> {
            long shopId = arr[1];
            List<Voucher> vouchers = voucherService.query().eq("shop_id", shopId).list();
            ret[2] = vouchers;
        }, threadPool);

        // 1.3.并行查店铺
        CompletableFuture<Void> cf4 = cf1.thenAcceptAsync((arr) -> {
            long shopId = arr[1];
            Shop shop = shopService.getById(shopId);
            ret[3] = shop;
        }, threadPool);

        // 2.等待执行完
        CompletableFuture<Void> tmp = CompletableFuture.allOf(cf2, cf3, cf4);
        tmp.join();

        return Result.ok(ret);
    }

    // ------------------------------------舍弃功能------------------------------------------------------------------

    /**
     * 博客主页面，分页
     *
     * @param current
     * @return
     */
    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId())
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }


    /**
     * 点赞博客
     *
     * @param id
     * @return
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        Result result = blogService.likeBlog(id);
        System.out.println(result);
        return result;
    }

    /**
     * 热门博客
     *
     * @param current
     * @return
     */
    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        Result result = blogService.queryHotBlog(current);
        System.out.println(result);
        return result;
    }

    /**
     * 点击blog详情页；根据id查blog
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result queryById(@PathVariable("id") Long id) {
        Result result = blogService.queryById(id);
        System.out.println(result);
        return result;
    }

    /**
     * 查询 博客点赞top5 的用户信息
     *
     * @param id
     * @return
     */
    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long id) {
        Result result = blogService.queryBlogLikes(id);
        System.out.println(result);
        return result;
    }


    /**
     * 根据用户id查询blog
     *
     * @param current
     * @param id
     * @return
     */
    @GetMapping("/of/user")
    public Result queryBlogByUserId(@RequestParam(value = "current", defaultValue = "1") Integer current, @RequestParam("id") Long id) {
        // 根据用户查询
        Page<Blog> page = blogService
                .query()
                .eq("user_id", id)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

}

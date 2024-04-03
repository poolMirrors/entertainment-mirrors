package com.mirrors.controller;


import com.mirrors.dto.Result;
import com.mirrors.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 关注管理
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    // ------------------------------------------舍弃功能---------------------------------------------

    /**
     * 关注和取关
     *
     * @param followUserId 被关注的id
     * @param isFollow     true或false
     * @return
     */
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollow) {
        Result result = followService.follow(followUserId, isFollow);
        System.out.println(result);
        return result;
    }

    /**
     * 判断是否关注
     *
     * @param followUserId
     * @return
     */
    @GetMapping("/or/not/{id}")
    public Result followOrNot(@PathVariable("id") Long followUserId) {
        Result result = followService.followOrNot(followUserId);
        System.out.println(result);
        return result;
    }

    /**
     * 求 传入id用户 和 当前登录用户 的共同关注列表
     *
     * @param id
     * @return
     */
    @GetMapping("/common/{id}")
    public Result followCommon(@PathVariable("id") Long id) {
        Result result = followService.followCommon(id);
        System.out.println(result);
        return result;
    }
}

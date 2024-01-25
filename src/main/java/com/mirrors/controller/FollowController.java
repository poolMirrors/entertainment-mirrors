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

    /**
     * 关注和取关
     *
     * @param followUserId 被关注的id
     * @param isFollow     true或false
     * @return
     */
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollow) {
        return followService.follow(followUserId, isFollow);
    }

    /**
     * 判断是否关注
     *
     * @param followUserId
     * @return
     */
    @GetMapping("/or/not/{id}")
    public Result followOrNot(@PathVariable("id") Long followUserId) {
        return followService.followOrNot(followUserId);
    }

    /**
     * 求 传入id用户 和 当前登录用户 的共同关注列表
     *
     * @param id
     * @return
     */
    @GetMapping("/common/{id}")
    public Result followCommon(@PathVariable("id") Long id) {
        return followService.followCommon(id);
    }
}

package com.mirrors.service;

import com.mirrors.dto.Result;
import com.mirrors.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 关注相关服务
 */
public interface IFollowService extends IService<Follow> {

    /**
     * 关注和取关
     *
     * @param followUserId
     * @param isFollow
     * @return
     */
    Result follow(Long followUserId, Boolean isFollow);

    /**
     * 判断是否关注
     *
     * @param followUserId
     * @return
     */
    Result followOrNot(Long followUserId);

    /**
     * 求 传入id用户 和 当前登录用户 的共同关注列表
     *
     * @param id
     * @return
     */
    Result followCommon(Long id);
}

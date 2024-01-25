package com.mirrors.service;

import org.springframework.stereotype.Service;

/**
 * Token 服务类接口
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/9 10:37
 */
public interface ITokenService {

    /**
     * 下单前获取令牌用户防重提交
     *
     * @return
     */
    String getOrderToken();
}

package com.mirrors.service.impl;

import com.mirrors.service.ITokenService;
import com.mirrors.utils.CommonUtil;
import com.mirrors.utils.RedisConstants;
import com.mirrors.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * token 相关服务实现类
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/9 10:38
 */
@Service
public class ITokenServiceImpl implements ITokenService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 下单前获取令牌用户防重提交
     *
     * @return
     */
    @Override
    public String getOrderToken() {
        // 获取当前登录用户账号
        Long userId = UserHolder.getUser().getId();
        // 随机获取32位的 数字+字母 作为token
        String token = CommonUtil.getStringNumRandom(32);
        // key
        String key = String.format(RedisConstants.REPEAT_SUBMIT_ORDER_TOKEN_KEY, userId, token);

        // 设置 防重刷令牌 有效时间30分钟
        stringRedisTemplate.opsForValue().set(key, String.valueOf(Thread.currentThread().getId()), 30, TimeUnit.MINUTES);

        return token;
    }
}

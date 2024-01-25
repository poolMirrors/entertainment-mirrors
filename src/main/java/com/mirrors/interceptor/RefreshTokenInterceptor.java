package com.mirrors.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mirrors.dto.UserDTO;
import com.mirrors.utils.RedisConstants;
import com.mirrors.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 【重点】手动new出来的对象，注入要利用构造器，并不是spring的自动注入
 * RefreshTokenInterceptor拦截所有，只对有token的请求，进行refresh和保存在ThreadLocal中
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {

    /**
     * 自己手动创建的对象，里面的实例需要自己手动注入，
     * spring创建的对象，可以使用自动注入方法。
     */
    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 进入controller前
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 获取请求头的token信息
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            return true;
        }

        // token不为空，获取redis中用户，并判断
        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(tokenKey);
        if (map.isEmpty()) {
            return true;
        }

        // 用户存在，hash数据转为UserDTO，并保存ThreadLocal
        UserDTO userDTO = BeanUtil.fillBeanWithMap(map, new UserDTO(), false);
        UserHolder.saveUser(userDTO);

        //【重点】刷新token有效期
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL_MINUTES, TimeUnit.MINUTES);

        // 放行
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 删除当前线程的user
        UserHolder.removeUser();
    }
}

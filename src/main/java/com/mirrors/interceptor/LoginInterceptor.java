package com.mirrors.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mirrors.dto.UserDTO;
import com.mirrors.entity.User;
import com.mirrors.utils.RedisConstants;
import com.mirrors.utils.UserHolder;
import com.mirrors.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 【重点】手动new出来的对象，注入要利用构造器，并不是spring的自动注入；
 * 实现 HandlerInterceptor
 */
public class LoginInterceptor implements HandlerInterceptor {

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
        // 判断是否拦截，ThreadLocal有用户不用拦截
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 拦截，返回状态码
            response.setStatus(401);
            return false;
        }
        // 有用户则放行
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }

}

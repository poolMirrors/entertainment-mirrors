package com.mirrors.prevent;

import cn.hutool.core.util.StrUtil;
import com.mirrors.exception.BusinessException;
import com.mirrors.utils.IPUtil;
import com.mirrors.utils.RedisConstants;
import com.mirrors.utils.UserHolder;
import lombok.extern.apachecommons.CommonsLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 接口防刷 切面实现类
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/9 22:18
 */
@Slf4j
@Component
@Aspect // 声明当前类是一个切面
public class RestrictRequestAop {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 定义 防重提交 切入点
     */
    @Pointcut("@annotation(com.mirrors.prevent.RepeatSubmit)")
    public void repeatSubmitPointcut() {

    }

    /**
     * 定义 防刷 切入点
     */
    @Pointcut("@annotation(com.mirrors.prevent.RestrictRequest)")
    public void restrictRequestPointcut() {
    }

    /**
     * 目标方法执行前（防刷切入点）；
     * 限制 同一个ip和user 在规定时间内的最大访问次数【防刷】
     *
     * @param joinPoint
     * @throws NoSuchMethodException
     */
    @Before("restrictRequestPointcut()")
    public void joinPoint(JoinPoint joinPoint) throws NoSuchMethodException {
        // 使用redisson声明 ip锁 和 user锁
        RLock ipLock = null, userLock = null;
        boolean isIp = false, isUser = false;
        try {
            // 根据 方法签名 获得 被注解标注的具体实现方法
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = joinPoint.getTarget().getClass().getMethod(signature.getName(), signature.getParameterTypes());

            // 获取 RestrictRequest 注解，获取注解信息
            RestrictRequest restrictRequest = method.getAnnotation(RestrictRequest.class);
            long interval = restrictRequest.interval();
            int count = restrictRequest.count();
            String message = restrictRequest.message();

            // 获得访问 被注解标注的接口 的http请求
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            assert requestAttributes != null;

            HttpServletRequest request = requestAttributes.getRequest();
            HttpServletResponse response = requestAttributes.getResponse();

            assert response != null;
            response.setContentType("application/json;charset=UTF-8");

            // TODO 以下操作可以使用 lua脚本 进行实现，从而保证 操作的原子性 以及降低 网络开销
            // 得到 限制ip的key 和 限制user的key
            String ipKey = RedisConstants.ACCESS_LIMIT_IP_KEY + IPUtil.getIpAddr(request) + "-" + request.getRequestURI();
            String userKey = RedisConstants.ACCESS_LIMIT_USER_KEY + UserHolder.getUser().getId() + "-" + request.getRequestURI();

            // 获取 ip锁
            ipLock = redissonClient.getLock("lock:" + ipKey);
            isIp = ipLock.tryLock();
            if (!isIp) {
                throw new BusinessException("相同IP不能在同一时刻下单！");
            }

            // 获取 user锁
            userLock = redissonClient.getLock("lock" + userKey);
            isUser = userLock.tryLock();
            if (!isUser) {
                throw new BusinessException("相同用户不能在同一时刻下单！");
            }

            // 取得在限定时间内的访问次数
            String ipValue = stringRedisTemplate.opsForValue().get(ipKey);
            int ipCount = (ipValue == null ? 0 : Integer.parseInt(ipValue));

            String userValue = stringRedisTemplate.opsForValue().get(userKey);
            int userCount = (userValue == null ? 0 : Integer.parseInt(userValue));

            // 如果 还没有达到最大访问次数，则将redis中的次数加1
            if (ipCount < count && userCount < count) {
                // ip
                if (ipCount == 0) {
                    stringRedisTemplate.opsForValue().set(ipKey, Integer.toString(1), interval, TimeUnit.SECONDS);
                } else {
                    stringRedisTemplate.opsForValue().increment(ipKey, 1);
                }
                // user
                if (userCount == 0) {
                    stringRedisTemplate.opsForValue().set(userKey, Integer.toString(1), interval, TimeUnit.SECONDS);
                } else {
                    stringRedisTemplate.opsForValue().increment(userKey, 1);
                }
            } else {
                message = "".equals(message) ? "相同IP或用户在" + interval + "秒内达到了最大访问次数：" + count : message;
                throw new BusinessException(message);
            }
        } finally {
            // 释放已经拿到的锁
            if (isIp) {
                ipLock.unlock();
            }
            if (isUser) {
                userLock.unlock();
            }
        }
    }

    /**
     * 防重提交 回绕通知；
     * <p>
     * 参考 <a href="https://www.cnblogs.com/muxi0407/p/11818999.html">MethodSignature</a>
     * </p>
     *
     * @param proceedingJoinPoint
     * @return
     */
    @Around("repeatSubmitPointcut()")
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        // 获取当前 http request 对象
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes());
        HttpServletRequest request = requestAttributes.getRequest();

        // 获取当前登录用户id
        Long userId = UserHolder.getUser().getId();

        // 根据 方法签名 得到 具体实现方法具体实现方法
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = proceedingJoinPoint.getTarget().getClass().getMethod(signature.getName(), signature.getParameterTypes());

        // 得到 @RepeatSubmit 注解
        RepeatSubmit repeatSubmit = method.getAnnotation(RepeatSubmit.class);

        // 防重 类型判断
        boolean result;
        String type = repeatSubmit.limitType().name();
        if (type.equalsIgnoreCase(RepeatSubmit.Type.PARAM.name())) {
            // 方法一：参数形式防重提交
            long lockTime = repeatSubmit.lockTime();
            String ipAddr = IPUtil.getIpAddr(request);

            // 拿到 接口方法？
            MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
            method = methodSignature.getMethod();

            // 锁的key
            String className = method.getDeclaringClass().getName();
            String key = "order-server:repeat_submit:" + Arrays.toString(DigestUtils.md5Digest(String.format("%s-%s-%s-%s", ipAddr, className, method, userId).getBytes()));

            // redisson加锁
            RLock lock = redissonClient.getLock(key);
            // 尝试加锁，最多等待0秒，上锁5{lockTime}秒后自动解锁
            result = lock.tryLock(0, lockTime, TimeUnit.SECONDS);

        } else {
            // 方法二：令牌形式防重提交
            String requestToken = request.getHeader("request-token");
            if (!StringUtils.hasText(requestToken)) {
                throw new BusinessException("token is blank.");
            }

            // 建立key
            String key = String.format(RedisConstants.REPEAT_SUBMIT_ORDER_TOKEN_KEY, userId, requestToken);

            // 直接删除，删除成功表示完成
            result = Boolean.TRUE.equals(stringRedisTemplate.delete(key));
        }

        if (!result) {
            log.error("请求重复提交");
            log.info("环绕通知中");
        }

        // 开始执行方法
        return proceedingJoinPoint.proceed();

    }


}

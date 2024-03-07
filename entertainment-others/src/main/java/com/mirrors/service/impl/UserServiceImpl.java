package com.mirrors.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mirrors.dto.LoginFormDTO;
import com.mirrors.dto.Result;
import com.mirrors.dto.UserDTO;
import com.mirrors.entity.User;
import com.mirrors.mapper.UserMapper;
import com.mirrors.service.IUserService;
import com.mirrors.utils.RegexUtils;
import com.mirrors.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.mirrors.utils.RedisConstants.*;
import static com.mirrors.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * 用户 相关服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送验证码
     *
     * @param phone
     * @param session
     * @return
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号码格式错误");
        }

        // 生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 保存redis，验证码有效期
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL_SECONDS, TimeUnit.SECONDS);

        // 假设发送验证码成功（非借助第三方）
        log.debug("发送验证码：" + code);

        return Result.ok();
    }

    /**
     * 登录
     *
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号码格式错误");
        }

        // 从redis获取，校验验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)) {
            return Result.fail("验证码错误");
        }

        // 查询用户
        User user = query().eq("phone", phone).one();
        if (user == null) {
            // 不存在，创建用户
            user = new User();
            user.setPhone(phone);
            user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(5));
            // 保存到数据库
            save(user);
        }

        // key为随机token，返回前端
        String token = UUID.randomUUID().toString(true);

        // 保存信息到redis，保存UserDTO，浪费内存少
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> map = BeanUtil.beanToMap(
                userDTO,
                new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> {
                            return fieldValue.toString(); // 将其他类型 全部转为String
                        })
        );

        // 存储，并验证有效期
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, map);
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL_MINUTES, TimeUnit.MINUTES);

        // 返回token
        return Result.ok(token);
    }

    /**
     * 签到
     *
     * @return
     */
    @Override
    public Result sign() {
        // 获取用户
        Long userId = UserHolder.getUser().getId();

        // 获取当前日期
        LocalDateTime now = LocalDateTime.now();
        String key = "sign:" + userId + now.format(DateTimeFormatter.ofPattern(":yyyyMM"));

        // 今天是本月第几天
        int dayOfMonth = now.getDayOfMonth();
        int offset = dayOfMonth - 1;

        // 写入redis
        stringRedisTemplate.opsForValue().setBit(key, offset, true);
        return Result.ok();
    }

    /**
     * 签到统计
     *
     * @return
     */
    @Override
    public Result signCount() {
        // 获取用户
        Long userId = UserHolder.getUser().getId();

        // 获取当前日期
        LocalDateTime now = LocalDateTime.now();
        String key = "sign:" + userId + now.format(DateTimeFormatter.ofPattern(":yyyyMM"));

        // 今天是本月第几天
        int dayOfMonth = now.getDayOfMonth();

        // 获取所有签到记录
        List<Long> bitField = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if (bitField == null || bitField.isEmpty()) {
            return Result.ok(0);
        }

        Long num = bitField.get(0);
        if (num == null || num == 0) {
            return Result.ok(0);
        }

        // 与 1 做 与运算，循环
        int count = 0;
        while (true) {
            if ((num & 1) == 0) {
                break; // 为0，未签到
            } else {
                count++;
            }
            // num 无符号右移1位
            num = num >>> 1;
        }
        return Result.ok(count);
    }

}

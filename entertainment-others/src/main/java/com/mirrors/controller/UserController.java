package com.mirrors.controller;


import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import com.mirrors.dto.CaptchaDTO;
import com.mirrors.dto.LoginFormDTO;
import com.mirrors.dto.Result;
import com.mirrors.dto.UserDTO;
import com.mirrors.entity.User;
import com.mirrors.entity.UserInfo;
import com.mirrors.service.IUserInfoService;
import com.mirrors.service.IUserService;
import com.mirrors.utils.UserHolder;
import com.mirrors.validate.ValidationGroups;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 用户相关
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // -------------------------------------主要实现----------------------------------------------------

    /**
     * 生成手机号码验证码，并没有实现发送，只是显示
     *
     * @param phone
     * @param session
     * @return
     */
    @PostMapping("/code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        Result result = userService.sendCode(phone, session);
        System.out.println(result);
        return result;
    }

    /**
     * 登录功能
     *
     * @param loginForm
     * @param session
     * @return
     */
    @PostMapping("/login")
    public Result login(@RequestBody @Validated(ValidationGroups.Insert.class) LoginFormDTO loginForm, HttpSession session) {
        Result result = userService.login(loginForm, session);
        System.out.println(result);
        return result;
    }

    /**
     * 登出功能
     *
     * @return
     */
    @PostMapping("/logout")
    public Result logout() {
        // 删除ThreadLocal的值，以及Redis种的session（拦截器已写入）
        UserHolder.removeUser();

        return Result.ok("退出成功");
    }

    /**
     * 登录验证，获取登录的用户信息
     *
     * @return
     */
    @GetMapping("/me")
    public Result me() {
        // 从ThreadLocal取（拦截器已写入）
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    // -------------------------------------舍弃功能----------------------------------------------------

    /**
     * 根据id查询用户，详细信息
     *
     * @param userId
     * @return
     */
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId) {
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }


    /**
     * 根据id查询用户，简略信息
     *
     * @param userId
     * @return
     */
    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId) {
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }

    /**
     * 签到
     *
     * @return
     */
    @PostMapping("/sign")
    public Result sign() {
        Result result = userService.sign();
        System.out.println(result);
        return result;
    }

    /**
     * 签到统计
     *
     * @return
     */
    @PostMapping("/sign/count")
    public Result signCount() {
        Result result = userService.signCount();
        System.out.println(result);
        return result;
    }

}

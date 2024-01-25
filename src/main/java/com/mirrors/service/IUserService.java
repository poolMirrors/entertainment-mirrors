package com.mirrors.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mirrors.dto.LoginFormDTO;
import com.mirrors.dto.Result;
import com.mirrors.entity.User;

import javax.servlet.http.HttpSession;

/**
 * 用户服务类
 */
public interface IUserService extends IService<User> {

    /**
     * 发送验证码
     *
     * @param phone
     * @param session
     * @return
     */
    Result sendCode(String phone, HttpSession session);


    /**
     * 登录
     *
     * @param loginForm
     * @param session
     * @return
     */
    Result login(LoginFormDTO loginForm, HttpSession session);

    /**
     * 签到
     *
     * @return
     */
    Result sign();

    /**
     * 签到统计
     *
     * @return
     */
    Result signCount();
}

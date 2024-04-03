package com.mirrors.controller;


import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.core.lang.UUID;
import com.mirrors.dto.CaptchaDTO;
import com.mirrors.dto.LoginFormDTO;
import com.mirrors.dto.Result;
import com.mirrors.prevent.RestrictRequest;
import com.mirrors.service.IVoucherOrderService;
import com.mirrors.utils.UserHolder;
import com.mirrors.validate.ValidationGroups;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 订单管理
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 图片数字验证码
     *
     * @return
     */
    @GetMapping("/captcha")
    public CaptchaDTO getCaptcha() {
        // 创建一个图像验证码宽度为130，高度为48，包含6个字符，干扰线6个
        CircleCaptcha circleCaptcha = CaptchaUtil.createCircleCaptcha(130, 48, 6, 6);
        // 获取验证码的文本
        String captchaText = circleCaptcha.getCode();
        // 获取验证码图片的Base64编码
        String captchaImageBase64Data = circleCaptcha.getImageBase64Data();
        // 拦截器写入的userId，作为验证码id
        Long captchaId = UserHolder.getUser().getId();
        captchaId = Optional.ofNullable(captchaId).orElseGet(() -> Long.valueOf(UUID.randomUUID().toString()));
        // 保存验证码文本到Redis中，有效期30秒【下次从Redis取进行校验】
        stringRedisTemplate.opsForValue().set("captcha:" + captchaId, captchaText, 60, TimeUnit.SECONDS);

        return CaptchaDTO.builder()
                .captchaId(captchaId.toString())
                .captchaImage(captchaImageBase64Data)
                .build();
    }

    /**
     * 校验图片验证码；采用重定向？
     * <a href="https://blog.csdn.net/m0_45899013/article/details/106928429">重定向和转发</a>
     *
     * @param voucherId
     * @param captchaText
     * @param captchaId
     * @param response
     * @return
     */
    @PostMapping("/captchaCheck/{voucherId}")
    public String login(@PathVariable("id") Long voucherId, @RequestBody String captchaText, @RequestBody String captchaId, HttpServletResponse response) {
        // 校验
        String origin = stringRedisTemplate.opsForValue().get("captcha:" + captchaId);
        if(origin.equals(captchaText)) {
            try {
                response.sendRedirect("/seckill/" + voucherId);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }
        return "验证失败";
    }

    /**
     * 秒杀下单
     *
     * @param voucherId
     * @return
     */
    @PostMapping("/seckill/{id}")
    @RestrictRequest(interval = 3, count = 3000) // TODO 限制接口访问（限流）
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        Result result = voucherOrderService.seckillVoucherRabbitMQ(voucherId);
        System.out.println(result);
        return result;
    }
}

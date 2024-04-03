package com.mirrors.controller;

import com.mirrors.service.ITokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author mirrors
 * @version 1.0
 * @date 2024/1/9 10:37
 */
@RestController
public class TokenController {

    @Autowired
    private ITokenService iTokenService;

    // ----------------------------------------舍弃功能---------------------------------------------------

    /**
     * 获取当前token
     *
     * @return
     */
    @Deprecated
    @GetMapping("token")
    public String getOrderToken() {
        String orderToken = iTokenService.getOrderToken();
        System.out.println(orderToken);
        return orderToken;
    }
}

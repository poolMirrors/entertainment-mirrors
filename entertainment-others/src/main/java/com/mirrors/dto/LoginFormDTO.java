package com.mirrors.dto;

import com.mirrors.validate.ValidationGroups;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
@Builder
public class LoginFormDTO {

    @NotEmpty(message = "登录手机号不能为空1", groups = {ValidationGroups.Insert.class}) // 参数合法性校验
    @NotEmpty(message = "登录手机号不能为空2", groups = {ValidationGroups.Update.class}) // 分组
    private String phone;

    // 其他属性也要分组？
    @NotEmpty(message = "登录验证码不能为空1", groups = {ValidationGroups.Insert.class}) // 参数合法性校验
    private String code;

    // 其他属性也要分组？
    //@NotEmpty(message = "登录密码不能为空1", groups = {ValidationGroups.Insert.class}) // 参数合法性校验
    private String password;
}

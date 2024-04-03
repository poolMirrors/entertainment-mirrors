package com.mirrors.dto;

import lombok.Builder;
import lombok.Data;

/**
 * @author mirrors
 * @version 1.0
 * @date 2024/4/1 19:50
 */

@Data
@Builder
public class CaptchaDTO {
    //验证码id
    private String captchaId;

    //验证码图片base64编码
    private String captchaImage;
}

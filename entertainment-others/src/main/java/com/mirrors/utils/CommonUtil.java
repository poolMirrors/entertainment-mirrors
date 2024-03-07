package com.mirrors.utils;

import java.util.Random;

/**
 * 通用工具类
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/9 17:48
 */
public class CommonUtil {

    private static final String ALL_CHAR_NUM = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * 获取指定长度的随机串
     *
     * @param length
     * @return
     */
    public static String getStringNumRandom(int length) {
        // 生成随机数字和字母
        Random random = new Random();
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ALL_CHAR_NUM.charAt(random.nextInt(ALL_CHAR_NUM.length())));
        }
        return builder.toString();
    }

}

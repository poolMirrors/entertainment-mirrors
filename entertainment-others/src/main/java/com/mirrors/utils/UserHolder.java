package com.mirrors.utils;

import com.mirrors.dto.UserDTO;

/**
 * 保存当前用户
 */
public class UserHolder {

    private static final ThreadLocal<UserDTO> threadLocal = new ThreadLocal<>();

    public static void saveUser(UserDTO userDTO) {
        threadLocal.set(userDTO);
    }

    public static UserDTO getUser() {
        return threadLocal.get();
    }

    public static void removeUser() {
        threadLocal.remove();
    }
}

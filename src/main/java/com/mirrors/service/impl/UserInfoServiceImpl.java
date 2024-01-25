package com.mirrors.service.impl;

import com.mirrors.entity.UserInfo;
import com.mirrors.mapper.UserInfoMapper;
import com.mirrors.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 用户信息 服务实现类
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}

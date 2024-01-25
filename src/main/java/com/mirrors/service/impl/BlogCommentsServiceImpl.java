package com.mirrors.service.impl;

import com.mirrors.entity.BlogComments;
import com.mirrors.mapper.BlogCommentsMapper;
import com.mirrors.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 博客评论 服务实现类
 */
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}

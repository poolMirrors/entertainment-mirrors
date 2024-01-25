package com.mirrors.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mirrors.dto.Result;

/**
 * @author mirrors
 * @version 1.0
 * @date 2024/1/20 15:16
 */
public interface ISearchService {

    /**
     * 从Elasticsearch中查询blog
     *
     * @param text
     * @param page
     * @param size
     * @return
     */
    Result searchBlogByEs(String text, int page, int size);
}

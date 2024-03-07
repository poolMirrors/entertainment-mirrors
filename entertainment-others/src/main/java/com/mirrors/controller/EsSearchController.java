package com.mirrors.controller;

import com.mirrors.dto.Result;
import com.mirrors.service.ISearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Elasticsearch相关搜索
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/20 10:52
 */
@RestController
@RequestMapping("/es")
public class EsSearchController {

    @Autowired
    private ISearchService searchService;

    /**
     * 查询推文blog，同时分页
     *
     * @param text
     * @param page
     * @param size
     * @return
     */
    @GetMapping("/blog/{text}/{page}/{size}")
    public Result list(@PathVariable("text") String text, @PathVariable("page") Integer page, @PathVariable("size") Integer size) {
        Result result = searchService.searchBlogByEs(text, page.intValue(), size.intValue());
        System.out.println(result);
        return result;
    }

}

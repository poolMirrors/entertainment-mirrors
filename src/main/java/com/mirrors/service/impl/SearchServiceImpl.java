package com.mirrors.service.impl;

import com.alibaba.fastjson.JSON;
import com.mirrors.dto.Result;
import com.mirrors.entity.Blog;
import com.mirrors.service.ISearchService;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mirrors
 * @version 1.0
 * @date 2024/1/20 15:18
 */
@Service
public class SearchServiceImpl implements ISearchService {

    @Resource
    private RestHighLevelClient restHighLevelClient;

    /**
     * 从Elasticsearch中查询blog
     *
     * @param text
     * @param page
     * @param size
     * @return
     */
    @Override
    public Result searchBlogByEs(String text, int page, int size) {
        // 从Elasticsearch中查询blog
        try {
            // 准备查询参数
            SearchRequest request = new SearchRequest("blog");
            request.source().query(QueryBuilders.multiMatchQuery(text, "title", "content"));
            // 发送请求
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            // 结果解析
            SearchHits searchHits = response.getHits();
            long total = searchHits.getTotalHits().value;
            System.out.println("共有" + total + "条数据");
            // 文档数组
            SearchHit[] hits = searchHits.getHits();
            List<Blog> list = new ArrayList<>();
            for (SearchHit hit : hits) {
                String json = hit.getSourceAsString();
                Blog blog = JSON.parseObject(json, Blog.class);

                System.out.println("Blog = " + json);
                list.add(blog);
            }
            // TODO 从es中查询到blog返回结果
            return Result.ok(list);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.fail("查找blog出错");
    }
}

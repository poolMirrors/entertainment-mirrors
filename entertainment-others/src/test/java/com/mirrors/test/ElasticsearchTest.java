package com.mirrors.test;

import com.alibaba.fastjson.JSON;
import com.mirrors.entity.Blog;
import com.mirrors.service.IBlogService;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

/**
 * @author mirrors
 * @version 1.0
 * @date 2024/1/20 10:26
 */
@SpringBootTest
public class ElasticsearchTest {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private IBlogService blogService;

    /**
     * 将blog存入es
     */
    @Test
    public void testBulkRequest() throws IOException {
        List<Blog> list = blogService.list();

        BulkRequest bulkRequest = new BulkRequest();
        for (Blog blog : list) {
            String json = JSON.toJSONString(blog);
            bulkRequest.add(
                    new IndexRequest("blog").id(blog.getId().toString()).source(json, XContentType.JSON)
            );
        }

        restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    }

}

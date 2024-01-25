package com.mirrors.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置Elasticsearch
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/20 9:46
 */
@Configuration
public class ElasticsearchConfig {

    @Value("${es.address}")
    private String address;

    @Bean
    public RestHighLevelClient restHighLevelClient() {
        return new RestHighLevelClient(
                RestClient.builder(HttpHost.create(address))
        );
    }

}

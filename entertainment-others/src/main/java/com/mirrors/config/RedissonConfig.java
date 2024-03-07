package com.mirrors.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * redisson客户端类
 */
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        // 配置
        Config config = new Config();
        // 单体，添加redis地址（也可以使用config.useClusterServers()添加集群地址）
        config.useSingleServer().setAddress("redis://192.168.101.130:6379").setPassword("root");
        // 创建
        return Redisson.create(config);
    }

}

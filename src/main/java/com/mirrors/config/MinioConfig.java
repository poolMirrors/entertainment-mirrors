package com.mirrors.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO配置类，自定义yaml文件配置
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/16 10:46
 */
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint) // 注意区分 管理界面端口 还是 服务端口
                .credentials(accessKey, secretKey)
                .build();
    }

}

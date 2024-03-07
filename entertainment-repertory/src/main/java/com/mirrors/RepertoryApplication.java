package com.mirrors;

import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author mirrors
 * @version 1.0
 * @date 2024/3/7 15:14
 */
@DubboComponentScan(basePackages = {"com.mirrors.service.impl"})
@SpringBootApplication
public class RepertoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(RepertoryApplication.class);
    }
}

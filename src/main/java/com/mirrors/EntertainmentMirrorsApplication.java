package com.mirrors;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true) // 暴露代理对象
@MapperScan("com.mirrors.mapper")
@SpringBootApplication
public class EntertainmentMirrorsApplication {

    public static void main(String[] args) {
        SpringApplication.run(EntertainmentMirrorsApplication.class, args);
    }

}

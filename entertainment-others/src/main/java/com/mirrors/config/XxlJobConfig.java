package com.mirrors.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 针对多网卡、容器内部署等情况，可借助 "spring-cloud-commons" 提供的 "InetUtils" 组件灵活定制注册IP；
 * <p>
 * 1、引入依赖：
 * <dependency>
 * <groupId>org.springframework.cloud</groupId>
 * <artifactId>spring-cloud-commons</artifactId>
 * <version>${version}</version>
 * </dependency>
 * <p>
 * 2、配置文件，或者容器启动变量
 * spring.cloud.inetutils.preferred-networks: 'xxx.xxx.xxx.'
 * <p>
 * 3、获取IP
 * String ip_ = inetUtils.findFirstNonLoopbackHostInfo().getIpAddress();
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/17 17:16
 */
@Slf4j
//@Configuration TODO 任务调度配置
public class XxlJobConfig {

    @Value("${xxl-job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl-job.accessToken}")
    private String accessToken;

    @Value("${xxl-job.executor.app-name}")
    private String appName;

    @Value("${xxl-job.executor.address}")
    private String address;

    @Value("${xxl-job.executor.ip}")
    private String ip;

    @Value("${xxl-job.executor.port}")
    private int port;

    @Value("${xxl-job.executor.log-path}")
    private String logPath;

    @Value("${xxl-job.executor.log-retention-days}")
    private int logRetentionDays;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info(">>>>>>>>>>> xxl-job config init.");

        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(appName);
        xxlJobSpringExecutor.setAddress(address);
        xxlJobSpringExecutor.setIp(ip);
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setAccessToken(accessToken);
        xxlJobSpringExecutor.setLogPath(logPath);
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);

        return xxlJobSpringExecutor;
    }
}

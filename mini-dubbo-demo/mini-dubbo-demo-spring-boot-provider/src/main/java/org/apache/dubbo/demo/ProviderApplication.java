package org.apache.dubbo.demo;

import org.apache.dubbo.spring.boot.annotation.EnableDubbo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.CountDownLatch;

/**
 * Spring Boot Provider 启动入口。
 * <p>
 * 通过 @EnableDubbo 启用注解扫描，自动发现 @DubboService 标注的服务并暴露。
 * 配置从 application.yml 读取。
 */
@SpringBootApplication
@EnableDubbo(scanBasePackages = "org.apache.dubbo.demo")
public class ProviderApplication {

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(ProviderApplication.class, args);
        new CountDownLatch(1).await();
    }
}

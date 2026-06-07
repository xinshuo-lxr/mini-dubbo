package org.apache.dubbo.demo;

import org.apache.dubbo.spring.boot.annotation.EnableDubbo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Consumer 启动入口。
 * <p>
 * 通过 @EnableDubbo 启用注解扫描，自动注入 @DubboReference 标注的远程服务代理。
 * 配置从 application.yml 读取。
 */
@SpringBootApplication
@EnableDubbo
public class ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}

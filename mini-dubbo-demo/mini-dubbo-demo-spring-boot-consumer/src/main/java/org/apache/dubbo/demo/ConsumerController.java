package org.apache.dubbo.demo;

import org.apache.dubbo.spring.boot.annotation.DubboReference;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 消费者示例 — 使用 @DubboReference 注入远程服务代理。
 * <p>
 * 实现 CommandLineRunner，在 Spring Boot 启动后自动调用远程服务。
 */
@Component
public class ConsumerController implements CommandLineRunner {

    @DubboReference
    private DemoService demoService;

    @Override
    public void run(String... args) {
        String result = demoService.sayHello("mini-dubbo");
        System.out.println("RPC result: " + result);
    }
}

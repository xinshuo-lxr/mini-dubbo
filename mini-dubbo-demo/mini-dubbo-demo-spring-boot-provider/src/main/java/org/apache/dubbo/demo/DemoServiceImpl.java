package org.apache.dubbo.demo;

import org.apache.dubbo.spring.boot.annotation.DubboService;

/**
 * DemoService 实现类 — 使用 @DubboService 注解标记为 Dubbo 服务。
 */
@DubboService
public class DemoServiceImpl implements DemoService {

    @Override
    public String sayHello(String name) {
        return "Hello " + name + ", this is mini-dubbo!";
    }
}

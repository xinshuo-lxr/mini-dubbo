package org.apache.dubbo.demo;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.Bootstrap;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;

/**
 * Provider 启动入口 — 暴露 DemoService 到 ZooKeeper。
 *
 * 启动前确保 ZooKeeper 运行在 localhost:2181：
 *   docker run -d -p 2181:2181 zookeeper:3.8
 */
public class ProviderApplication {

    public static void main(String[] args) throws Exception {
        Bootstrap.provider()
                .application(new ApplicationConfig("mini-dubbo-provider"))
                .registry(new RegistryConfig("zookeeper://localhost:2181"))
                .protocol(new ProtocolConfig("dubbo", 20880))
                .service(DemoService.class, new DemoServiceImpl())
                .start();

        System.out.println("Provider started. Press Enter to exit...");
        System.in.read();
    }
}

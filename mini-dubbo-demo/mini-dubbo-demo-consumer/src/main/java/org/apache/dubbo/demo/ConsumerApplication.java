package org.apache.dubbo.demo;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.Bootstrap;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;

/**
 * Consumer 启动入口 — 从 ZooKeeper 发现服务并调用。
 *
 * 启动前确保：
 * 1. ZooKeeper 运行在 localhost:2181
 * 2. ProviderApplication 已启动
 */
public class ConsumerApplication {

    public static void main(String[] args) {
        ReferenceConfig<DemoService> reference = Bootstrap.consumer()
                .application(new ApplicationConfig("mini-dubbo-consumer"))
                .registry(new RegistryConfig("zookeeper://localhost:2181"))
                .reference(DemoService.class);

        // get() 触发：subscribe → Cluster.join → Proxy 创建
        DemoService demoService = reference.get();

        // 发起 RPC 调用
        String result = demoService.sayHello("mini-dubbo");
        System.out.println("RPC result: " + result);
    }
}

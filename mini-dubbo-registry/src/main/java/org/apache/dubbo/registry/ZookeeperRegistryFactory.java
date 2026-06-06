package org.apache.dubbo.registry;

import org.apache.dubbo.common.URL;

/**
 * ZooKeeper 注册中心工厂 — 创建 ZookeeperRegistry 实例。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.registry.zookeeper.ZookeeperRegistryFactory
 */
public class ZookeeperRegistryFactory implements RegistryFactory {

    @Override
    public Registry getRegistry(URL url) {
        return new ZookeeperRegistry(url);
    }
}

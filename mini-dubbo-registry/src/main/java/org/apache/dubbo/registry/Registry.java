package org.apache.dubbo.registry;

import org.apache.dubbo.common.URL;

/**
 * 注册中心接口 — 服务注册、订阅、发现。
 *
 * Provider 调用 register() 注册服务地址。
 * Consumer 调用 subscribe() 订阅服务变化，通过 NotifyListener 收到通知。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.registry.Registry
 */
public interface Registry {

    /**
     * 注册服务地址（Provider 端）
     * 在 ZK 中创建临时节点：/dubbo/{interfaceName}/providers/{url}
     */
    void register(URL url);

    /**
     * 取消注册（Provider 下线时）
     */
    void unregister(URL url);

    /**
     * 订阅服务变化（Consumer 端）
     * 当 Provider 上下线时，通过 listener 回调通知
     */
    void subscribe(URL url, NotifyListener listener);

    /**
     * 取消订阅
     */
    void unsubscribe(URL url, NotifyListener listener);

    /**
     * 获取当前已注册的所有服务 URL
     */
    java.util.List<URL> lookup(URL url);
}

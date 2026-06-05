package org.apache.dubbo.registry;

import org.apache.dubbo.common.URL;

import java.util.List;

/**
 * 变更通知回调 — 注册中心发现 Provider 变化时通知 Consumer。
 *
 * RegistryDirectory 实现了这个接口。
 * 当 Provider 上下线时，ZK 触发回调，Directory 更新内部的 Invoker 列表。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.registry.NotifyListener
 */
public interface NotifyListener {

    /**
     * 当注册中心的服务列表发生变化时回调
     *
     * @param urls 最新的服务 URL 列表
     */
    void notify(List<URL> urls);
}

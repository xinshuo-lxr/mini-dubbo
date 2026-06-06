package org.apache.dubbo.rpc.cluster.directory;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Directory;

import java.util.Collections;
import java.util.List;

/**
 * 静态目录 — 持有固定的 Invoker 列表，不会动态变化。
 *
 * 用于直连模式（用户直接指定 Provider 地址）。
 * 与 RegistryDirectory 对比：RegistryDirectory 会订阅注册中心，动态更新列表。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.cluster.directory.StaticDirectory
 */
public class StaticDirectory<T> implements Directory<T> {

    private final Class<T> serviceType;
    private final URL url;
    private final List<Invoker<T>> invokers;
    private volatile boolean destroyed;

    public StaticDirectory(Class<T> serviceType, URL url, List<Invoker<T>> invokers) {
        this.serviceType = serviceType;
        this.url = url;
        this.invokers = invokers;
    }

    @Override
    public Class<T> getInterface() { return serviceType; }

    @Override
    public List<Invoker<T>> list(Invocation invocation)  throws RpcException {
        if (destroyed) {
            throw new IllegalStateException("Directory is destroyed");
        }
        return invokers;
    }

    @Override
    public List<Invoker<T>> getAllInvokers() {
        return Collections.unmodifiableList(invokers);
    }

    @Override
    public URL getUrl() { return url; }

    @Override
    public boolean isAvailable() { return !destroyed && !invokers.isEmpty(); }

    @Override
    public void destroy() {
        destroyed = true;
        for (Invoker<T> invoker : invokers) {
            invoker.destroy();
        }
    }
}

package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.List;

/**
 * 集群 Invoker 基类 — 模板方法模式。
 *
 * invoke() 做三件事：
 * 1. directory.list(invocation) — 从 Directory 获取可用 Invoker 列表
 * 2. initLoadBalance() — 初始化负载均衡策略
 * 3. doInvoke(invocation, invokers, loadbalance) — 子类实现（重试/快速失败等）
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker
 */
public abstract class AbstractClusterInvoker<T> implements Invoker<T> {

    private final Directory<T> directory;
    private volatile boolean destroyed;

    public AbstractClusterInvoker(Directory<T> directory) {
        this.directory = directory;
    }

    @Override
    public URL getUrl() { return directory.getUrl(); }

    @Override
    public Class<T> getInterface() { return directory.getInterface(); }

    @Override
    public boolean isAvailable() { return !destroyed && directory.isAvailable(); }

    @Override
    public void destroy() {
        destroyed = true;
        directory.destroy();
    }

    public Directory<T> getDirectory() { return directory; }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        if (destroyed) {
            throw new RpcException("Invoker is destroyed");
        }

        // ① 从 Directory 获取可用 Invoker 列表
        List<Invoker<T>> invokers = directory.list(invocation);
        if (invokers == null || invokers.isEmpty()) {
            throw new RpcException("No available invokers for " + invocation.getServiceName());
        }

        // ② 初始化负载均衡策略
        LoadBalance loadbalance = initLoadBalance(invokers, invocation);

        // ③ 子类实现：重试/快速失败等
        return doInvoke(invocation, invokers, loadbalance);
    }

    /**
     * 初始化负载均衡策略
     * 从 URL 的 loadbalance 参数读取，默认 "random"
     */
    protected LoadBalance initLoadBalance(List<Invoker<T>> invokers, Invocation invocation) {
        String loadbalanceKey = getUrl().getMethodParameter(invocation.getMethodName(), "loadbalance", "random");
        return ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(loadbalanceKey);
    }

    /**
     * 子类实现：集群调用策略
     */
    protected abstract Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException;
}

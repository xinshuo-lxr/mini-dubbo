package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;

import java.util.List;

/**
 * Directory 接口 — 动态维护可用 Invoker 列表。
 *
 * ClusterInvoker 不直接持有 Invoker 列表，而是每次调用时从 Directory 获取。
 * Directory 订阅注册中心，Provider 上下线时自动更新列表。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.cluster.Directory
 */
public interface Directory<T> {

    /**
     * 获取服务接口类型
     */
    Class<T> getInterface();

    /**
     * 获取可用 Invoker 列表（经过路由过滤）
     */
    List<Invoker<T>> list(Invocation invocation) throws RpcException;

    /**
     * 获取所有 Invoker（未经过路由过滤）
     */
    List<Invoker<T>> getAllInvokers();

    /**
     * 获取 URL
     */
    URL getUrl();

    /**
     * 是否可用
     */
    boolean isAvailable();

    /**
     * 销毁
     */
    void destroy();
}

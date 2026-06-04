package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;

/**
 * Cluster 接口 — 集群容错策略扩展点。
 *
 * join(directory) 把 Directory 包装成 ClusterInvoker。
 * ClusterInvoker 持有 Directory，每次调用时：
 *   1. 从 Directory 获取 Invoker 列表
 *   2. 通过 LoadBalance 选一个
 *   3. 调用选中的 Invoker（失败时根据策略重试/快速失败/安全失败等）
 *
 * @SPI("failover") 表示默认使用失败重试策略。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.cluster.Cluster
 */
@SPI("failover")
public interface Cluster {

    /**
     * 把 Directory 包装成 ClusterInvoker
     */
    @Adaptive
    <T> Invoker<T> join(Directory<T> directory) throws RpcException;
}

package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.Directory;

/**
 * 快速失败集群策略 — 失败立即抛异常，不重试。
 *
 * 适用于非幂等的写操作（如扣款、下单），重试可能导致重复执行。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.cluster.support.FailfastCluster
 */
public class FailfastCluster implements Cluster {

    public static final String NAME = "failfast";

    @Override
    public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
        return new FailfastClusterInvoker<>(directory);
    }
}

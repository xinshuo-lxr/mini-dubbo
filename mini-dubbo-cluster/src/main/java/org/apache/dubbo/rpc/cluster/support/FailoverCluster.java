package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.Directory;

/**
 * 失败重试集群策略 — 失败时自动重试其他 Provider。
 *
 * 默认重试 2 次（共 3 次机会），适用于幂等的读操作。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.cluster.support.FailoverCluster
 */
public class FailoverCluster implements Cluster {

    public static final String NAME = "failover";

    @Override
    public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
        return new FailoverClusterInvoker<>(directory);
    }
}

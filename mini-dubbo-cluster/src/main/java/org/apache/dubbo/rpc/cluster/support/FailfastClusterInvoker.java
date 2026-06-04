package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.List;

/**
 * 快速失败集群 Invoker — 失败立即抛异常，不重试。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.cluster.support.FailfastClusterInvoker
 */
public class FailfastClusterInvoker<T> extends AbstractClusterInvoker<T> {

    public FailfastClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    protected Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        // 只调用一次，不重试
        Invoker<T> invoker = loadbalance.select(invokers, invocation);
        try {
            return invoker.invoke(invocation);
        } catch (RpcException e) {
            throw e;
        } catch (Exception e) {
            throw new RpcException("Failfast invoke failed: " + e.getMessage(), e);
        }
    }
}

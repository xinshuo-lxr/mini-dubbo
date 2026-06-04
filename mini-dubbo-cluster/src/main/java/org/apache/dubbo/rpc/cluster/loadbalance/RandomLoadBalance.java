package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机负载均衡 — 从 Invoker 列表中随机选择一个。
 *
 * 这是最简单的负载均衡策略。
 * 真实 Dubbo 还支持加权随机（根据 Provider 配置的 weight 参数）。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.cluster.loadbalance.RandomLoadBalance
 */
public class RandomLoadBalance implements LoadBalance {

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, Invocation invocation) throws RpcException {
        if (invokers == null || invokers.isEmpty()) {
            throw new RpcException("No available invokers");
        }
        if (invokers.size() == 1) {
            return invokers.get(0);
        }
        return invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
    }
}

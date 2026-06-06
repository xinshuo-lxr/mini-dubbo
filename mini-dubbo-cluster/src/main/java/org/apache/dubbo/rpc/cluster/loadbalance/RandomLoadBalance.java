package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.ArrayList;
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
    public <T> Invoker<T> select(List<Invoker<T>> invokers, Invocation invocation, List<Invoker<T>> invoked) throws RpcException {
        if (invokers == null || invokers.isEmpty()) {
            throw new RpcException("No available invokers");
        }
        if (invokers.size() == 1) {
            return invokers.get(0);
        }

        // 如果有已调用过的 invoker，优先从未调用过的中选择
        if (invoked != null && !invoked.isEmpty() && invokers.size() > invoked.size()) {
            List<Invoker<T>> notInvoked = new ArrayList<>(invokers);
            notInvoked.removeAll(invoked);
            if (!notInvoked.isEmpty()) {
                return notInvoked.get(ThreadLocalRandom.current().nextInt(notInvoked.size()));
            }
        }

        return invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
    }
}

package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.ArrayList;
import java.util.List;

/**
 * 失败重试集群 Invoker — 失败时自动重试其他 Provider。
 *
 * doInvoke() 逻辑：
 * 1. 计算重试次数（retries + 1，默认 3 次）
 * 2. 每次重试通过 LoadBalance 重新选择一个 Provider
 * 3. 成功则返回，失败则重试
 * 4. 全部失败则抛出最后一次的异常
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.cluster.support.FailoverClusterInvoker
 */
public class FailoverClusterInvoker<T> extends AbstractClusterInvoker<T> {

    public FailoverClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    protected Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        // 计算重试次数：retries 参数 + 1（默认 retries=2，共 3 次）
        int retries = getUrl().getMethodParameter(invocation.getMethodName(), "retries", 2);
        int len = retries + 1;

        // 记录已经调用过的 Invoker（避免重试时选到同一个）
        List<Invoker<T>> invoked = new ArrayList<>();
        RpcException lastException = null;

        for (int i = 0; i < len; i++) {
            // 通过 LoadBalance 选择一个 Invoker
            Invoker<T> invoker = loadbalance.select(invokers, invocation);
            invoked.add(invoker);

            try {
                // 调用选中的 Invoker
                Result result = invoker.invoke(invocation);
                return result;
            } catch (RpcException e) {
                lastException = e;
                // 业务异常不重试，直接抛出
                if (e.isBiz()) {
                    throw e;
                }
                // 非业务异常（网络超时等），继续重试
            }
        }

        throw new RpcException("Failed after " + len + " retries: " + lastException.getMessage(), lastException);
    }
}

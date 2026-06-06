package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;

import java.util.List;

/**
 * LoadBalance 接口 — 负载均衡策略扩展点。
 *
 * 从多个可用 Invoker 中选择一个。
 *
 * @SPI("random") 表示默认使用随机策略。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.cluster.LoadBalance
 */
@SPI("random")
public interface LoadBalance {

    /**
     * 从 Invoker 列表中选择一个
     *
     * @param invokers   可用 Invoker 列表
     * @param invocation 当前调用
     * @param invoked    已经调用过的 Invoker 列表（用于 Failover 重试时排除）
     * @return 选中的 Invoker
     */
    @Adaptive
    <T> Invoker<T> select(List<Invoker<T>> invokers, Invocation invocation, List<Invoker<T>> invoked) throws RpcException;
}

package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

/**
 * 集群级 Filter 接口 — 在 LoadBalance 选择 Provider 之前执行。
 *
 * 典型实现：ConsumerContextFilter（设置 RpcContext）、FutureFilter（异步回调）
 * 此时还不知道要调用哪个 Provider。
 *
 * 与 Filter 的区别：
 * - ClusterFilter：集群级，LoadBalance 之前，不知道目标 Provider
 * - Filter：实例级，LoadBalance 之后，已知目标 Provider
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.cluster.filter.ClusterFilter
 */
@SPI
public interface ClusterFilter {

    Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException;
}

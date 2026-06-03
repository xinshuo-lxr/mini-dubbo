package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

/**
 * 实例级 Filter 接口 — 在 LoadBalance 选择 Provider 之后执行。
 *
 * 典型实现：TokenFilter（令牌校验）、ActiveLimitFilter（限流）
 * 此时已经知道要调用哪个具体的 Provider。
 *
 * 用法：filter.invoke(nextInvoker, invocation)
 * filter 做完自己的事，然后调用 nextInvoker.invoke(invocation) 传递给下一层。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.Filter
 */
@SPI
public interface Filter {

    Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException;
}

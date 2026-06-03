package org.apache.dubbo.rpc.proxy;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;

/**
 * ProxyFactory 接口 — 代理工厂扩展点。
 *
 * getInvoker(): Provider 端，把业务实现类包装成 Invoker
 * getProxy():   Consumer 端，把 Invoker 包装成代理对象
 *
 * @SPI("jdk") 表示默认使用 JDK 动态代理。
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.ProxyFactory
 */
@SPI("jdk")
public interface ProxyFactory {

    /**
     * Provider 端：业务对象 → Invoker
     * 把 DemoServiceImpl 包装成 AbstractProxyInvoker
     */
    @Adaptive
    <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException;

    /**
     * Consumer 端：Invoker → 代理对象
     * 把 Invoker 包装成 JDK 动态代理，用户调用方法时被 InvokerInvocationHandler 拦截
     */
    @Adaptive
    <T> T getProxy(Invoker<T> invoker) throws RpcException;
}

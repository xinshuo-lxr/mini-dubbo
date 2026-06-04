package org.apache.dubbo.rpc.proxy;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;

import java.lang.reflect.Proxy;

/**
 * JDK 动态代理工厂。
 *
 * getInvoker(): Provider 端，业务对象 → Invoker
 * getProxy():   Consumer 端，Invoker → 代理对象
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.proxy.jdk.JdkProxyFactory
 */
public class JdkProxyFactory implements ProxyFactory {

    /**
     * Provider 端：把业务实现类包装成 Invoker
     */
    @Override
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException {
        return new AbstractProxyInvoker<>(proxy, type, url);
    }

    /**
     * Consumer 端：把 Invoker 包装成 JDK 动态代理
     *
     * 返回的对象实现了服务接口（如 DemoService），
     * 用户调用方法时被 InvokerInvocationHandler 拦截。
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Invoker<T> invoker) throws RpcException {
        return (T) Proxy.newProxyInstance(
                invoker.getInterface().getClassLoader(),
                new Class[]{invoker.getInterface()},
                new InvokerInvocationHandler(invoker)
        );
    }
}

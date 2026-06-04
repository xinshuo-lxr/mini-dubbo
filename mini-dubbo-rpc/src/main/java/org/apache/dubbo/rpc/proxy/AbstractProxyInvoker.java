package org.apache.dubbo.rpc.proxy;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.lang.reflect.Method;

/**
 * Provider 端 Invoker — 包装业务实现类。
 *
 * 当 Consumer 发来请求时，通过反射调用真正的业务方法。
 * 例如：invocation.getMethodName() = "sayHello"
 *      → 反射调用 demoServiceImpl.sayHello("dubbo")
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.proxy.AbstractProxyInvoker
 */
public class AbstractProxyInvoker<T> implements Invoker<T> {

    private final T proxy;       // 业务实现类（如 DemoServiceImpl）
    private final Class<T> type; // 服务接口（如 DemoService.class）
    private final URL url;

    public AbstractProxyInvoker(T proxy, Class<T> type, URL url) {
        this.proxy = proxy;
        this.type = type;
        this.url = url;
    }

    @Override
    public URL getUrl() { return url; }

    @Override
    public Class<T> getInterface() { return type; }

    @Override
    public boolean isAvailable() { return true; }

    @Override
    public void destroy() {}

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        try {
            // 通过反射调用业务方法
            Method method = type.getMethod(invocation.getMethodName(), invocation.getParameterTypes());
            Object result = method.invoke(proxy, invocation.getArguments());
            return new AppResponse(result);
        } catch (Exception e) {
            return new AppResponse(e);
        }
    }
}

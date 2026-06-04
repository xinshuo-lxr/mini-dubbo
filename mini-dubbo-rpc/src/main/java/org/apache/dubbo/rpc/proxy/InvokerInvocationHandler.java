package org.apache.dubbo.rpc.proxy;

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 代理拦截器 — 把方法调用转发给 Invoker。
 *
 * 用户调用 demoService.sayHello("dubbo") 时：
 * 1. JDK 动态代理拦截方法调用
 * 2. 创建 RpcInvocation（methodName、args 等）
 * 3. 调用 invoker.invoke(invocation) — 进入 Invoker 链
 * 4. 返回结果
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.proxy.InvokerInvocationHandler
 */
public class InvokerInvocationHandler implements InvocationHandler {

    private final Invoker<?> invoker;

    public InvokerInvocationHandler(Invoker<?> invoker) {
        this.invoker = invoker;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Object 方法直接代理（toString、hashCode 等）
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        // 创建 RpcInvocation
        RpcInvocation invocation = new RpcInvocation(
                method.getName(),
                invoker.getInterface().getName(),
                method.getParameterTypes(),
                args
        );

        // 调用 Invoker 链
        Result result = invoker.invoke(invocation);

        // 有异常就抛出
        if (result.hasException()) {
            throw result.getException();
        }
        return result.getValue();
    }
}

package org.apache.dubbo.rpc.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;

/**
 * Consumer 端 Invoker 基类 — 模板方法模式。
 *
 * invoke() 做三件事：
 * 1. prepareInvocation() — 设置 invoker 反向引用、合并 URL 参数到 attachments
 * 2. doInvoke() — 子类实现（真正的 RPC 调用）
 * 3. 简化版直接返回结果
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.protocol.AbstractInvoker
 */
public abstract class AbstractInvoker<T> implements Invoker<T> {

    private final Class<T> type;
    private final URL url;
    private volatile boolean destroyed;

    public AbstractInvoker(Class<T> type, URL url) {
        this.type = type;
        this.url = url;
    }

    @Override
    public URL getUrl() { return url; }

    @Override
    public Class<T> getInterface() { return type; }

    @Override
    public boolean isAvailable() { return !destroyed; }

    @Override
    public void destroy() { destroyed = true; }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        if (destroyed) {
            throw new RpcException("Invoker is destroyed");
        }
        // ① 准备调用数据
        prepareInvocation(invocation);
        // ② 子类实现：真正的 RPC 调用
        return doInvoke(invocation);
    }

    /**
     * 准备调用数据：
     * - 设置反向引用（invocation → invoker）
     * - 把 URL 参数合并到 attachments（path、version、timeout、group 等）
     */
    private void prepareInvocation(Invocation invocation) {
        invocation.setInvoker(this);
        // 合并 URL 参数到 attachments
        if (invocation instanceof RpcInvocation) {
            RpcInvocation rpcInvocation = (RpcInvocation) invocation;
            for (java.util.Map.Entry<String, String> entry : url.getParameters().entrySet()) {
                if (!rpcInvocation.getAttachments().containsKey(entry.getKey())) {
                    rpcInvocation.setAttachment(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * 子类实现：真正的 RPC 调用逻辑
     */
    protected abstract Result doInvoke(Invocation invocation) throws RpcException;
}

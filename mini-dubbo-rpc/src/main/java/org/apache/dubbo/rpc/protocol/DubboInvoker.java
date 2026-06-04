package org.apache.dubbo.rpc.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.NettyClient;
import org.apache.dubbo.remoting.Request;
import org.apache.dubbo.remoting.Response;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;

/**
 * Dubbo 协议 Invoker — Consumer 端真正发送 RPC 请求的 Invoker。
 *
 * doInvoke() 做的事：
 * 1. 设置 PATH_KEY、VERSION_KEY 到 attachments
 * 2. 创建 Request 对象，把 RpcInvocation 塞进去
 * 3. 通过 NettyClient 发送请求，等待响应
 * 4. 返回 AppResponse
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.protocol.dubbo.DubboInvoker
 */
public class DubboInvoker<T> extends AbstractInvoker<T> {

    private final NettyClient client;

    public DubboInvoker(Class<T> type, URL url, NettyClient client) {
        super(type, url);
        this.client = client;
    }

    @Override
    protected Result doInvoke(Invocation invocation) throws RpcException {
        RpcInvocation rpcInvocation = (RpcInvocation) invocation;

        // 设置服务路径和版本到 attachments
        rpcInvocation.setAttachment("path", getUrl().getServiceInterface());
        rpcInvocation.setAttachment("version", getUrl().getParameter("version", ""));

        // 创建 Request
        Request request = new Request();
        request.setData(rpcInvocation);

        // 发送请求，等待响应
        int timeout = getUrl().getMethodParameter(invocation.getMethodName(), "timeout", 1000);
        Response response = client.send(request, timeout);

        // 处理响应
        if (response.getErrorMsg() != null && !response.getErrorMsg().isEmpty()) {
            throw new RpcException("Remote call failed: " + response.getErrorMsg());
        }

        return (AppResponse) response.getResult();
    }

    @Override
    public void destroy() {
        super.destroy();
        client.close();
    }
}

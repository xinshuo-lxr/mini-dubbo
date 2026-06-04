package org.apache.dubbo.rpc.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.NettyClient;
import org.apache.dubbo.remoting.NettyServer;
import org.apache.dubbo.remoting.Request;
import org.apache.dubbo.remoting.Response;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dubbo 协议实现 — Provider 端暴露服务，Consumer 端创建 Invoker。
 *
 * export(): Provider 端
 *   → 把 Invoker 注册到 serviceMap
 *   → 启动 NettyServer（如果还没启动）
 *   → NettyServer 收到请求时，从 serviceMap 找到 Invoker 执行调用
 *
 * refer(): Consumer 端
 *   → 创建 NettyClient 连接到 Provider
 *   → 返回 DubboInvoker（持有 NettyClient）
 *
 * 对应 Dubbo 源码：org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol
 */
public class DubboProtocol implements Protocol {

    private static final Logger logger = LoggerFactory.getLogger(DubboProtocol.class);

    /** serviceKey → Invoker 映射（Provider 端暴露的所有服务） */
    private final Map<String, Invoker<?>> serviceMap = new ConcurrentHashMap<>();

    /** 已启动的 Server：host:port → NettyServer */
    private final Map<String, NettyServer> serverMap = new ConcurrentHashMap<>();

    /** 已创建的 Client：host:port → NettyClient */
    private final Map<String, NettyClient> clientMap = new ConcurrentHashMap<>();

    /**
     * Provider 端：暴露服务
     */
    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        URL url = invoker.getUrl();
        String serviceKey = url.getServiceKey();

        // 注册服务
        serviceMap.put(serviceKey, invoker);
        logger.info("Exported service: " + serviceKey + " on port " + url.getPort());

        // 启动 NettyServer（如果还没启动）
        String address = url.getHost() + ":" + url.getPort();
        if (!serverMap.containsKey(address)) {
            synchronized (serverMap) {
                if (!serverMap.containsKey(address)) {
                    try {
                        NettyServer server = new NettyServer(url.getPort(), this::handleRequest);
                        server.start();
                        serverMap.put(address, server);
                    } catch (InterruptedException e) {
                        throw new RpcException("Failed to start server: " + e.getMessage(), e);
                    }
                }
            }
        }

        return new SimpleExporter<>(invoker);
    }

    /**
     * Consumer 端：引用服务
     */
    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        String address = url.getHost() + ":" + url.getPort();

        // 获取或创建 NettyClient（连接复用）
        NettyClient client = clientMap.get(address);
        if (client == null) {
            synchronized (clientMap) {
                client = clientMap.get(address);
                if (client == null) {
                    try {
                        client = new NettyClient(url.getHost(), url.getPort());
                        client.connect();
                        clientMap.put(address, client);
                    } catch (InterruptedException e) {
                        throw new RpcException("Failed to connect: " + e.getMessage(), e);
                    }
                }
            }
        }

        return new DubboInvoker<>(type, url, client);
    }

    /**
     * 处理 Consumer 发来的请求（在 NettyServer 的 IO 线程中执行）
     */
    private Response handleRequest(Request request) {
        Response response = new Response(request.getId());
        try {
            Invocation invocation = (Invocation) request.getData();
            String serviceKey = invocation.getServiceName();
            Invoker<?> invoker = serviceMap.get(serviceKey);

            if (invoker == null) {
                response.setErrorMsg("Service not found: " + serviceKey);
            } else {
                Result result = invoker.invoke(invocation);
                response.setResult(result);
            }
        } catch (Exception e) {
            response.setErrorMsg(e.getMessage());
        }
        return response;
    }

    /**
     * 简单的 Exporter 实现
     */
    private static class SimpleExporter<T> implements Exporter<T> {
        private final Invoker<T> invoker;

        public SimpleExporter(Invoker<T> invoker) {
            this.invoker = invoker;
        }

        @Override
        public Invoker<T> getInvoker() { return invoker; }

        @Override
        public void unexport() { invoker.destroy(); }
    }
}

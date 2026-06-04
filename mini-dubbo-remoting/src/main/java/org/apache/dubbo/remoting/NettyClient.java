package org.apache.dubbo.remoting;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Netty 客户端 — Consumer 端连接 Provider，发送 RPC 请求。
 *
 * 工作流程：
 * 1. 连接到 Provider 的 host:port
 * 2. 发送请求 → 编码 Request → 通过 Channel 发出
 * 3. 等待响应 → 收到字节 → 解码 Response → 匹配 Request ID → 完成 Future
 *
 * 对应 Dubbo 源码：org.apache.dubbo.remoting.transport.netty4.NettyClient
 */
public class NettyClient {

    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);

    private final String host;
    private final int port;
    private Channel channel;
    private EventLoopGroup group;

    /** 未完成的请求：requestId → CompletableFuture，用于异步等待响应 */
    private final ConcurrentMap<Long, CompletableFuture<Response>> pendingRequests = new ConcurrentHashMap<>();

    public NettyClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 连接到 Provider
     */
    public void connect() throws InterruptedException {
        group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new ClientHandler(pendingRequests));
                    }
                });

        ChannelFuture future = bootstrap.connect(host, port).sync();
        this.channel = future.channel();
        logger.info("Netty Client connected to " + host + ":" + port);
    }

    /**
     * 发送请求并等待响应
     *
     * @param request 请求对象
     * @param timeout 超时时间（毫秒）
     * @return 响应对象
     */
    public Response send(Request request, int timeout) {
        CompletableFuture<Response> future = new CompletableFuture<>();
        pendingRequests.put(request.getId(), future);

        try {
            // 编码 → 发送
            byte[] bytes = DubboCodec.encode(request);
            channel.writeAndFlush(Unpooled.wrappedBuffer(bytes));

            // 等待响应（超时则抛异常）
            return future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            pendingRequests.remove(request.getId());
            throw new RuntimeException("Failed to send request: " + e.getMessage(), e);
        }
    }

    /**
     * 关闭连接
     */
    public void close() {
        if (channel != null) channel.close();
        if (group != null) group.shutdownGracefully();
        logger.info("Netty Client closed");
    }

    /**
     * 客户端处理器：收到字节 → 解码 Response → 找到对应的 Future → 完成
     */
    private static class ClientHandler extends ChannelInboundHandlerAdapter {
        private final ConcurrentMap<Long, CompletableFuture<Response>> pendingRequests;

        public ClientHandler(ConcurrentMap<Long, CompletableFuture<Response>> pendingRequests) {
            this.pendingRequests = pendingRequests;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf buf = (ByteBuf) msg;
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);

            // 解码 Response
            Response response = (Response) DubboCodec.decode(bytes);

            // 找到对应的 Future，完成它
            CompletableFuture<Response> future = pendingRequests.remove(response.getId());
            if (future != null) {
                future.complete(response);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Client handler error", cause);
            ctx.close();
        }
    }
}

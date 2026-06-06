package org.apache.dubbo.remoting;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
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
 * Pipeline：
 *   Encoder（Request → ByteBuf）→ Decoder（ByteBuf → Response）→ ClientHandler（匹配 Future）
 *
 * 对应 Dubbo 源码：org.apache.dubbo.remoting.transport.netty4.NettyClient
 */
public class NettyClient {

    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);

    private final String host;
    private final int port;
    private Channel channel;
    private EventLoopGroup group;

    /** 未完成的请求：requestId → CompletableFuture */
    private final ConcurrentMap<Long, CompletableFuture<Response>> pendingRequests = new ConcurrentHashMap<>();

    public NettyClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws InterruptedException {
        group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast("encoder", new RequestEncoder())
                                .addLast("decoder", new ResponseDecoder())
                                .addLast("handler", new ClientHandler(pendingRequests));
                    }
                });

        ChannelFuture future = bootstrap.connect(host, port).sync();
        this.channel = future.channel();
        logger.info("Netty Client connected to " + host + ":" + port);
    }

    /**
     * 发送请求并等待响应
     */
    public Response send(Request request, int timeout) {
        CompletableFuture<Response> future = new CompletableFuture<>();
        pendingRequests.put(request.getId(), future);

        try {
            channel.writeAndFlush(request);
            return future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            pendingRequests.remove(request.getId());
            throw new RuntimeException("Failed to send request: " + e.getMessage(), e);
        }
    }

    public void close() {
        if (channel != null) channel.close();
        if (group != null) group.shutdownGracefully();
        // 清理所有 pending 请求
        for (CompletableFuture<Response> future : pendingRequests.values()) {
            future.completeExceptionally(new RuntimeException("Channel closed"));
        }
        pendingRequests.clear();
        logger.info("Netty Client closed");
    }

    /**
     * 编码器：Request → ByteBuf
     */
    private static class RequestEncoder extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof Request) {
                ByteBuf out = ctx.alloc().buffer();
                DubboCodec.encodeRequest(out, (Request) msg);
                ctx.write(out, promise);
            } else {
                ctx.write(msg, promise);
            }
        }
    }

    /**
     * 解码器：ByteBuf → Response
     */
    private static class ResponseDecoder extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf buf = (ByteBuf) msg;

            DubboCodec.Header header = DubboCodec.decodeHeader(buf);
            if (header == null) {
                return;
            }

            if (!header.isResponse()) {
                throw new RuntimeException("Expected response but got type: " + header.getType());
            }

            if (buf.readableBytes() < header.getDataLength()) {
                throw new RuntimeException("Incomplete data: expected " + header.getDataLength()
                        + " but got " + buf.readableBytes());
            }

            Object data = DubboCodec.decodeData(buf, header.getDataLength());

            Response response = new Response(header.getId());
            if (data instanceof String) {
                response.setErrorMsg((String) data);
            } else {
                response.setResult(data);
            }

            ctx.fireChannelRead(response);
        }
    }

    /**
     * 业务处理器：匹配 Response 和 Future
     */
    private static class ClientHandler extends ChannelInboundHandlerAdapter {
        private final ConcurrentMap<Long, CompletableFuture<Response>> pendingRequests;

        public ClientHandler(ConcurrentMap<Long, CompletableFuture<Response>> pendingRequests) {
            this.pendingRequests = pendingRequests;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof Response) {
                Response response = (Response) msg;
                CompletableFuture<Response> future = pendingRequests.remove(response.getId());
                if (future != null) {
                    if (response.getErrorMsg() != null) {
                        future.complete(response);
                    } else {
                        future.complete(response);
                    }
                }
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            // 连接断开时，fail 所有 pending 请求
            for (CompletableFuture<Response> future : pendingRequests.values()) {
                future.completeExceptionally(new RuntimeException("Channel disconnected"));
            }
            pendingRequests.clear();
            logger.warn("Channel disconnected: " + ctx.channel().remoteAddress());
            super.channelInactive(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Client handler error", cause);
            ctx.close();
        }
    }
}

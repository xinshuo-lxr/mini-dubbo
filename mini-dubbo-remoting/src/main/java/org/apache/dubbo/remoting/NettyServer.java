package org.apache.dubbo.remoting;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Netty 服务端 — Provider 端监听端口，接收 Consumer 的 RPC 请求。
 *
 * 工作流程：
 * 1. 绑定端口，启动 Netty Server
 * 2. 收到请求 → 解码为 Request
 * 3. 调用 requestHandler 处理请求 → 得到 Response
 * 4. 编码 Response → 发回给 Consumer
 *
 * 对应 Dubbo 源码：org.apache.dubbo.remoting.transport.netty4.NettyServer
 */
public class NettyServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private final int port;

    /** 请求处理器：Request → Response */
    private final Function<Request, Response> requestHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyServer(int port, Function<Request, Response> requestHandler) {
        this.port = port;
        this.requestHandler = requestHandler;
    }

    /**
     * 启动 Netty Server
     */
    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new ServerHandler(requestHandler));
                    }
                });

        ChannelFuture future = bootstrap.bind(port).sync();
        logger.info("Netty Server started on port " + port);
    }

    /**
     * 停止 Netty Server
     */
    public void stop() {
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        logger.info("Netty Server stopped");
    }

    /**
     * 服务端处理器：收到字节 → 解码 Request → 处理 → 编码 Response → 发回
     */
    private static class ServerHandler extends ChannelInboundHandlerAdapter {
        private final Function<Request, Response> requestHandler;

        public ServerHandler(Function<Request, Response> requestHandler) {
            this.requestHandler = requestHandler;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // 读取字节
            ByteBuf buf = (ByteBuf) msg;
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);

            // 解码 Request
            Request request = (Request) DubboCodec.decode(bytes);

            // 处理请求
            Response response = requestHandler.apply(request);

            // 编码 Response → 发回
            byte[] responseBytes = DubboCodec.encode(response);
            ctx.writeAndFlush(Unpooled.wrappedBuffer(responseBytes));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Server handler error", cause);
            ctx.close();
        }
    }
}

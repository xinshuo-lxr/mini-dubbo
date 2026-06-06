package org.apache.dubbo.remoting;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Netty 服务端 — Provider 端监听端口，接收 Consumer 的 RPC 请求。
 *
 * 线程模型：
 *   Boss 线程（1个）：接收连接
 *   Worker 线程（默认 CPU*2）：网络 I/O（读写 ByteBuf）
 *   业务线程池（独立）：执行 requestHandler（避免阻塞 IO 线程）
 *
 * Pipeline：
 *   Decoder（ByteBuf → Request）→ ServerHandler（分派到业务线程）→ Encoder（Response → ByteBuf）
 *
 * 对应 Dubbo 源码：org.apache.dubbo.remoting.transport.netty4.NettyServer
 */
public class NettyServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private final int port;
    private final Function<Request, Response> requestHandler;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    /** 业务线程池 — 执行 requestHandler，不阻塞 Netty IO 线程 */
    private final ExecutorService bizExecutor;

    public NettyServer(int port, Function<Request, Response> requestHandler) {
        this.port = port;
        this.requestHandler = requestHandler;
        this.bizExecutor = new ThreadPoolExecutor(
                0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new NamedThreadFactory("mini-dubbo-biz")
        );
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast("decoder", new RequestDecoder())
                                .addLast("encoder", new ResponseEncoder())
                                .addLast("handler", new ServerHandler(requestHandler, bizExecutor));
                    }
                });

        ChannelFuture future = bootstrap.bind(port).sync();
        logger.info("Netty Server started on port " + port);
    }

    public void stop() {
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        bizExecutor.shutdown();
        logger.info("Netty Server stopped");
    }

    /**
     * 解码器：ByteBuf → Request
     */
    private static class RequestDecoder extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf buf = (ByteBuf) msg;

            DubboCodec.Header header = DubboCodec.decodeHeader(buf);
            if (header == null) {
                return;
            }

            if (!header.isRequest()) {
                throw new RuntimeException("Expected request but got type: " + header.getType());
            }

            if (buf.readableBytes() < header.getDataLength()) {
                throw new RuntimeException("Incomplete data: expected " + header.getDataLength()
                        + " but got " + buf.readableBytes());
            }

            Object data = DubboCodec.decodeData(buf, header.getDataLength());

            Request request = new Request(header.getId());
            request.setData(data);

            ctx.fireChannelRead(request);
        }
    }

    /**
     * 编码器：Response → ByteBuf
     */
    private static class ResponseEncoder extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof Response) {
                ByteBuf out = ctx.alloc().buffer();
                DubboCodec.encodeResponse(out, (Response) msg);
                ctx.write(out, promise);
            } else {
                ctx.write(msg, promise);
            }
        }
    }

    /**
     * 业务处理器：将请求分派到业务线程池执行
     *
     * 不在 IO 线程上执行业务逻辑，避免阻塞 EventLoop。
     * 业务线程执行完后调用 ctx.writeAndFlush()，Netty 会自动切换回 IO 线程写数据。
     */
    private static class ServerHandler extends ChannelInboundHandlerAdapter {
        private final Function<Request, Response> requestHandler;
        private final ExecutorService bizExecutor;

        public ServerHandler(Function<Request, Response> requestHandler, ExecutorService bizExecutor) {
            this.requestHandler = requestHandler;
            this.bizExecutor = bizExecutor;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof Request) {
                Request request = (Request) msg;
                // 分派到业务线程池，不阻塞 IO 线程
                bizExecutor.execute(() -> {
                    try {
                        Response response = requestHandler.apply(request);
                        ctx.writeAndFlush(response);
                    } catch (Exception e) {
                        logger.error("Biz handler error", e);
                        Response errorResponse = new Response(request.getId());
                        errorResponse.setErrorMsg(e.getMessage());
                        ctx.writeAndFlush(errorResponse);
                    }
                });
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Server handler error", cause);
            ctx.close();
        }
    }

    /**
     * 命名线程池工厂 — 线程名带前缀，方便排查问题
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final String prefix;

        public NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }
}

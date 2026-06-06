package org.apache.dubbo.remoting;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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

import java.util.function.Function;

/**
 * Netty 服务端 — Provider 端监听端口，接收 Consumer 的 RPC 请求。
 *
 * Pipeline：
 *   Decoder（ByteBuf → Request）→ ServerHandler（处理请求）→ Encoder（Response → ByteBuf）
 *
 * 对应 Dubbo 源码：org.apache.dubbo.remoting.transport.netty4.NettyServer
 */
public class NettyServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private final int port;
    private final Function<Request, Response> requestHandler;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyServer(int port, Function<Request, Response> requestHandler) {
        this.port = port;
        this.requestHandler = requestHandler;
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
                                .addLast("handler", new ServerHandler(requestHandler));
                    }
                });

        ChannelFuture future = bootstrap.bind(port).sync();
        logger.info("Netty Server started on port " + port);
    }

    public void stop() {
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        logger.info("Netty Server stopped");
    }

    /**
     * 解码器：ByteBuf → Request
     *
     * 1. 读协议头（15 字节）
     * 2. 读数据体（dataLength 字节）
     * 3. 反序列化为 Request 对象
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
     * 业务处理器：Request → Response
     */
    private static class ServerHandler extends ChannelInboundHandlerAdapter {
        private final Function<Request, Response> requestHandler;

        public ServerHandler(Function<Request, Response> requestHandler) {
            this.requestHandler = requestHandler;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof Request) {
                Request request = (Request) msg;
                Response response = requestHandler.apply(request);
                ctx.writeAndFlush(response);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Server handler error", cause);
            ctx.close();
        }
    }
}

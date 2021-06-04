import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

/**
 * @Description
 * @Author tianbin
 * @Since 2021/5/25
 */
public class HttpServer2 {

    public void start(int port) throws InterruptedException {
        EventLoopGroup boos = new NioEventLoopGroup();
        EventLoopGroup work = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(boos, work)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()
                                    .addLast(new SimpleInboundHandler("inA", false))
                                    .addLast(new SimpleInboundHandler("inB", false))
                                    .addLast(new SimpleInboundHandler("inC", true));
                            socketChannel.pipeline()
                                    .addLast(new SampleOutBoundHandler("outA"))
                                    .addLast(new SampleOutBoundHandler("outB"))
                                    .addLast(new SampleOutBoundHandler("outC"))
                                    .addLast(new ExceptionHandler());
                        }
                    }).childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = serverBootstrap.bind(port).sync();
            System.out.println("Http Server started， Listening on " + port);
            future.channel().closeFuture().sync();
        } finally {
            work.shutdownGracefully();
            boos.shutdownGracefully();
        }
    }

    public class SimpleInboundHandler extends ChannelInboundHandlerAdapter {

        private String name;
        private final boolean flush;

        public SimpleInboundHandler(String name, boolean flush) {
            this.name = name;
            this.flush = flush;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println("InBoundHandler: " + name);
            if (flush) {
                ctx.channel().writeAndFlush(msg);
            } else {
                super.channelRead(ctx, msg);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            System.out.println("InBoundHandlerException: " + name);
            ctx.fireExceptionCaught(cause);
        }
    }

    public class SampleOutBoundHandler extends ChannelOutboundHandlerAdapter {
        private final String name;

        public SampleOutBoundHandler(String name) {
            this.name = name;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            System.out.println("OutBoundHandler: " + name);
            super.write(ctx, msg, promise);
        }
    }

    public class ExceptionHandler extends ChannelDuplexHandler {

        @Override

        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

            if (cause instanceof RuntimeException) {

                System.out.println("Handle Business Exception Success.");

            }

        }

    }


    public static void main(String[] args) throws InterruptedException {
        HttpServer2 httpServer = new HttpServer2();
        httpServer.start(8080);
    }
}

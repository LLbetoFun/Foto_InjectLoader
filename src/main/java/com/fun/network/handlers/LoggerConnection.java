package com.fun.network.handlers;

import com.fun.network.codec.MessageDecoder;
import com.fun.network.codec.MessageEncoder;
import com.fun.network.logger.Logger;
import com.fun.network.model.DataModel;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class LoggerConnection extends SimpleChannelInboundHandler<DataModel> {
    public static enum Flow{
        CLIENTBOUND, SERVERBOUND
    }


    public Channel channel;
    public final Flow flow;
    public static LoggerConnection connection;

    public LoggerConnection(Flow flow) {
        this.flow = flow;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DataModel msg) throws Exception {
        try {
            if (msg != null) Logger.unwrap(msg);
        }
        catch (NullPointerException ignored) {}
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        channel = ctx.channel();
        connection = this;
        System.out.println("Channel " + channel.id().asLongText() + " connected");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if(flow == Flow.CLIENTBOUND){
            //destroy self
            com.fun.inject.Bootstrap.destroyClient();
        }
        else if(flow == Flow.SERVERBOUND) {
            System.out.println("Channel " + channel.id().asLongText() + " disconnected");
            System.exit(0);
        }
    }

    public void writeMessage(DataModel packet) {
        assert channel != null;
        if (this.channel.eventLoop().inEventLoop()) {
            this.channel.writeAndFlush(packet).awaitUninterruptibly();
        } else {
            this.channel.eventLoop().execute(() -> {
                this.channel.writeAndFlush(packet).awaitUninterruptibly();
            });
        }
    }

    public static void main(String[] args) {
        LoggerConnection.startServer(new InetSocketAddress("localhost",13337));
    }

    public static void connect(SocketAddress socketAddress) {
        EventLoopGroup group;
        group = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast("decoder",new MessageDecoder())
                                    .addLast("encoder",new MessageEncoder())

                                    .addLast(new  LoggerConnection(Flow.CLIENTBOUND));
                        }
                    });

            Channel channel = b.connect(socketAddress).sync().channel();
            System.out.println("Connected to Message Server");

            // 发送测试消息

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public static void startServer(SocketAddress socketAddress) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast("encoder",new MessageEncoder())
                                    .addLast("decoder",new MessageDecoder())

                                    .addLast(new LoggerConnection(Flow.SERVERBOUND));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(socketAddress).sync();
            System.out.println("Log Server started");
            new Thread(()-> {
                try {
                    f.channel().closeFuture().sync();
                } catch (InterruptedException e) {

                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

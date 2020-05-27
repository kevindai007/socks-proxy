package com.kevindai.socks.proxy.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kevindai.socks.proxy.constants.BizConstants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author daiwenkai
 * @Date 18/02/2020 16:05
 **/
public class BizUtils {
    public static final Logger LOGGER = LoggerFactory.getLogger(BizUtils.class);
    private static EventLoopGroup bossGroup;
    private static EventLoopGroup workerGroup;
    private static EventLoopGroup customWorkerGroup = new DefaultEventLoopGroup(BizConstants.THREAD_NUM, new ThreadFactoryBuilder().setNameFormat("custom-worker-thread-%d").build());
    //用于管理所有连接的channel
    private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static ChannelGroup remoteChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);


    static {
        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(1, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyEPOLLBoss_%d", this.threadIndex.incrementAndGet()));
                }
            });
            workerGroup = new EpollEventLoopGroup(BizConstants.THREAD_NUM, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);
                private int threadTotal = BizConstants.THREAD_NUM;

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyServerEPOLLWorker_%d_%d", threadTotal, this.threadIndex.incrementAndGet()));
                }
            });
            LOGGER.info("using epoll ");
        } else if (KQueue.isAvailable()) {
            bossGroup = new KQueueEventLoopGroup(1, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyKQueueBoss_%d", this.threadIndex.incrementAndGet()));
                }
            });
            workerGroup = new KQueueEventLoopGroup(BizConstants.THREAD_NUM, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);
                private int threadTotal = BizConstants.THREAD_NUM;

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyKQueueWorker_%d_%d", threadTotal, this.threadIndex.incrementAndGet()));
                }
            });
            LOGGER.info("using kqueue");
        } else {
            bossGroup = new NioEventLoopGroup(1, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyNIOBoss_%d", this.threadIndex.incrementAndGet()));
                }
            });
            workerGroup = new NioEventLoopGroup(BizConstants.THREAD_NUM, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);
                private int threadTotal = BizConstants.THREAD_NUM;

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyNIOWorker_%d_%d", threadTotal, this.threadIndex.incrementAndGet()));
                }
            });
            LOGGER.info("using nio");

        }
    }

    public static void closeEventGroup() {
        bossGroup.shutdownGracefully().syncUninterruptibly();
        workerGroup.shutdownGracefully().syncUninterruptibly();
        customWorkerGroup.shutdownGracefully().syncUninterruptibly();
    }

    public static ChannelGroup getChannelGroup() {
        return channelGroup;
    }

    public static ChannelGroup getRemoteChannelGroup() {
        return remoteChannelGroup;
    }

    public static EventLoopGroup getCustomWorkerGroup() {
        return customWorkerGroup;
    }

    public static EventLoopGroup getBossGroup() {
        return bossGroup;
    }

    public static EventLoopGroup getWorkerGroup() {
        return workerGroup;
    }

    public static ServerBootstrap buildServerBootstrap() {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        if (Epoll.isAvailable()) {
            serverBootstrap.channel(EpollServerSocketChannel.class);
        } else if (KQueue.isAvailable()) {
            serverBootstrap.channel(KQueueServerSocketChannel.class);
        } else {
            serverBootstrap.channel(NioServerSocketChannel.class);
        }
        return serverBootstrap;
    }


    public static String getClientIpAddress(ChannelHandlerContext ctx) {
        try {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            return inetSocketAddress.getHostName() + ":" + inetSocketAddress.getPort();
        } catch (Exception e) {
            LOGGER.info("parse ip address error,{}", e);
            return null;
        }
    }

    public static void closeOnFlush(Channel channel) {
        if (channel != null && !channel.isActive()) {
            channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}

package com.kevindai.socks.proxy.server;

import com.kevindai.socks.proxy.util.BizUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

/**
 * @Author daiwenkai
 * @Date 18/02/2020 16:04
 **/
public abstract class AbstractServer {
    private static final Logger logger = LoggerFactory.getLogger(AbstractServer.class);

    @Value("${proxy.port}")
    protected int port;

    @Value("${socks.proxy.port}")
    protected int socksPort;

    protected EventLoopGroup bossGroup = BizUtils.getBossGroup();
    protected EventLoopGroup workerGroup = BizUtils.getWorkerGroup();
    protected ServerBootstrap serverBootstrap = BizUtils.buildServerBootstrap();
    protected ChannelFuture channelFuture;


    public abstract void start();

    public abstract ChannelFuture sync() throws Exception;
}

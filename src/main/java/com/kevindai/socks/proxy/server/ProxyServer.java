package com.kevindai.socks.proxy.server;

import com.kevindai.socks.proxy.constants.BizConstants;
import com.kevindai.socks.proxy.handler.ProxyServerHandler;
import com.kevindai.socks.proxy.util.BizUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * @Author daiwenkai
 * @Date 18/02/2020 16:25
 **/
@Component
public class ProxyServer extends AbstractServer {

    private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);

    @Override
    public void start() {
        this.serverBootstrap.group(this.bossGroup, this.workerGroup)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .handler(new LoggingHandler(LogLevel.INFO))
                .localAddress(new InetSocketAddress(this.port))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast("readTimeout", new ReadTimeoutHandler(BizConstants.MAX_IDLE_TIMEOUT));
                        pipeline.addLast("codec", new HttpServerCodec());
                        pipeline.addLast("aggregator", new HttpObjectAggregator(Integer.MAX_VALUE));
                        pipeline.addLast(BizUtils.getCustomWorkerGroup(), "handler", new ProxyServerHandler());
                    }
                });

        this.channelFuture = this.serverBootstrap.bind(this.port);
        logger.info("server start bind port :{}", this.port);
    }

    @Override
    public ChannelFuture sync() throws Exception {
        return this.channelFuture.sync().channel().closeFuture();
    }
}

package com.kevindai.socks.proxy.handler;

import com.kevindai.socks.proxy.util.ReferenceCountedUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * @Author: xm20200119
 * @Date: 26/05/2020 10:14
 */
@Slf4j
public class ProxyRelayHandler extends ChannelInboundHandlerAdapter {

    private Channel channel;

    public ProxyRelayHandler(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (channel.isActive()) {
                channel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        // was able to flush out data, start to read the next chunk
                        ctx.channel().read();
                    } else {
                        log.info("channel id:{},https,send response to target error.e:{}", channel.id(), future.cause());
                        future.channel().close();
                    }
                });
            } else {
                log.error("write response to client error");
                ReferenceCountedUtils.release(msg);
                channel.close();
                ctx.close();
            }
        } finally {
            MDC.clear();
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!(cause instanceof ReadTimeoutException)) {
            log.error(cause.getMessage());
        }
        channel.close();
        ctx.close();
        MDC.clear();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (channel != null) {
            closeOnFlush(channel);
        }
    }

    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}

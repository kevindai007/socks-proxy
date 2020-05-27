package com.kevindai.socks.proxy.handler;

import com.kevindai.socks.proxy.constants.ChannelAttributeKeyConstants;
import com.kevindai.socks.proxy.exception.SslHandShakeException;
import com.kevindai.socks.proxy.util.BizUtils;
import com.kevindai.socks.proxy.util.ReferenceCountedUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * @Author daiwenkai
 * @Date 20/02/2020 16:56
 **/
@Slf4j
public class ProxyClientHandler extends ChannelInboundHandlerAdapter {

    private volatile Channel channel;
    private String host;
    private Integer port;
    private String url;

    public ProxyClientHandler(Channel channel, String urlStr) {
        this.channel = channel;
        this.host = channel.attr(ChannelAttributeKeyConstants.REQUEST_HOST_KEY).get();
        this.port = channel.attr(ChannelAttributeKeyConstants.REQUEST_PORT_KEY).get();
        this.url = urlStr;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        MDC.put("url", url);

        try {
            if (channel.isActive()) {
                if (msg instanceof HttpResponse) {
                    FullHttpResponse response = (FullHttpResponse)msg;
                    response.retain();
                    if (response.status().code() != HttpResponseStatus.OK.code() || HttpUtil.isKeepAlive(response)) {
                        BizUtils.closeOnFlush(channel);
                    }
                    channel.writeAndFlush(msg);
                }
            } else {
                log.error("write response to client error");
                ReferenceCountedUtils.release(msg);
                channel.close();
                ctx.close();
            }

            super.channelRead(ctx, msg);
        } finally {
            MDC.clear();
        }

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
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
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof SslHandshakeCompletionEvent) {
            SslHandshakeCompletionEvent handshakeCompletionEvent = (SslHandshakeCompletionEvent) evt;
            if (handshakeCompletionEvent.isSuccess()) {
                log.info("ssl handshake success!");
            } else {
                Throwable cause = ((SslHandshakeCompletionEvent) evt).cause();
                log.info("ssl handshake failed!");
                throw new SslHandShakeException(cause.getMessage(), cause);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }

    }
}

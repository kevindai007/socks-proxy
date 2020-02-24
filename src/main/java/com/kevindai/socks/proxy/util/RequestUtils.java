package com.kevindai.socks.proxy.util;

import com.google.common.collect.Lists;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.util.List;

/**
 * @Author daiwenkai
 * @Date 19/02/2020 15:46
 **/
public class RequestUtils {
    public static final Logger logger = LoggerFactory.getLogger(RequestUtils.class);

    private static List<String> REMOVE_HEADERS = Lists.newArrayList("TASK_ID");
    private static SslContext sslCtx;

    static {
        try {
            sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } catch (SSLException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public static Pair<String, Integer> parseRemoteHostAndPort(HttpRequest httpRequest) {
        String host = httpRequest.headers().get(HttpHeaderNames.HOST);
        String[] split = host.split(":");
        logger.info("original host :{},methods :{} ", host, httpRequest.method());
        int port = 80;
        host = split[0];
        if (split.length > 1) {
            port = Integer.valueOf(split[1]);
        } else {
            String uri = httpRequest.uri();
            if (StringUtils.containsIgnoreCase(uri, HttpScheme.HTTPS.name())) {
                port = 443;
            }
            String method = httpRequest.method().name();
            if (StringUtils.containsIgnoreCase(method, HttpMethod.CONNECT.name())) {
                port = 443;
                split = uri.split(":");
                if (split.length > 1) {
                    port = Integer.valueOf(split[1]);
                }

            }
        }
        logger.info("parse host:{},port :{}", host, port);
        return Pair.of(host, port);

    }

    public static void removeHeaders(HttpHeaders httpHeaders) {
        REMOVE_HEADERS.forEach(httpHeaders::remove);
    }


    public static void handleHttpRequest(SocketChannel ch, ChannelPipeline pipeline, HttpRequest msg) {
        try {
            if (pipeline != null && StringUtils.containsIgnoreCase(msg.uri(), HttpScheme.HTTPS.name())) {
                SSLEngine sslEngine = sslCtx.newEngine(ch.alloc());
                sslEngine.setUseClientMode(true);
                sslEngine.setNeedClientAuth(false);
                pipeline.addLast("ssl", new SslHandler(sslEngine));
            }
        } catch (Exception e) {
            logger.error("ssl request error,{}", e.getMessage());
        }
    }
}

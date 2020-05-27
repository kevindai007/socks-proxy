package com.kevindai.socks.proxy.handler;

import com.kevindai.socks.proxy.constants.BizConstants;
import com.kevindai.socks.proxy.constants.ChannelAttributeKeyConstants;
import com.kevindai.socks.proxy.manager.ChannelManager;
import com.kevindai.socks.proxy.manager.ChannelTimeoutManager;
import com.kevindai.socks.proxy.manager.SpringContextManager;
import com.kevindai.socks.proxy.util.BizUtils;
import com.kevindai.socks.proxy.util.NetUtils;
import com.kevindai.socks.proxy.util.ReferenceCountedUtils;
import com.kevindai.socks.proxy.util.RequestUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.TimeoutException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.URL;

/**
 * @Author daiwenkai
 * @Date 18/02/2020 17:03
 **/
public class ProxyServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ProxyServerHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        BizUtils.getChannelGroup().add(ctx.channel());
        String clientIpAddress = BizUtils.getClientIpAddress(ctx);
        if (StringUtils.isNotBlank(clientIpAddress)) {
            //在channel中添加client_ip属性
            ctx.channel().attr(ChannelAttributeKeyConstants.CLIENT_IP_ATTRIBUTE_KEY).set(clientIpAddress);
            MDC.put("clientHost", clientIpAddress);
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ChannelManager channelManager = SpringContextManager.getBean(ChannelManager.class);
        // http请求、以及Https的第一次连接会被转化为FullHttpRequest,如果不是这标识是https协议简历第一次连接后后续的请求
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;
            Pair<String, Integer> pair = RequestUtils.parseRemoteHostAndPort(fullHttpRequest);
            String host = pair.getKey();
            Integer port = pair.getValue();

            ctx.channel().attr(ChannelAttributeKeyConstants.REQUEST_HOST_KEY).set(host);
            ctx.channel().attr(ChannelAttributeKeyConstants.REQUEST_PORT_KEY).set(port);

            //host是需要过滤的host
            if (!channelManager.checkFilterHost(host)) {
                logger.warn("found forbidden host : {}", host);
                ReferenceCountedUtils.release(msg);
                ctx.close();
                return;
            }


            if (HttpMethod.CONNECT.name().equalsIgnoreCase(fullHttpRequest.method().name())) {
                //https第一次连接，直接返回成功
                HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                ctx.writeAndFlush(response);

                ctx.pipeline().remove("codec");
                ctx.pipeline().remove("aggregator");

                ReferenceCountedUtils.release(ctx);
            } else {
                //http请求直接返回响应
                HttpHeaders headers = fullHttpRequest.headers();
                RequestUtils.removeHeaders(headers);

                String urlStr = fullHttpRequest.uri();
                String webHost = NetUtils.getWebHost(fullHttpRequest);
                logger.info("send request to :{}", webHost);


                createConnection(ctx, fullHttpRequest, host, port, urlStr);
            }


        }else{
            //https建立连接之后的请求会走以下逻辑
            String host = ctx.channel().attr(ChannelAttributeKeyConstants.REQUEST_HOST_KEY).get();
            Integer port = ctx.channel().attr(ChannelAttributeKeyConstants.REQUEST_PORT_KEY).get();
            if (StringUtils.isNotBlank(host) && port != null) {
                if (port != 443) {
                    //有些Https的端口不是443
                    logger.warn("https request with unusual port! host: {}, port: {}", host, port);
                    ctx.channel().attr(ChannelAttributeKeyConstants.REQUEST_PORT_KEY).set(port);
                }
                logger.info("handle https request, get host: {}, port: {}", host, port);
                createConnectionDirectly(ctx, msg, host, port);
            } else {
                logger.error("unknown msg type. {} close current channel.", msg.getClass().getName());
                ReferenceCountedUtils.release(msg);
                ctx.close();
            }

        }


//        super.channelRead(ctx, msg);
    }

    private void createConnectionDirectly(ChannelHandlerContext ctx, Object msg, String host, int port) {
        Channel inboundChannel = ctx.channel();
        Bootstrap httpsBootstrap = new Bootstrap();
        httpsBootstrap.group(inboundChannel.eventLoop())
                .channel(inboundChannel.getClass())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("readTimeout", new ReadTimeoutHandler(BizConstants.MAX_IDLE_TIMEOUT));
                        pipeline.addLast(new ProxyRelayHandler(inboundChannel));
                    }
                });
        ChannelFuture cf = httpsBootstrap.connect(host, port);
        cf.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                Channel outboundChannel = future.channel();
                outboundChannel.writeAndFlush(msg);
                ctx.pipeline().remove(this);
                ctx.pipeline().addLast(new ProxyRelayHandler(future.channel()));
            } else {
                logger.error("[https] can not connect to remote server! host: {}," +
                        " port: {}, error: {}", host, port, future.cause().getMessage());
                ReferenceCountedUtils.release(msg);
                ctx.close();
            }
        });
    }


    private void createConnection(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, String host, Integer port, String urlStr) {
        logger.info("create a new channel,connect to host:{},port :{}", host, port);
        Bootstrap bootstrap = new Bootstrap();
        Channel channel = ctx.channel();
        bootstrap.group(channel.eventLoop())
                .channel(channel.getClass())
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast("readTimeout", new ReadTimeoutHandler(BizConstants.MAX_IDLE_TIMEOUT));
                        RequestUtils.handleHttpRequest(socketChannel, pipeline, fullHttpRequest);
                        pipeline.addLast("codec", new HttpClientCodec());
                        pipeline.addLast("aggregator", new HttpObjectAggregator(Integer.MAX_VALUE));
                        pipeline.addLast(BizUtils.getCustomWorkerGroup(), "handler",
                                new ProxyClientHandler(channel, urlStr));
                    }
                });


        ChannelTimeoutManager timeoutManager = SpringContextManager.getBean(ChannelTimeoutManager.class);
        ChannelFuture channelFuture = bootstrap.connect(host, port);
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                timeoutManager.putChannel(channel);
                future.channel().writeAndFlush(fullHttpRequest);
                logger.info("send reqeust to remote server,host:{},port:{}", host, port);
            } else {
                logger.info("cann't connect to remote server,host:{},port:{},error:{}", host, port, future.cause().getMessage());
                ReferenceCountedUtils.release(fullHttpRequest);
                ctx.close();

            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if(!(cause instanceof TimeoutException)){
            if (StringUtils.containsIgnoreCase(cause.getMessage(), "Connection reset by peer")) {
                logger.error(cause.getMessage());
            } else {
                logger.error(cause.getMessage(), cause);
            }
        }
        ctx.close();
        MDC.clear();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("channel inactivate. client host: {}", ctx.channel()
                .attr(ChannelAttributeKeyConstants.CLIENT_IP_ATTRIBUTE_KEY).get());
        super.channelInactive(ctx);
        MDC.clear();
    }


    public static void main(String[] args) throws Exception {
        FullHttpRequest fullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://localhost:8080/ok");
        String host = new URL(fullHttpRequest.uri()).getHost();
        System.out.println(host);


    }
}

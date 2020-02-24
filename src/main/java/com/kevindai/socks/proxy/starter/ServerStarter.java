package com.kevindai.socks.proxy.starter;

import com.kevindai.socks.proxy.manager.ChannelManager;
import com.kevindai.socks.proxy.manager.ChannelTimeoutManager;
import com.kevindai.socks.proxy.server.ProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @Author daiwenkai
 * @Date 19/02/2020 17:12
 **/
@Service
public class ServerStarter {
    private static final Logger logger = LoggerFactory.getLogger(ServerStarter.class);

    @Autowired
    private ProxyServer proxyServer;


    @Autowired
    private ChannelTimeoutManager channelTimeoutManager;
    @Autowired
    private ChannelManager channelManager;

    private static volatile Boolean flag = false;


    @PostConstruct
    public void start() {

        try {
            this.proxyServer.start();
            this.proxyServer.sync();
        } catch (Exception e) {
            logger.error("proxy server start failed! ", e);
            System.exit(1);
        }
    }

    @PreDestroy
    public void destory() {
        if(!flag){
            flag  = true;
            channelTimeoutManager.destory();
            channelManager.close();
        }

    }

}

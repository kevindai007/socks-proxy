package com.kevindai.socks.proxy.manager;

import com.google.common.collect.Lists;
import com.kevindai.socks.proxy.util.BizUtils;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author daiwenkai
 * @Date 19/02/2020 16:27
 **/
@Slf4j
@Component
public class ChannelManager {

    @Value("${filter.webhosts}")
    private String filterHost;

    private List<String> filterHosts = new ArrayList<>();

    @PostConstruct
    public void start() {
        if (StringUtils.isNotBlank(filterHost)) {
            log.info("found filter host : {}", filterHost);
            filterHosts = Lists.newArrayList(filterHost.split(";"));
        }
    }


    public boolean checkFilterHost(String host) {
        if (CollectionUtils.isNotEmpty(filterHosts) && StringUtils.isNotBlank(host)) {
            for (String s : filterHosts) {
                if (StringUtils.containsIgnoreCase(s, host)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void closeChannelQuietlyAsync(Channel channel) {
        if (channel != null) {
            try {
                channel.close().syncUninterruptibly();
            } catch (Exception e) {

            }
        }
    }

    public void close() {
        try{
            BizUtils.getChannelGroup().forEach(this::closeChannelQuietlyAsync);
            log.info("close all client channel");
            BizUtils.getRemoteChannelGroup().forEach(this::closeChannelQuietlyAsync);
            log.info("close all remote channel");
        }catch (Exception e){
            log.error("close channel error,{}",e);
        }



    }
}

package com.kevindai.socks.proxy.manager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kevindai.socks.proxy.constants.ChannelAttributeKeyConstants;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @Author daiwenkai
 * @Date 20/02/2020 17:49
 **/
@Slf4j
@Component
public class ChannelTimeoutManager implements InitializingBean {
    public static final int CHANNEL_CACHE_SIZE = 500_000;
    public static final int MONITOR_DURATION = 10;
    private ScheduledExecutorService scheduledExecutorService;

    private Cache<String, Channel> httpTimeoutChannelCache = CacheBuilder.newBuilder().maximumSize(CHANNEL_CACHE_SIZE).build();

    public void putChannel(Channel channel) {
        if (channel != null && channel.isActive()) {
            httpTimeoutChannelCache.put(channel.id().asLongText(), channel);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            private int count = 0;
            private String prefix = "channelTimeoutMonitor";

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, prefix + "-" + count++);
            }
        });

        scheduledExecutorService.scheduleWithFixedDelay(new TimeoutChannelMonitor(), MONITOR_DURATION, MONITOR_DURATION, TimeUnit.SECONDS);
        log.info("init monitor timeout channel");
    }

    public void destory() {
        scheduledExecutorService.shutdown();
        try {
            scheduledExecutorService.awaitTermination(MONITOR_DURATION, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("scheduledExecutorService close error,{}", e);
        }
        httpTimeoutChannelCache.invalidateAll();
        log.info("close timeout channel monitor pool and cache");
    }

    class TimeoutChannelMonitor implements Runnable {

        @Override
        public void run() {
            Map<String, Channel> timeoutChannelMap = new HashMap<>(httpTimeoutChannelCache.asMap());
            if (MapUtils.isNotEmpty(timeoutChannelMap)) {
                HashMap<String, Channel> deleteMap = new HashMap<>();

                for (Map.Entry<String, Channel> entry : timeoutChannelMap.entrySet()) {
                    String channelId = entry.getKey();
                    Channel channel = entry.getValue();
                    if (channel == null || channel.isActive()) {
                        deleteMap.put(channelId, channel);
                    } else {
                        String host = channel.attr(ChannelAttributeKeyConstants.REQUEST_HOST_KEY).get();
                        if (StringUtils.isBlank(host)) {
                            deleteMap.put(channelId, channel);
                        }
                    }
                }

                if (MapUtils.isNotEmpty(deleteMap)) {
                    log.warn("found timeout channel,size:{}", deleteMap.size());
                    httpTimeoutChannelCache.invalidateAll(deleteMap.keySet());
                }
            }


        }
    }
}



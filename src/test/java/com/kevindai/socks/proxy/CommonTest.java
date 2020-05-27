package com.kevindai.socks.proxy;

import com.kevindai.socks.proxy.util.HttpUtil;
import com.kevindai.socks.proxy.util.SpeedLimitedThreadPoolUtil;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @Author: xm20200119
 * @Date: 27/05/2020 09:54
 */
public class CommonTest {
    @Test
    public void testProxy() throws Exception {
        httpLogSetting();
        AtomicInteger successCount = new AtomicInteger();
        CloseableHttpClient client = HttpClients.createDefault();
        SpeedLimitedThreadPoolUtil speedLimitedThreadPoolUtil = new SpeedLimitedThreadPoolUtil();

        for (int i = 0; i < 1000; i++) {
            speedLimitedThreadPoolUtil.submit(() -> {
                HttpGet get = new HttpGet("https://www.baidu.com");

                HttpHost proxy = new HttpHost("127.0.0.1", 53030);
                RequestConfig requestConfig = RequestConfig.custom().setProxy(proxy).build();

                get.setConfig(requestConfig);
                CloseableHttpResponse execute = null;
                try {
                    execute = client.execute(get);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (execute.getStatusLine().getStatusCode() == 200) {
                    successCount.getAndIncrement();
                }
                String s = HttpUtil.gainFromResponse(execute);
            });
        }

        System.out.println(successCount);
    }


    public void httpLogSetting(){
        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");
    }
}

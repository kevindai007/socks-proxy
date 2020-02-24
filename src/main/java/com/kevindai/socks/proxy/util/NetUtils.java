package com.kevindai.socks.proxy.util;

import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @Author daiwenkai
 * @Date 20/02/2020 16:11
 **/
public class NetUtils {
    public static final Logger LOGGER = LoggerFactory.getLogger(NetUtils.class);

    public static String getWebHost(HttpRequest request) {
        try {
            return new URL(request.getUri()).getHost();
        } catch (MalformedURLException e) {
            LOGGER.error("parse host error,{}", e.getMessage());
        }
        return "";
    }
}

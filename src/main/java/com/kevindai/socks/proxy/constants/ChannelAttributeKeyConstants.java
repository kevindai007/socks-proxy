package com.kevindai.socks.proxy.constants;

import io.netty.util.AttributeKey;

/**
 * @Author daiwenkai
 * @Date 19/02/2020 10:23
 **/
public class ChannelAttributeKeyConstants {
    public static final AttributeKey<String> CLIENT_IP_ATTRIBUTE_KEY = AttributeKey.valueOf("clientIp");
    public static final AttributeKey<String> REQUEST_HOST_KEY = AttributeKey.valueOf("host");
    public static final AttributeKey<Integer> REQUEST_PORT_KEY = AttributeKey.valueOf("port");
}

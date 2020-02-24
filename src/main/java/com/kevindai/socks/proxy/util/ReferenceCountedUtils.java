package com.kevindai.socks.proxy.util;

import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author daiwenkai
 * @Date 19/02/2020 17:02
 **/
public class ReferenceCountedUtils {
    public static final Logger logger = LoggerFactory.getLogger(ReferenceCountedUtils.class);

    public static void release(Object msg) {
        try {
            if (msg instanceof ReferenceCounted) {
                ReferenceCounted counted = (ReferenceCounted) msg;
                int count = counted.refCnt();
                if (count != 0) {
                    ReferenceCountUtil.release(msg, count);
                }

            }
        } catch (Exception e) {
            logger.error("release msg failed. {}", e);
        }

    }
}

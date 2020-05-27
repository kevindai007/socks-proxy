package com.kevindai.socks.proxy.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author: xm20200119
 * @Date: 27/05/2020 10:37
 */
public class SpeedLimitedThreadPoolUtil {
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 8, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(12));
    private Semaphore semaphore = new Semaphore(10);

    public void submit(Runnable r) {
        try {
            semaphore.acquire();
            executor.submit(() -> {
                try{
                    r.run();
                }finally {
                    semaphore.release();
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
            semaphore.release();
        }

    }
}

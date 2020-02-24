package com.kevindai.socks.proxy.exception;

/**
 * @Author daiwenkai
 * @Date 21/02/2020 14:39
 **/
public class SslHandShakeException extends RuntimeException{
    public SslHandShakeException() {
    }

    public SslHandShakeException(String message) {
        super(message);
    }

    public SslHandShakeException(String message, Throwable cause) {
        super(message, cause);
    }

    public SslHandShakeException(Throwable cause) {
        super(cause);
    }
}

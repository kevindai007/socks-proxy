package com.kevindai.socks.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SocksProxyApplication {

	public static void main(String[] args) {
		System.out.println("test1");
		SpringApplication.run(SocksProxyApplication.class, args);
	}

}

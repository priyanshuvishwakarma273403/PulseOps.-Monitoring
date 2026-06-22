package com.pulseops.websocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.pulseops"})
public class WebSocketServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebSocketServiceApplication.class, args);
    }
}

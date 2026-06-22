package com.pulseops.websocket.config;

import com.pulseops.websocket.handler.RealtimeDashboardHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RealtimeDashboardHandler dashboardHandler;

    public WebSocketConfig(RealtimeDashboardHandler dashboardHandler) {
        this.dashboardHandler = dashboardHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(dashboardHandler, "/ws/dashboard")
                .setAllowedOrigins("*");
    }
}

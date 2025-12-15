// src/main/java/com/intelligentcar/config/WebSocketConfig.java
package com.intelligentcar.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import com.intelligentcar.websocket.CarCommandHandler;
import com.intelligentcar.websocket.CarStatusHandler;
import com.intelligentcar.websocket.VideoStreamHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    // 注入CarControlService
    @Autowired
    private com.intelligentcar.service.CarControlService carControlService;

    // 注入VideoStreamHandler
    @Autowired
    private VideoStreamHandler videoStreamHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 小车控制WebSocket
        registry.addHandler(carCommandHandler(), "/ws/control")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOriginPatterns("*");

        // 小车状态WebSocket
        registry.addHandler(carStatusHandler(), "/ws/status")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOriginPatterns("*");

        // 视频流WebSocket
        registry.addHandler(videoStreamHandler, "/ws/video")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOriginPatterns("*");
    }

    // 声明CarCommandHandler为Bean
    @Bean
    public CarCommandHandler carCommandHandler() {
        return new CarCommandHandler(carControlService);
    }

    // 声明CarStatusHandler为Bean
    @Bean
    public CarStatusHandler carStatusHandler() {
        return new CarStatusHandler(carControlService);
    }
}
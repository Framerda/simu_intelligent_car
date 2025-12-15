// src/main/java/com/intelligentcar/websocket/CarStatusHandler.java
package com.intelligentcar.websocket;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.intelligentcar.service.CarControlService;
import com.intelligentcar.model.CarStatus;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class CarStatusHandler extends TextWebSocketHandler {

    private static final Map<String, WebSocketSession> statusSessions = new ConcurrentHashMap<>();
    private final CarControlService carControlService;
    private final ObjectMapper objectMapper;

    public CarStatusHandler(CarControlService carControlService) {
        this.carControlService = carControlService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        statusSessions.put(sessionId, session);

        System.out.println("新的状态监听连接建立: " + sessionId);

        // 发送欢迎消息 - 使用 ObjectMapper
        Map<String, Object> welcomeMsg = new HashMap<>();
        welcomeMsg.put("type", "STATUS_WELCOME");
        welcomeMsg.put("message", "已连接到状态更新服务");
        welcomeMsg.put("timestamp", LocalDateTime.now());

        String welcomeJson = objectMapper.writeValueAsString(welcomeMsg);
        session.sendMessage(new TextMessage(welcomeJson));

        // 发送初始状态
        sendStatusUpdate(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("状态监听消息: " + payload);

        if ("GET_STATUS".equals(payload)) {
            sendStatusUpdate(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        String sessionId = session.getId();
        statusSessions.remove(sessionId);
        System.out.println("状态监听连接关闭: " + sessionId);
    }

    /**
     * 发送状态更新
     */
    private void sendStatusUpdate(WebSocketSession session) throws IOException {
        CarStatus status = carControlService.getCurrentStatus();
        Map<String, Object> statusMap = status.toMap();
        statusMap.put("type", "STATUS_UPDATE");
        statusMap.put("timestamp", LocalDateTime.now());

        String statusJson = objectMapper.writeValueAsString(statusMap);
        session.sendMessage(new TextMessage(statusJson));
    }

    /**
     * 广播状态更新给所有监听客户端
     */
    public void broadcastStatus() {
        try {
            CarStatus status = carControlService.getCurrentStatus();
            Map<String, Object> statusMap = status.toMap();
            statusMap.put("type", "STATUS_BROADCAST");
            statusMap.put("timestamp", LocalDateTime.now());
            statusMap.put("activeListeners", statusSessions.size());

            String statusJson = objectMapper.writeValueAsString(statusMap);
            TextMessage message = new TextMessage(statusJson);

            for (WebSocketSession session : statusSessions.values()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        System.err.println("发送状态更新失败: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("准备状态更新失败: " + e.getMessage());
        }
    }

    /**
     * 获取状态监听连接数
     */
    public int getStatusListenerCount() {
        return statusSessions.size();
    }
}
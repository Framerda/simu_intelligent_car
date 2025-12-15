// src/main/java/com/intelligentcar/websocket/CarCommandHandler.java
package com.intelligentcar.websocket;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.intelligentcar.model.ControlCommand;
import com.intelligentcar.service.CarControlService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class CarCommandHandler extends TextWebSocketHandler {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final CarControlService carControlService;
    private final ObjectMapper objectMapper;

    public CarCommandHandler(CarControlService carControlService) {
        this.carControlService = carControlService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        carControlService.registerControlSession(sessionId, session);

        System.out.println("新的控制连接建立: " + sessionId + ", IP: " + session.getRemoteAddress());

        // 发送欢迎消息 - 使用 ObjectMapper 构建
        Map<String, Object> welcomeMsg = new HashMap<>();
        welcomeMsg.put("type", "WELCOME");
        welcomeMsg.put("message", "已连接到智能小车控制系统");
        welcomeMsg.put("sessionId", sessionId);
        welcomeMsg.put("timestamp", LocalDateTime.now());

        String welcomeJson = objectMapper.writeValueAsString(welcomeMsg);
        session.sendMessage(new TextMessage(welcomeJson));

        // 发送当前状态
        sendCurrentStatus(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload().trim(); // 注意：增加了 trim()

        System.out.println("收到控制消息[" + sessionId + "]: " + payload);

        // === 新增：先处理纯文本指令 ===
        if ("GET_STATUS".equals(payload)) {
            System.out.println("接收到状态查询请求，发送当前状态");
            sendCurrentStatus(session);
            return; // 处理完成，直接返回
        }
        // === 处理结束 ===

        try {
            // 解析控制命令 (期望是JSON)
            ControlCommand command = objectMapper.readValue(payload, ControlCommand.class);
            command.setSessionId(sessionId);

            if (!command.isValid()) {
                sendError(session, "无效的命令: " + command.getCommand());
                return;
            }

            // 执行命令
            carControlService.executeCommand(command.getCommand(), command.getValue());

            // 发送确认消息
            Map<String, Object> ackMsg = new HashMap<>();
            ackMsg.put("type", "ACK");
            ackMsg.put("command", command.getCommand());
            ackMsg.put("status", "EXECUTED");
            ackMsg.put("timestamp", System.currentTimeMillis());

            String ackJson = objectMapper.writeValueAsString(ackMsg);
            session.sendMessage(new TextMessage(ackJson));

            // 广播状态更新
            broadcastStatusUpdate();

        } catch (Exception e) {
            System.err.println("处理控制消息失败: " + e.getMessage());
            // 更友好的错误提示：说明期望的格式
            if (e.getMessage().contains("Unrecognized token")) {
                sendError(session, "消息格式错误，请发送JSON格式的控制命令。收到: " + payload);
            } else {
                sendError(session, "命令处理失败: " + e.getMessage());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        carControlService.removeControlSession(sessionId);

        System.out.println("控制连接关闭: " + sessionId + ", 原因: " + status.getReason() + ", 代码: " + status.getCode());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("传输错误: " + session.getId() + " - " + exception.getMessage());
        session.close();
    }

    /**
     * 发送当前状态
     */
    private void sendCurrentStatus(WebSocketSession session) throws IOException {
        Map<String, Object> status = carControlService.getCurrentStatus().toMap();
        status.put("type", "STATUS_UPDATE");
        status.put("activeConnections", sessions.size());

        String statusJson = objectMapper.writeValueAsString(status);
        session.sendMessage(new TextMessage(statusJson));
    }

    /**
     * 广播状态更新
     */
    private void broadcastStatusUpdate() throws IOException {
        Map<String, Object> status = carControlService.getCurrentStatus().toMap();
        status.put("type", "BROADCAST_STATUS");
        status.put("timestamp", LocalDateTime.now());

        String statusJson = objectMapper.writeValueAsString(status);
        TextMessage message = new TextMessage(statusJson);

        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                session.sendMessage(message);
            }
        }
    }

    /**
     * 发送错误消息
     */
    private void sendError(WebSocketSession session, String errorMessage) throws IOException {
        // 使用 ObjectMapper 构建错误消息
        Map<String, Object> errorMsg = new HashMap<>();
        errorMsg.put("type", "ERROR");
        errorMsg.put("message", errorMessage);
        errorMsg.put("timestamp", LocalDateTime.now());

        String errorJson = objectMapper.writeValueAsString(errorMsg);
        session.sendMessage(new TextMessage(errorJson));
    }

    /**
     * 获取活动连接数
     */
    public int getActiveConnections() {
        return sessions.size();
    }
}
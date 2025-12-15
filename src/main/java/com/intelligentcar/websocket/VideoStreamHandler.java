// src/main/java/com/intelligentcar/websocket/VideoStreamHandler.java
package com.intelligentcar.websocket;

import com.intelligentcar.service.SimulatedVideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class VideoStreamHandler extends TextWebSocketHandler {

    @Autowired
    private SimulatedVideoService videoService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final ConcurrentHashMap<String, AtomicBoolean> sessionStreamingMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("视频WebSocket连接建立: " + session.getId());

        // 为每个会话创建一个流状态
        sessionStreamingMap.put(session.getId(), new AtomicBoolean(false));

        // 发送欢迎消息
        session.sendMessage(new TextMessage(
                "{\"type\":\"welcome\",\"message\":\"视频流连接已建立\",\"sessionId\":\"" + session.getId() + "\"}"
        ));
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        // 处理控制命令
        if ("start".equals(payload)) {
            startSendingFrames(session);
            session.sendMessage(new TextMessage("{\"type\":\"control\",\"status\":\"streaming_started\"}"));
        } else if ("stop".equals(payload)) {
            stopSendingFrames(session);
            session.sendMessage(new TextMessage("{\"type\":\"control\",\"status\":\"streaming_stopped\"}"));
        } else if (payload.startsWith("fps:")) {
            // 可以添加调整帧率的逻辑
            session.sendMessage(new TextMessage("{\"type\":\"control\",\"message\":\"FPS调整命令已接收\"}"));
        } else if ("ping".equals(payload)) {
            // 心跳响应
            session.sendMessage(new TextMessage("{\"type\":\"pong\",\"timestamp\":" + System.currentTimeMillis() + "}"));
        }
    }

    private void startSendingFrames(WebSocketSession session) {
        AtomicBoolean isStreaming = sessionStreamingMap.get(session.getId());
        if (isStreaming != null && !isStreaming.get()) {
            isStreaming.set(true);

            scheduler.scheduleAtFixedRate(() -> {
                if (isStreaming.get() && session.isOpen()) {
                    try {
                        // 生成带传感器数据的视频帧
                        String frameData = videoService.generateFrameWithSensorData();
                        session.sendMessage(new TextMessage(frameData));
                    } catch (IOException e) {
                        System.err.println("发送视频帧失败: " + e.getMessage());
                        isStreaming.set(false);
                    }
                }
            }, 0, 66, TimeUnit.MILLISECONDS); // 约15fps
        }
    }

    private void stopSendingFrames(WebSocketSession session) {
        AtomicBoolean isStreaming = sessionStreamingMap.get(session.getId());
        if (isStreaming != null) {
            isStreaming.set(false);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("视频WebSocket连接关闭: " + session.getId());
        stopSendingFrames(session);
        sessionStreamingMap.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("视频WebSocket传输错误: " + exception.getMessage());
        stopSendingFrames(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
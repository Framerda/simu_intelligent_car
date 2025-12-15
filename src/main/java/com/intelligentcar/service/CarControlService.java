// src/main/java/com/intelligentcar/service/CarControlService.java
package com.intelligentcar.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.intelligentcar.model.CarStatus;
import com.intelligentcar.websocket.CarCommandHandler;
import com.intelligentcar.model.ControlCommand;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class CarControlService {

    private CarStatus currentStatus;
    private Map<String, WebSocketSession> controlSessions = new ConcurrentHashMap<>();
    private boolean isConnected = false;
    private LocalDateTime lastConnectionTime;

    public CarControlService() {
        // 初始化小车状态
        this.currentStatus = new CarStatus();
        this.currentStatus.setSpeed(0);
        this.currentStatus.setDirection("STOP");
        this.currentStatus.setFrontDistance(0);
        this.currentStatus.setLeftDistance(0);
        this.currentStatus.setRightDistance(0);
        this.currentStatus.setBatteryLevel(100);
        this.currentStatus.setTimestamp(LocalDateTime.now());
    }

    /**
     * 注册控制会话
     */
    public void registerControlSession(String sessionId, WebSocketSession session) {
        controlSessions.put(sessionId, session);
        isConnected = true;
        lastConnectionTime = LocalDateTime.now();
    }

    /**
     * 移除控制会话
     */
    public void removeControlSession(String sessionId) {
        controlSessions.remove(sessionId);
        if (controlSessions.isEmpty()) {
            isConnected = false;
        }
    }

    /**
     * 执行控制命令
     */
    public void executeCommand(String command, String value) {
        ControlCommand controlCommand = new ControlCommand();
        controlCommand.setCommand(command);
        controlCommand.setValue(value);

        // 更新小车状态
        updateCarStatus(controlCommand);

        // 通过WebSocket发送给所有连接的客户端（包括硬件）
        sendCommandToAll(controlCommand);
    }

    /**
     * 紧急停止
     */
    public void emergencyStop() {
        ControlCommand emergencyCommand = new ControlCommand();
        emergencyCommand.setCommand("EMERGENCY_STOP");

        // 更新状态
        currentStatus.setSpeed(0);
        currentStatus.setDirection("STOP");

        // 发送紧急停止命令
        sendCommandToAll(emergencyCommand);
    }

    /**
     * 更新小车状态
     */
    private void updateCarStatus(ControlCommand command) {
        switch (command.getCommand().toUpperCase()) {
            case "FORWARD":
                currentStatus.setDirection("FORWARD");
                currentStatus.setSpeed(50); // 默认速度值
                break;
            case "BACKWARD":
                currentStatus.setDirection("BACKWARD");
                currentStatus.setSpeed(30);
                break;
            case "LEFT":
                currentStatus.setDirection("LEFT");
                break;
            case "RIGHT":
                currentStatus.setDirection("RIGHT");
                break;
            case "STOP":
                currentStatus.setDirection("STOP");
                currentStatus.setSpeed(0);
                break;
            case "SPEED":
                if (command.getValue() != null) {
                    currentStatus.setSpeed(Integer.parseInt(command.getValue()));
                }
                break;
        }
        currentStatus.setTimestamp(LocalDateTime.now());
    }

    /**
     * 发送命令给所有连接
     */
    private void sendCommandToAll(ControlCommand command) {
        TextMessage message = new TextMessage(command.toJson());
        for (WebSocketSession session : controlSessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    System.err.println("发送命令失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 获取当前状态
     */
    public CarStatus getCurrentStatus() {
        return currentStatus;
    }

    /**
     * 更新传感器数据
     */
    public void updateSensorData(int frontDistance, int leftDistance, int rightDistance) {
        currentStatus.setFrontDistance(frontDistance);
        currentStatus.setLeftDistance(leftDistance);
        currentStatus.setRightDistance(rightDistance);
        currentStatus.setTimestamp(LocalDateTime.now());
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * 获取活动连接数
     */
    public int getActiveConnections() {
        return controlSessions.size();
    }
}
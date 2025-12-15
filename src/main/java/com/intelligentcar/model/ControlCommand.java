// src/main/java/com/intelligentcar/model/ControlCommand.java
package com.intelligentcar.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class ControlCommand {

    // 命令类型
    private String command;  // FORWARD, BACKWARD, LEFT, RIGHT, STOP, SPEED, EMERGENCY_STOP
    private String value;    // 命令值（如速度值）

    // 来源信息
    private String source;   // WEB, MOBILE, AUTOMATIC
    private String sessionId;// 会话ID

    // 时间戳 - 改为 Long 类型以兼容前端毫秒时间戳
    private Long timestamp;

    // 构造方法
    public ControlCommand() {
        this.timestamp = System.currentTimeMillis();  // 使用当前毫秒时间戳
        this.source = "WEB";
    }

    // Getter和Setter方法
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    /**
     * 获取 LocalDateTime 类型的时间（可选，用于需要时间格式的场合）
     */
    @JsonIgnore  // 不序列化到JSON，避免重复
    public LocalDateTime getLocalDateTime() {
        if (this.timestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(this.timestamp),
                ZoneId.systemDefault()
        );
    }

    /**
     * 转换为JSON字符串
     */
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 从JSON字符串解析
     */
    public static ControlCommand fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.readValue(json, ControlCommand.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 转换为Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("command", command);
        map.put("value", value);
        map.put("source", source);
        map.put("sessionId", sessionId);
        map.put("timestamp", timestamp);
        map.put("localDateTime", getLocalDateTime());  // 添加转换后的时间
        return map;
    }

    /**
     * 验证命令是否有效
     */
    public boolean isValid() {
        if (command == null || command.trim().isEmpty()) {
            return false;
        }

        String cmd = command.toUpperCase();
        return cmd.equals("FORWARD") || cmd.equals("BACKWARD") ||
                cmd.equals("LEFT") || cmd.equals("RIGHT") ||
                cmd.equals("STOP") || cmd.equals("SPEED") ||
                cmd.equals("EMERGENCY_STOP");
    }

    @Override
    public String toString() {
        return String.format("ControlCommand{command='%s', value='%s', source='%s', time=%s}",
                command, value, source, getLocalDateTime());
    }
}
// src/main/java/com/intelligentcar/model/CarStatus.java
package com.intelligentcar.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class CarStatus {

    // 运动状态
    private Integer speed;          // 速度（0-100）
    private String direction;       // 方向：FORWARD, BACKWARD, LEFT, RIGHT, STOP
    private Boolean isMoving;       // 是否在移动

    // 传感器数据
    private Integer frontDistance;  // 前方距离（厘米）
    private Integer leftDistance;   // 左侧距离（厘米）
    private Integer rightDistance;  // 右侧距离（厘米）

    // 系统状态
    private Integer batteryLevel;   // 电池电量（0-100）
    private Double cpuTemperature;  // CPU温度
    private String wifiSignal;      // WiFi信号强度

    // 时间戳
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    // 构造方法
    public CarStatus() {
        this.speed = 0;
        this.direction = "STOP";
        this.isMoving = false;
        this.frontDistance = 0;
        this.leftDistance = 0;
        this.rightDistance = 0;
        this.batteryLevel = 100;
        this.cpuTemperature = 25.0;
        this.wifiSignal = "强";
        this.timestamp = LocalDateTime.now();
    }

    // Getter和Setter方法
    public Integer getSpeed() { return speed; }
    public void setSpeed(Integer speed) {
        this.speed = speed;
        this.isMoving = speed > 0;
    }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public Boolean getIsMoving() { return isMoving; }
    public void setIsMoving(Boolean isMoving) { this.isMoving = isMoving; }

    public Integer getFrontDistance() { return frontDistance; }
    public void setFrontDistance(Integer frontDistance) { this.frontDistance = frontDistance; }

    public Integer getLeftDistance() { return leftDistance; }
    public void setLeftDistance(Integer leftDistance) { this.leftDistance = leftDistance; }

    public Integer getRightDistance() { return rightDistance; }
    public void setRightDistance(Integer rightDistance) { this.rightDistance = rightDistance; }

    public Integer getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }

    public Double getCpuTemperature() { return cpuTemperature; }
    public void setCpuTemperature(Double cpuTemperature) { this.cpuTemperature = cpuTemperature; }

    public String getWifiSignal() { return wifiSignal; }
    public void setWifiSignal(String wifiSignal) { this.wifiSignal = wifiSignal; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

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
     * 转换为Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("speed", speed);
        map.put("direction", direction);
        map.put("isMoving", isMoving);
        map.put("frontDistance", frontDistance);
        map.put("leftDistance", leftDistance);
        map.put("rightDistance", rightDistance);
        map.put("batteryLevel", batteryLevel);
        map.put("cpuTemperature", cpuTemperature);
        map.put("wifiSignal", wifiSignal);
        map.put("timestamp", timestamp);
        return map;
    }

    @Override
    public String toString() {
        return String.format("CarStatus{speed=%d, direction='%s', front=%dcm, left=%dcm, right=%dcm, battery=%d%%}",
                speed, direction, frontDistance, leftDistance, rightDistance, batteryLevel);
    }
}
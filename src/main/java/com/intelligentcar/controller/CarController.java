// src/main/java/com/intelligentcar/controller/CarController.java
package com.intelligentcar.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import com.intelligentcar.model.CarStatus;
import com.intelligentcar.service.CarControlService;

@RestController
@RequestMapping("/api/car")
public class CarController {

    private final CarControlService carControlService;

    public CarController(CarControlService carControlService) {
        this.carControlService = carControlService;
    }

    /**
     * 获取小车状态
     */
    @GetMapping("/status")
    public ResponseEntity<CarStatus> getCarStatus() {
        CarStatus status = carControlService.getCurrentStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * 发送控制命令
     */
    @PostMapping("/control")
    public ResponseEntity<String> controlCar(@RequestParam String command,
                                             @RequestParam(required = false) String value) {
        try {
            carControlService.executeCommand(command, value);
            return ResponseEntity.ok("命令执行成功: " + command);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("命令执行失败: " + e.getMessage());
        }
    }

    /**
     * 紧急停止
     */
    @PostMapping("/emergency-stop")
    public ResponseEntity<String> emergencyStop() {
        carControlService.emergencyStop();
        return ResponseEntity.ok("紧急停止指令已发送");
    }

    /**
     * 获取连接状态
     */
    @GetMapping("/connection-status")
    public ResponseEntity<String> getConnectionStatus() {
        boolean isConnected = carControlService.isConnected();
        return ResponseEntity.ok(isConnected ? "已连接" : "未连接");
    }
}
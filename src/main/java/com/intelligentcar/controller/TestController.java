// src/main/java/com/intelligentcar/controller/TestController.java
package com.intelligentcar.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
public class TestController {

    /**
     * 测试接口 - 健康检查
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("智能小车Web控制平台运行正常！");
    }

    /**
     * 系统信息接口
     */
    @GetMapping("/api/system/info")
    public ResponseEntity<SystemInfo> getSystemInfo() {
        SystemInfo info = new SystemInfo();
        info.setAppName("智能小车控制平台");
        info.setVersion("1.0.0");
        info.setStatus("运行中");
        info.setTimestamp(java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(info);
    }

    // 内部类用于返回系统信息
    static class SystemInfo {
        private String appName;
        private String version;
        private String status;
        private String timestamp;

        // Getter和Setter
        public String getAppName() { return appName; }
        public void setAppName(String appName) { this.appName = appName; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
}
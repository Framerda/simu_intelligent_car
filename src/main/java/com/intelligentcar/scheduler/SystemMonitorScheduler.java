// src/main/java/com/intelligentcar/scheduler/SystemMonitorScheduler.java
package com.intelligentcar.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 系统监控定时任务
 * 用于定期检查系统状态、清理临时文件等
 */
@Component
public class SystemMonitorScheduler {

    private final AtomicInteger heartbeatCounter = new AtomicInteger(0);

    /**
     * 心跳检测 - 每30秒执行一次
     */
    @Scheduled(fixedRate = 30000)
    public void heartbeat() {
        int count = heartbeatCounter.incrementAndGet();
        System.out.println("[" + LocalDateTime.now() + "] 系统心跳 #" + count + " - 应用程序运行正常");
    }

    /**
     * 系统状态监控 - 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000)
    public void monitorSystemStatus() {
        try {
            Runtime runtime = Runtime.getRuntime();

            // 获取内存使用情况
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();

            double memoryUsage = (double) usedMemory / totalMemory * 100;

            // 获取CPU核心数
            int availableProcessors = runtime.availableProcessors();

            // 获取操作系统信息
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double systemLoad = osBean.getSystemLoadAverage();

            System.out.println("[" + LocalDateTime.now() + "] 系统状态监控:");
            System.out.println("  - 内存使用: " + String.format("%.2f", memoryUsage) + "%");
            System.out.println("  - 已使用内存: " + (usedMemory / (1024 * 1024)) + "MB");
            System.out.println("  - 总内存: " + (totalMemory / (1024 * 1024)) + "MB");
            System.out.println("  - 最大内存: " + (maxMemory / (1024 * 1024)) + "MB");
            System.out.println("  - CPU核心数: " + availableProcessors);
            System.out.println("  - 系统负载: " + systemLoad);

        } catch (Exception e) {
            System.err.println("系统监控异常: " + e.getMessage());
        }
    }

    /**
     * 清理临时文件 - 每10分钟执行一次
     */
    @Scheduled(fixedRate = 600000)
    public void cleanupTempFiles() {
        System.out.println("[" + LocalDateTime.now() + "] 执行临时文件清理...");
        // 这里可以添加清理临时文件的逻辑
    }
}
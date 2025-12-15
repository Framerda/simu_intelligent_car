// src/main/java/com/intelligentcar/StartupCheck.java
package com.intelligentcar;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 应用程序启动检查类
 * 用于在启动时检查必要的资源和配置
 */
@Component
public class StartupCheck implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.out.println("执行启动检查...");

        // 检查必要目录
        checkDirectories();

        // 检查配置文件
        checkConfiguration();

        System.out.println("启动检查完成！");
    }

    private void checkDirectories() {
        String[] directories = {
                "recordings",
                "logs",
                "uploads"
        };

        for (String dir : directories) {
            File directory = new File(dir);
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    System.out.println("创建目录: " + directory.getAbsolutePath());
                } else {
                    System.out.println("无法创建目录: " + directory.getAbsolutePath());
                }
            }
        }
    }

    private void checkConfiguration() {
        System.out.println("检查系统配置...");

        // 检查Java版本
        String javaVersion = System.getProperty("java.version");
        System.out.println("Java版本: " + javaVersion);

        // 检查操作系统
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        System.out.println("操作系统: " + osName + " " + osVersion);

        // 检查可用内存
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        System.out.println("最大内存: " + maxMemory + "MB");
        System.out.println("已分配内存: " + totalMemory + "MB");
        System.out.println("可用内存: " + freeMemory + "MB");
    }
}
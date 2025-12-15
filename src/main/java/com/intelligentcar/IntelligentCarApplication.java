// src/main/java/com/intelligentcar/IntelligentCarApplication.java
package com.intelligentcar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智能小车Web控制平台 - 主启动类
 *
 * 这是一个Spring Boot应用程序的入口点，负责：
 * 1. 启动嵌入式Tomcat服务器
 * 2. 自动扫描和配置所有组件
 * 3. 启用WebSocket和定时任务功能
 *
 * 注解说明：
 * @SpringBootApplication - 组合注解，包含：
 *   - @Configuration: 声明为配置类
 *   - @EnableAutoConfiguration: 启用自动配置
 *   - @ComponentScan: 扫描当前包及其子包中的组件
 * @EnableScheduling - 启用Spring的定时任务功能
 */
@SpringBootApplication
@EnableScheduling
public class IntelligentCarApplication {

    /**
     * 主方法 - 应用程序的入口点
     *
     * @param args 命令行参数，可用于配置应用程序
     */
    public static void main(String[] args) {
        // 打印启动信息
        System.out.println("==============================================");
        System.out.println("  智能小车Web控制平台正在启动...");
        System.out.println("  版本: 1.0.0");
        System.out.println("  开发者: 智能小车项目组");
        System.out.println("  启动时间: " + java.time.LocalDateTime.now());
        System.out.println("==============================================");

        // 启动Spring Boot应用程序
        SpringApplication.run(IntelligentCarApplication.class, args);

        // 打印启动完成信息
        System.out.println("==============================================");
        System.out.println("  智能小车Web控制平台启动成功！");
        System.out.println("  访问地址: http://localhost:8080");
        System.out.println("  WebSocket控制地址: ws://localhost:8080/ws/control");
        System.out.println("  WebSocket状态地址: ws://localhost:8080/ws/status");
        System.out.println("  REST API测试地址: http://localhost:8080/test");
        System.out.println("==============================================");
    }

    /**
     * 自定义启动完成后执行的操作
     *
     * 这个方法会在Spring Boot应用程序完全启动后自动执行，
     * 用于执行初始化任务、预加载数据等操作。
     *
     * 注意：这个方法不是必须的，可以根据需要添加。
     */
    /*
    @Bean
    public CommandLineRunner init() {
        return args -> {
            System.out.println("应用程序初始化完成，准备接收请求...");
            // 可以在这里添加初始化逻辑
            // 例如：创建必要的目录、初始化数据库等
        };
    }
    */
}
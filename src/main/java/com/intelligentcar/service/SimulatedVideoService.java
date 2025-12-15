// src/main/java/com/intelligentcar/service/SimulatedVideoService.java
package com.intelligentcar.service;

import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;

@Service
public class SimulatedVideoService {

    private final Random random = new Random();
    private int frameCount = 0;
    private long lastFrameTime = System.currentTimeMillis();
    private boolean isRecording = false;

    /**
     * 生成模拟视频帧
     */
    public byte[] generateVideoFrame(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 设置背景颜色
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRect(0, 0, width, height);

        // 绘制网格
        g2d.setColor(new Color(100, 100, 100));
        for (int i = 0; i < width; i += 20) {
            g2d.drawLine(i, 0, i, height);
        }
        for (int i = 0; i < height; i += 20) {
            g2d.drawLine(0, i, width, i);
        }

        // 绘制移动的方块（模拟障碍物）
        int blockSize = 40;
        int blockX = (frameCount * 2) % (width - blockSize);
        int blockY = (height - blockSize) / 2 + (int)(Math.sin(frameCount * 0.1) * 50);

        g2d.setColor(new Color(255, 100, 100));
        g2d.fillRect(blockX, blockY, blockSize, blockSize);
        g2d.setColor(Color.WHITE);
        g2d.drawString("障碍物", blockX + 5, blockY + 25);

        // 绘制小车视角
        g2d.setColor(new Color(100, 200, 255));
        int carWidth = 60;
        int carHeight = 40;
        int carX = width / 2 - carWidth / 2;
        int carY = height - carHeight - 20;

        g2d.fillRect(carX, carY, carWidth, carHeight);
        g2d.setColor(Color.WHITE);
        g2d.drawString("智能小车", carX + 10, carY + 25);

        // 绘制距离信息
        g2d.setColor(Color.GREEN);
        int distance = 100 - (frameCount % 100);
        g2d.drawString("前方距离: " + distance + "cm", 20, 30);

        // 绘制速度信息
        int speed = 30 + (frameCount % 40);
        g2d.drawString("当前速度: " + speed + "cm/s", 20, 60);

        // 绘制帧率信息
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastFrameTime;
        int fps = elapsed > 0 ? (int)(1000 / elapsed) : 0;
        lastFrameTime = currentTime;
        g2d.drawString("FPS: " + fps, width - 100, 30);

        // 绘制时间戳
        g2d.setColor(Color.YELLOW);
        g2d.drawString("时间: " + System.currentTimeMillis(), 20, height - 20);

        // 如果是录制状态，添加录制标识
        if (isRecording) {
            g2d.setColor(Color.RED);
            g2d.fillOval(width - 40, 10, 10, 10);
            g2d.setColor(Color.WHITE);
            g2d.drawString("录制中", width - 80, 20);
        }

        g2d.dispose();
        frameCount++;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "JPEG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("生成视频帧失败", e);
        }
    }

    /**
     * 生成base64编码的图像
     */
    public String generateBase64Frame(int width, int height) {
        byte[] imageData = generateVideoFrame(width, height);
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageData);
    }

    /**
     * 生成带传感器的模拟帧（包含距离数据）
     */
    public String generateFrameWithSensorData() {
        int width = 640;
        int height = 480;
        byte[] frame = generateVideoFrame(width, height);
        String base64Frame = Base64.getEncoder().encodeToString(frame);

        // 模拟传感器数据
        double leftDistance = 50 + 20 * Math.sin(frameCount * 0.05);
        double rightDistance = 50 + 20 * Math.cos(frameCount * 0.05);
        double frontDistance = 100 - (frameCount % 100);

        return String.format(
                "{\"type\":\"video_frame\",\"frame\":\"%s\",\"sensors\":{\"left\":%.1f,\"right\":%.1f,\"front\":%.1f}}",
                base64Frame, leftDistance, rightDistance, frontDistance
        );
    }

    /**
     * 获取当前帧计数
     */
    public int getFrameCount() {
        return frameCount;
    }

    /**
     * 开始录制
     */
    public void startRecording() {
        this.isRecording = true;
    }

    /**
     * 停止录制
     */
    public void stopRecording() {
        this.isRecording = false;
    }

    /**
     * 是否正在录制
     */
    public boolean isRecording() {
        return this.isRecording;
    }
}
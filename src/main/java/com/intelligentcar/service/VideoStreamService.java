// src/main/java/com/intelligentcar/service/VideoStreamService.java
package com.intelligentcar.service;

import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class VideoStreamService {

    // 小车视频流的IP地址和端口（需要根据实际情况配置）
    private static final String CAR_VIDEO_URL = "http://192.168.4.1:81/stream";
    private static final String SNAPSHOT_URL = "http://192.168.4.1:80/capture";

    // 录制相关
    private boolean isRecording = false;
    private File recordingFile = null;
    private FileOutputStream recordingStream = null;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * 获取视频流地址
     */
    public String getStreamUrl() {
        return CAR_VIDEO_URL;
    }

    /**
     * 流转发方法（MJPEG流）
     */
    public void streamVideo(HttpServletResponse response) throws IOException {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            URL url = new URL(CAR_VIDEO_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(30000);

            // 设置响应头
            response.setContentType("multipart/x-mixed-replace; boundary=frame");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");
            response.setHeader("Pragma", "no-cache");

            inputStream = connection.getInputStream();
            outputStream = response.getOutputStream();

            byte[] buffer = new byte[1024 * 64]; // 64KB缓冲区
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // 如果正在录制，写入录制文件
                if (isRecording && recordingStream != null) {
                    recordingStream.write(buffer, 0, bytesRead);
                }

                // 转发给客户端
                outputStream.write(buffer, 0, bytesRead);
                outputStream.flush();
            }

        } catch (Exception e) {
            System.err.println("视频流转发失败: " + e.getMessage());
            // 返回错误图片
            returnErrorImage(response);
        } finally {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * 获取静态快照
     */
    public Resource getSnapshot() throws IOException {
        try {
            // 从视频流获取一帧
            URL url = new URL(SNAPSHOT_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);

            // 保存到临时文件
            Path tempFile = Files.createTempFile("snapshot_", ".jpg");
            try (InputStream in = connection.getInputStream();
                 OutputStream out = Files.newOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            return new UrlResource(tempFile.toUri());
        } catch (Exception e) {
            System.err.println("获取快照失败: " + e.getMessage());
            // 返回默认图片
            return getDefaultImage();
        }
    }

    /**
     * 开始录制
     */
    public void startRecording() throws IOException {
        if (!isRecording) {
            String timestamp = LocalDateTime.now().format(dateFormatter);
            String fileName = "recording_" + timestamp + ".mjpeg";
            recordingFile = new File("recordings/" + fileName);

            // 确保目录存在
            recordingFile.getParentFile().mkdirs();

            recordingStream = new FileOutputStream(recordingFile);
            isRecording = true;
            System.out.println("开始录制: " + fileName);
        }
    }

    /**
     * 停止录制
     */
    public void stopRecording() throws IOException {
        if (isRecording && recordingStream != null) {
            recordingStream.close();
            isRecording = false;
            System.out.println("停止录制，文件保存至: " + recordingFile.getAbsolutePath());
        }
    }

    /**
     * 返回错误图片
     */
    private void returnErrorImage(HttpServletResponse response) throws IOException {
        response.setContentType("image/jpeg");
        // 可以返回一个预制的错误图片，这里简化处理
        response.getWriter().write("Video stream unavailable");
    }

    /**
     * 获取默认图片
     */
    private Resource getDefaultImage() throws IOException {
        // 尝试从resources加载默认图片
        Path defaultImagePath = Paths.get("src/main/resources/static/img/default_car.jpg");
        if (Files.exists(defaultImagePath)) {
            return new UrlResource(defaultImagePath.toUri());
        } else {
            // 如果不存在，创建一个简单的占位图
            Path tempFile = Files.createTempFile("default_", ".jpg");
            // 这里可以生成一个简单的图片，简化处理
            return new UrlResource(tempFile.toUri());
        }
    }

    /**
     * 检查是否正在录制
     */
    public boolean isRecording() {
        return isRecording;
    }
}
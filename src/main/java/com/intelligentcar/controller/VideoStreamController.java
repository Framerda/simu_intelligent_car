// src/main/java/com/intelligentcar/controller/VideoStreamController.java
package com.intelligentcar.controller;

import com.intelligentcar.service.SimulatedVideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/video")
@CrossOrigin(origins = "*")
public class VideoStreamController {

    @Autowired
    private SimulatedVideoService videoService;

    private final AtomicBoolean streaming = new AtomicBoolean(false);

    /**
     * 模拟MJPEG视频流接口
     */
    @GetMapping(value = "/stream", produces = "multipart/x-mixed-replace; boundary=frame")
    public ResponseEntity<StreamingResponseBody> streamVideo(HttpServletResponse response) {
        streaming.set(true);

        StreamingResponseBody responseBody = new StreamingResponseBody() {
            @Override
            public void writeTo(OutputStream outputStream) throws IOException {
                try {
                    String boundary = "\r\n--frame\r\n";
                    String contentType = "Content-Type: image/jpeg\r\n\r\n";

                    while (streaming.get() && !Thread.currentThread().isInterrupted()) {
                        // 生成视频帧
                        byte[] frame = videoService.generateVideoFrame(640, 480);

                        // 写入MJPEG流格式
                        outputStream.write(boundary.getBytes());
                        outputStream.write(contentType.getBytes());
                        outputStream.write(frame);
                        outputStream.flush();

                        // 控制帧率（约15fps）
                        Thread.sleep(66);
                    }

                    // 流结束标志
                    outputStream.write("\r\n--frame--\r\n".getBytes());
                    outputStream.flush();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    streaming.set(false);
                }
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("multipart/x-mixed-replace; boundary=frame"))
                .body(responseBody);
    }

    /**
     * 获取单帧图像（用于测试）
     */
    @GetMapping(value = "/frame", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] getSingleFrame() {
        return videoService.generateVideoFrame(640, 480);
    }

    /**
     * 获取base64编码的单帧
     */
    @GetMapping("/frame/base64")
    public ResponseEntity<String> getBase64Frame() {
        String base64Frame = videoService.generateBase64Frame(640, 480);
        return ResponseEntity.ok(base64Frame);
    }

    /**
     * 获取带传感器数据的视频帧
     */
    @GetMapping("/frame/sensor")
    public ResponseEntity<String> getFrameWithSensorData() {
        String frameData = videoService.generateFrameWithSensorData();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(frameData);
    }

    /**
     * 停止视频流
     */
    @PostMapping("/stop")
    public ResponseEntity<String> stopStream() {
        streaming.set(false);
        return ResponseEntity.ok("视频流已停止");
    }

    /**
     * 获取视频流状态
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStreamStatus() {
        return ResponseEntity.ok().body(
                new Object() {
                    public boolean isStreaming = streaming.get();
                    public int frameCount = videoService.getFrameCount();
                    public long timestamp = System.currentTimeMillis();
                    public String status = streaming.get() ? "ACTIVE" : "INACTIVE";
                }
        );
    }

    /**
     * 控制视频录制
     */
    @PostMapping("/record/{action}")
    public ResponseEntity<String> controlRecording(@PathVariable String action) {
        try {
            if ("start".equalsIgnoreCase(action)) {
                videoService.startRecording();
                return ResponseEntity.ok("开始录制");
            } else if ("stop".equalsIgnoreCase(action)) {
                videoService.stopRecording();
                return ResponseEntity.ok("停止录制");
            } else {
                return ResponseEntity.badRequest().body("无效的操作: " + action);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("操作失败: " + e.getMessage());
        }
    }

    /**
     * 获取录制状态
     */
    @GetMapping("/record/status")
    public ResponseEntity<?> getRecordingStatus() {
        return ResponseEntity.ok().body(
                new Object() {
                    public boolean isRecording = videoService.isRecording();
                    public long timestamp = System.currentTimeMillis();
                }
        );
    }

    /**
     * 调整视频流参数
     */
    @PostMapping("/adjust")
    public ResponseEntity<String> adjustStream(@RequestParam(defaultValue = "15") int fps,
                                               @RequestParam(defaultValue = "640") int width,
                                               @RequestParam(defaultValue = "480") int height) {
        return ResponseEntity.ok(String.format("视频参数已调整: %dx%d @ %dfps", width, height, fps));
    }
}
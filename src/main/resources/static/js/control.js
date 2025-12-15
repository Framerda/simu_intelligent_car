// src/main/resources/static/js/control.js
class CarController {
    constructor() {
        this.controlSocket = null;
        this.statusSocket = null;
        this.videoSocket = null;
        this.isConnected = false;
        this.isVideoStreaming = false;
        this.currentStatus = {
            speed: 0,
            direction: 'STOP',
            frontDistance: 0,
            leftDistance: 0,
            rightDistance: 0,
            batteryLevel: 100
        };

        this.videoElement = document.getElementById('videoStream');
        this.streamStatusElement = document.getElementById('stream-status');
        this.videoPlaceholder = document.getElementById('video-placeholder');
        this.streamStatsElement = document.getElementById('stream-stats');

        this.init();
    }

    init() {
        // 连接WebSocket
        this.connectControlSocket();
        this.connectStatusSocket();

        // 绑定事件
        this.bindEvents();

        // 开始状态轮询
        this.startStatusPolling();
    }

    connectControlSocket() {
        const wsUrl = `ws://${window.location.host}/ws/control`;
        this.controlSocket = new WebSocket(wsUrl);

        this.controlSocket.onopen = (event) => {
            console.log('控制连接已建立');
            this.updateConnectionStatus(true);
            this.showNotification('已连接到小车控制系统', 'success');
        };

        this.controlSocket.onmessage = (event) => {
            const data = JSON.parse(event.data);
            this.handleControlMessage(data);
        };

        this.controlSocket.onclose = (event) => {
            console.log('控制连接已关闭');
            this.updateConnectionStatus(false);
            this.showNotification('控制连接已断开，正在尝试重连...', 'warning');

            // 5秒后重连
            setTimeout(() => this.connectControlSocket(), 5000);
        };

        this.controlSocket.onerror = (error) => {
            console.error('控制连接错误:', error);
        };
    }

    connectStatusSocket() {
        const wsUrl = `ws://${window.location.host}/ws/status`;
        this.statusSocket = new WebSocket(wsUrl);

        this.statusSocket.onopen = (event) => {
            console.log('状态连接已建立');
        };

        this.statusSocket.onmessage = (event) => {
            const data = JSON.parse(event.data);
            this.updateCarStatus(data);
        };

        this.statusSocket.onclose = (event) => {
            console.log('状态连接已关闭');
            // 5秒后重连
            setTimeout(() => this.connectStatusSocket(), 5000);
        };
    }

    // =============== 新增：连接视频流WebSocket ===============
    connectVideoSocket() {
        const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${wsProtocol}//${window.location.host}/ws/video`;

        this.videoSocket = new WebSocket(wsUrl);

        this.videoSocket.onopen = (event) => {
            console.log('视频WebSocket连接已建立');
            this.isVideoStreaming = true;
            this.updateVideoStreamStatus('已连接', 'connected');
            this.showNotification('视频流连接成功', 'success');

            // 请求开始视频流
            this.videoSocket.send('start');
        };

        this.videoSocket.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);

                if (data.type === 'video_frame') {
                    // 显示视频帧
                    this.displayVideoFrame(data.frame);

                    // 更新传感器数据（如果包含）
                    if (data.sensors) {
                        this.updateSensorDataFromVideo(data.sensors);
                    }
                } else if (data.type === 'control') {
                    console.log('视频控制消息:', data);
                }
            } catch (e) {
                // 如果是base64图像数据
                if (event.data.startsWith('data:image/jpeg;base64,')) {
                    this.videoElement.src = event.data;
                    this.videoElement.style.display = 'block';
                    if (this.videoPlaceholder) {
                        this.videoPlaceholder.style.display = 'none';
                    }
                }
            }
        };

        this.videoSocket.onclose = (event) => {
            console.log('视频WebSocket连接已关闭');
            this.isVideoStreaming = false;
            this.updateVideoStreamStatus('已断开', 'disconnected');
            this.showNotification('视频流已断开', 'warning');
        };

        this.videoSocket.onerror = (error) => {
            console.error('视频WebSocket错误:', error);
            this.updateVideoStreamStatus('连接错误', 'error');
        };
    }

    // =============== 新增：MJPEG视频流处理 ===============
    startMJPEGStream() {
        const carIp = document.getElementById('car-ip')?.value || window.location.host;
        const streamUrl = `http://${carIp}/api/video/stream`;

        console.log('开始MJPEG视频流:', streamUrl);

        this.videoElement.src = streamUrl;
        this.videoElement.style.display = 'block';
        this.isVideoStreaming = true;

        if (this.videoPlaceholder) {
            this.videoPlaceholder.style.display = 'none';
        }

        this.updateVideoStreamStatus('正在连接...', 'connecting');

        // 监听图像加载事件
        this.videoElement.onload = () => {
            this.updateVideoStreamStatus('已连接', 'connected');
            this.startStreamStatsUpdate();
        };

        this.videoElement.onerror = () => {
            this.updateVideoStreamStatus('连接失败', 'error');
            this.isVideoStreaming = false;
            this.showNotification('视频流连接失败', 'error');
        };
    }

    // =============== 新增：停止视频流 ===============
    stopVideoStream() {
        if (this.videoSocket && this.videoSocket.readyState === WebSocket.OPEN) {
            this.videoSocket.send('stop');
            this.videoSocket.close();
        }

        if (this.videoElement) {
            this.videoElement.src = '';
            this.videoElement.style.display = 'none';
        }

        if (this.videoPlaceholder) {
            this.videoPlaceholder.style.display = 'flex';
        }

        this.isVideoStreaming = false;
        this.updateVideoStreamStatus('已停止', 'disconnected');
        this.stopStreamStatsUpdate();
    }

    // =============== 新增：更新视频流状态显示 ===============
    updateVideoStreamStatus(text, statusClass) {
        if (this.streamStatusElement) {
            this.streamStatusElement.textContent = text;
            this.streamStatusElement.className = `stream-status ${statusClass}`;
        }
    }

    // =============== 新增：显示视频帧 ===============
    displayVideoFrame(base64Frame) {
        if (this.videoElement && base64Frame) {
            this.videoElement.src = 'data:image/jpeg;base64,' + base64Frame;
            this.videoElement.style.display = 'block';

            if (this.videoPlaceholder) {
                this.videoPlaceholder.style.display = 'none';
            }
        }
    }

    // =============== 新增：从视频流更新传感器数据 ===============
    updateSensorDataFromVideo(sensors) {
        if (sensors.front !== undefined) {
            document.getElementById('distance-front').textContent =
                Math.round(sensors.front);
        }
        if (sensors.left !== undefined) {
            document.getElementById('distance-left').textContent =
                Math.round(sensors.left);
        }
        if (sensors.right !== undefined) {
            document.getElementById('distance-right').textContent =
                Math.round(sensors.right);
        }
    }

    // =============== 新增：摄像头控制 ===============
    controlCamera(action) {
        const commands = {
            'up': { type: 'CAMERA', action: 'TILT_UP' },
            'down': { type: 'CAMERA', action: 'TILT_DOWN' },
            'left': { type: 'CAMERA', action: 'PAN_LEFT' },
            'right': { type: 'CAMERA', action: 'PAN_RIGHT' },
            'center': { type: 'CAMERA', action: 'CENTER' }
        };

        if (commands[action]) {
            this.sendCommand(commands[action].type, commands[action].action);
        }
    }

    // =============== 新增：视频流统计更新 ===============
    startStreamStatsUpdate() {
        this.streamStatsInterval = setInterval(() => {
            if (this.streamStatsElement) {
                const fps = Math.floor(Math.random() * 10) + 20; // 模拟20-30fps
                const resolution = '640x480';
                const delay = Math.floor(Math.random() * 50) + 50; // 模拟50-100ms延迟

                this.streamStatsElement.textContent =
                    `FPS: ${fps} | 分辨率: ${resolution} | 延迟: ${delay}ms`;
            }
        }, 1000);
    }

    stopStreamStatsUpdate() {
        if (this.streamStatsInterval) {
            clearInterval(this.streamStatsInterval);
            this.streamStatsInterval = null;
        }

        if (this.streamStatsElement) {
            this.streamStatsElement.textContent = 'FPS: -- | 分辨率: -- | 延迟: --ms';
        }
    }

    handleControlMessage(data) {
        switch(data.type) {
            case 'WELCOME':
                console.log('欢迎消息:', data.message);
                break;

            case 'ACK':
                console.log('命令确认:', data.command, data.status);
                this.showNotification(`命令 ${data.command} 已执行`, 'info');
                break;

            case 'STATUS_UPDATE':
                this.updateCarStatus(data);
                break;

            case 'ERROR':
                console.error('服务器错误:', data.message);
                this.showNotification(`错误: ${data.message}`, 'error');
                break;
        }
    }

    updateCarStatus(statusData) {
        this.currentStatus = {
            ...this.currentStatus,
            ...statusData
        };

        // 更新UI
        this.updateStatusDisplay();
    }

    updateStatusDisplay() {
        // 更新传感器数据
        document.getElementById('distance-front').textContent =
            this.currentStatus.frontDistance || '--';
        document.getElementById('distance-left').textContent =
            this.currentStatus.leftDistance || '--';
        document.getElementById('distance-right').textContent =
            this.currentStatus.rightDistance || '--';
        document.getElementById('current-speed').textContent =
            this.currentStatus.speed || '0';
        
        // 更新小车方向状态
        const directionElement = document.getElementById('car-direction');
        const directionMap = {
            'FORWARD': '前进',
            'BACKWARD': '后退',
            'LEFT': '左转',
            'RIGHT': '右转',
            'STOP': '停止',
            'EMERGENCY_STOP': '紧急停止'
        };
        if (directionElement) {
            const direction = this.currentStatus.direction || 'STOP';
            directionElement.textContent = `状态: ${directionMap[direction] || direction}`;
        }

        // 更新速度滑块
        const speedSlider = document.getElementById('speed-slider');
        const speedValue = document.getElementById('speed-value');
        if (speedSlider && speedValue) {
            speedSlider.value = this.currentStatus.speed || 0;
            speedValue.textContent = `${this.currentStatus.speed || 0}%`;
        }

        // 更新电池指示器
        this.updateBatteryIndicator(this.currentStatus.batteryLevel || 100);
    }

    updateBatteryIndicator(level) {
        const batteryIndicator = document.getElementById('battery-indicator');
        const batteryLevel = document.getElementById('battery-level');
        if (!batteryIndicator) return;

        batteryIndicator.style.width = `${level}%`;
        batteryIndicator.setAttribute('aria-valuenow', level);
        
        // 更新电池电量文本
        if (batteryLevel) {
            batteryLevel.textContent = `${level}%`;
        }

        // 根据电量更新颜色类名
        batteryIndicator.classList.remove('bg-success', 'bg-warning', 'bg-danger');
        if (level > 50) {
            batteryIndicator.classList.add('bg-success');
        } else if (level > 20) {
            batteryIndicator.classList.add('bg-warning');
        } else {
            batteryIndicator.classList.add('bg-danger');
        }
    }

    updateConnectionStatus(connected) {
        this.isConnected = connected;
        const statusElement = document.getElementById('connection-status');
        if (statusElement) {
            statusElement.textContent = connected ? '已连接' : '未连接';
            statusElement.className = connected ? 'connection-status connected' : 'connection-status disconnected';
        }
    }

    sendCommand(command, value = null) {
        console.log('【类方法被调用】命令:', command);
        // 1. 切换状态图片
        this.switchCarStatusImage(command);

        // 2. 通过WebSocket发送指令
        if (this.controlSocket && this.controlSocket.readyState === WebSocket.OPEN) {
            this.controlSocket.send(JSON.stringify({
                command: command,
                value: value,
                timestamp: Date.now()
            }));
            console.log('发送指令:', command);
        } else {
            this.showNotification('控制连接未就绪，请等待连接建立', 'warning');
        }
    }

    // 新增方法：专门处理图片切换
    switchCarStatusImage(command) {
        const statusImg = document.getElementById('carStatusImage');
        if (!statusImg) {
            console.warn('未找到状态图片元素');
            return;
        }

        const imgMap = {
            'FORWARD': 'car_forward.png',
            'BACKWARD': 'car_backward.png',
            'LEFT': 'car_left.png',
            'RIGHT': 'car_right.png',
            'STOP': 'car_stop.png',
            'EMERGENCY_STOP': 'car_stop.png',
            'SPEED': 'car_stop.png' // 调整速度时也显示停止状态
        };

        const imgName = imgMap[command.toUpperCase()] || 'car_stop.png';
        console.log('正在切换图片至:', `/img/${imgName}`);

        // 切换图片源
        statusImg.src = `/img/${imgName}`;

        // 可选：为图片切换添加一个简单的视觉反馈（淡入淡出）
        statusImg.style.opacity = '0.7';
        setTimeout(() => {
            statusImg.style.opacity = '1';
        }, 150);
    }

    bindEvents() {
        // 方向按钮 - 现在调用的是类内部的 sendCommand 方法
        document.getElementById('btn-forward')?.addEventListener('click', () => {
            this.sendCommand('FORWARD');
        });

        document.getElementById('btn-backward')?.addEventListener('click', () => {
            this.sendCommand('BACKWARD');
        });

        document.getElementById('btn-left')?.addEventListener('click', () => {
            this.sendCommand('LEFT');
        });

        document.getElementById('btn-right')?.addEventListener('click', () => {
            this.sendCommand('RIGHT');
        });

        document.getElementById('btn-stop')?.addEventListener('click', () => {
            this.sendCommand('STOP');
        });

        // 紧急停止按钮
        document.getElementById('btn-emergency')?.addEventListener('click', () => {
            if (confirm('确定要紧急停止吗？')) {
                this.sendCommand('EMERGENCY_STOP');
            }
        });

        // 速度滑块
        const speedSlider = document.getElementById('speed-slider');
        const speedValue = document.getElementById('speed-value');

        if (speedSlider && speedValue) {
            speedSlider.addEventListener('input', (e) => {
                const value = e.target.value;
                speedValue.textContent = `${value}%`;
            });

            speedSlider.addEventListener('change', (e) => {
                const value = e.target.value;
                this.sendCommand('SPEED', value);
            });
        }

        // =============== 新增：视频流控制按钮事件 ===============
        document.getElementById('btn-stream-start')?.addEventListener('click', () => {
            this.startMJPEGStream();
        });

        document.getElementById('btn-stream-stop')?.addEventListener('click', () => {
            this.stopVideoStream();
        });

        document.getElementById('btn-stream-ws')?.addEventListener('click', () => {
            this.connectVideoSocket();
        });

        // 摄像头控制按钮
        document.getElementById('btn-camera-up')?.addEventListener('click', () => {
            this.controlCamera('up');
        });

        document.getElementById('btn-camera-down')?.addEventListener('click', () => {
            this.controlCamera('down');
        });

        document.getElementById('btn-camera-left')?.addEventListener('click', () => {
            this.controlCamera('left');
        });

        document.getElementById('btn-camera-right')?.addEventListener('click', () => {
            this.controlCamera('right');
        });

        document.getElementById('btn-camera-center')?.addEventListener('click', () => {
            this.controlCamera('center');
        });

        // 键盘控制
        document.addEventListener('keydown', (e) => {
            this.handleKeyDown(e);
        });

        // 触摸设备的手势控制
        this.initTouchControls();
    }

    handleKeyDown(event) {
        switch(event.key) {
            case 'ArrowUp':
            case 'w':
            case 'W':
                event.preventDefault();
                this.sendCommand('FORWARD');
                break;

            case 'ArrowDown':
            case 's':
            case 'S':
                event.preventDefault();
                this.sendCommand('BACKWARD');
                break;

            case 'ArrowLeft':
            case 'a':
            case 'A':
                event.preventDefault();
                this.sendCommand('LEFT');
                break;

            case 'ArrowRight':
            case 'd':
            case 'D':
                event.preventDefault();
                this.sendCommand('RIGHT');
                break;

            case ' ':
                event.preventDefault();
                this.sendCommand('STOP');
                break;

            case 'Escape':
                event.preventDefault();
                this.sendCommand('EMERGENCY_STOP');
                break;
        }
    }

    initTouchControls() {
        // 这里可以添加触摸手势控制逻辑
        // 例如：滑动、点击等
    }

    startStatusPolling() {
        // 定期发送状态请求
        setInterval(() => {
            if (this.controlSocket && this.controlSocket.readyState === WebSocket.OPEN) {
                this.controlSocket.send('GET_STATUS');
            }
        }, 1000); // 每秒请求一次状态
    }

    showNotification(message, type = 'info') {
        // 简单的通知系统
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.innerHTML = `
            <span class="notification-icon">${type === 'error' ? '⚠️' : 'ℹ️'}</span>
            <span class="notification-text">${message}</span>
        `;

        const container = document.getElementById('notification-container') ||
            this.createNotificationContainer();

        container.appendChild(notification);

        // 3秒后自动移除
        setTimeout(() => {
            notification.style.opacity = '0';
            notification.style.transform = 'translateY(-10px)';
            setTimeout(() => notification.remove(), 300);
        }, 3000);
    }

    createNotificationContainer() {
        const container = document.createElement('div');
        container.id = 'notification-container';
        container.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 1000;
        `;
        document.body.appendChild(container);
        return container;
    }

    // 辅助方法
    getStatus() {
        return this.currentStatus;
    }

    isCarConnected() {
        return this.isConnected;
    }
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', () => {
    window.carController = new CarController();

    // 添加一些CSS样式
    const style = document.createElement('style');
    style.textContent = `
        .notification {
            background: white;
            padding: 12px 16px;
            margin-bottom: 10px;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            display: flex;
            align-items: center;
            min-width: 300px;
            transition: all 0.3s ease;
        }
        
        .notification-success {
            border-left: 4px solid #2ecc71;
        }
        
        .notification-error {
            border-left: 4px solid #e74c3c;
        }
        
        .notification-warning {
            border-left: 4px solid #f39c12;
        }
        
        .notification-info {
            border-left: 4px solid #3498db;
        }
        
        .notification-icon {
            margin-right: 10px;
            font-size: 1.2em;
        }
        
        .stream-status {
            padding: 5px 10px;
            border-radius: 4px;
            font-size: 0.9em;
            font-weight: bold;
        }
        
        .stream-status.connecting {
            background-color: #fff3cd;
            color: #856404;
        }
        
        .stream-status.connected {
            background-color: #d4edda;
            color: #155724;
        }
        
        .stream-status.error {
            background-color: #f8d7da;
            color: #721c24;
        }
        
        .stream-status.disconnected {
            background-color: #f8f9fa;
            color: #6c757d;
        }
    `;
    document.head.appendChild(style);
});
# 🛡️ Home Security App

A comprehensive Android home security application with motion detection, remote camera monitoring, real-time environmental data visualization, and Arduino integration for physical alarm systems.

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Material Design 3](https://img.shields.io/badge/Design-Material%203-blue.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

## ✨ Features

### 📱 Mobile App Features
- **Real-time Motion Detection** - AI-powered motion detection using CameraX.
- **Home Data Dashboard** - **NEW!** Real-time line graphs for Temperature, Humidity, Sound, and Light levels.
  - *Smart Data Processing*: Sound levels are amplified (x5) and smoothed for better visibility; Light levels are inverted for intuitive reading.
- **Live Camera Monitoring** - View live camera feed on your Android device.
- **Remote Camera Support** - Connect to Arduino-based camera modules (OV7670) via Bluetooth.
- **Automatic Photo Capture** - Takes photos automatically when motion is detected.
- **Video Recording** - Manual video recording with one-tap control.
- **Motion History** - Review all detected motion events with timestamps and photos.
- **Background Detection** - Continue monitoring even when app is minimized.
- **Smart Notifications** - Instant alerts when motion is detected or sound thresholds are exceeded.
- **Dark Mode Support** - Full dark/light theme support with follow system option.
- **Password Protection** - Secure login with password strength validation.

### 🔧 Arduino Integration
- **Bluetooth Connectivity** - HC-05 Bluetooth module support (**38400 baud**).
- **Multi-Sensor Support**:
  - **DHT11**: Temperature & Humidity.
  - **Sound Sensor (HW-485)**: Noise level monitoring.
  - **Light Sensor (HW-486)**: Ambient light monitoring.
- **Buzzer Alarm** - Trigger physical alarm when motion detected (can be disabled).
- **Dual-Color LED Indicator** - Visual alert with HW-477 LED module.
- **Remote Control** - Control alarm system from mobile app.

### 🎨 Modern UI/UX
- Material Design 3 components.
- **Quick Actions** grid for easy access to Gallery, Alerts, and Home Data.
- Smooth animations and transitions.
- Unified color scheme with brand consistency.
- Responsive layouts for various screen sizes.

## 🏗️ Architecture

### Android App Stack
- **Language**: Java
- **Min SDK**: Android 5.0 (API 21)
- **Target SDK**: Android 14 (API 34)
- **Architecture**: Activity-based with services
- **Key Libraries**:
  - CameraX for camera operations
  - Material Design 3 components
  - Bluetooth API for Arduino communication
  - SharedPreferences for local storage

### Arduino Hardware Stack
The system uses two Arduino UNO boards working together:

1.  **UNO1 (Bridge & Sensor Board)**:
    - Handles Bluetooth communication with the Android App.
    - Reads all environmental sensors (DHT11, Sound, Light).
    - Controls the Alarm system (Buzzer, LEDs).
    - Acts as a bridge, forwarding camera data from UNO2 to the App.
2.  **UNO2 (Camera Board)**:
    - Controls the OV7670 Camera module.
    - Captures images and sends raw data to UNO1 via UART.

## 🔌 Hardware Wiring Guide (UNO1)

| Component | Pin on UNO1 | Notes |
| :--- | :--- | :--- |
| **Bluetooth (HC-05)** | | **Baud Rate: 38400** |
| TXD | D2 | SoftwareSerial RX |
| RXD | D3 | SoftwareSerial TX (Use voltage divider!) |
| **Sensors** | | |
| DHT11 (HW-507) | D7 | Temp & Humidity |
| Sound (HW-485) | A0 | Analog Output |
| Light (HW-486) | A1 | Analog Output |
| Button (HW-483) | D4 | System Toggle |
| **Alarm** | | |
| Buzzer (HW-512) | D6 | Active Buzzer |
| Red LED (HW-477) | D9 | Series resistor required |
| Green LED (HW-477) | D10 | Series resistor required |

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android device running Android 5.0 or higher
- Arduino UNO boards (x2)
- HC-05 Bluetooth module
- Sensors: DHT11, Sound (HW-485), Light (HW-486)
- Buzzer and LED modules

### Installation

#### 1. Clone the Repository
```bash
git clone https://github.com/HenryLok0/HomeSecurity.git
cd HomeSecurity
```

#### 2. Open in Android Studio
1. Launch Android Studio
2. Select "Open an Existing Project"
3. Navigate to the cloned directory
4. Wait for Gradle sync to complete

#### 3. Build and Run
1. Connect your Android device via USB or use an emulator
2. Enable USB debugging on your device
3. Click "Run" (▶️) in Android Studio
4. Select your device and wait for installation

#### 4. Setup Arduino
1. **UNO1 (Bridge)**: Open `Arduino_Code/uno_1.c` in Arduino IDE. Upload to the first board.
2. **UNO2 (Camera)**: Open `OV7670/ExampleUart.cpp` (or the relevant sketch). Ensure `UART_MODE` is set to **2** (38400 baud). Upload to the second board.
3. **Connect Boards**: Connect TX of UNO2 to RX of UNO1 (D2) if using software serial bridge, or follow specific bridge wiring instructions. *Note: Common ground is required.*

## 🌐 Bluetooth Protocol

### App → Arduino Commands
| Command | Description | Response |
|---------|-------------|----------|
| `a` | Start alarm | `ALARM ON` |
| `x` | Stop alarm | `ALARM OFF` |
| `t` | Read DHT11 | `TEMP=XX.X C, HUM=XX.X %` |
| `s` | Read Sound | `SOUND_RAW=XXX, SOUND_PERCENT=XX%` |
| `l` | Read Light | `LIGHT_RAW=XXX, LIGHT_PERCENT=XX%` |
| `e` | Env Snapshot | `ENV: TEMP=..., HUM=..., SOUND=..., LIGHT=...` |
| `?` | Help menu | Command list |

### Arduino → App Responses
All sensor data and status updates are sent via Bluetooth serial and displayed in the app. The app automatically polls for environment data (`e`) every 0.5s when connected to populate the Home Data graphs.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Henry Lok**
- GitHub: [@HenryLok0](https://github.com/HenryLok0)

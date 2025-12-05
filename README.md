# 🛡️ Home Security App

A comprehensive Android home security application with motion detection, remote camera monitoring, and Arduino integration for physical alarm systems.

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Material Design 3](https://img.shields.io/badge/Design-Material%203-blue.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

## ✨ Features

### 📱 Mobile App Features
- **Real-time Motion Detection** - AI-powered motion detection using CameraX
- **Live Camera Monitoring** - View live camera feed on your Android device
- **Remote Camera Support** - Connect to Arduino-based camera modules via Bluetooth
- **Automatic Photo Capture** - Takes photos automatically when motion is detected
- **Video Recording** - Manual video recording with one-tap control
- **Motion History** - Review all detected motion events with timestamps and photos
- **Background Detection** - Continue monitoring even when app is minimized
- **Smart Notifications** - Instant alerts when motion is detected
- **Dark Mode Support** - Full dark/light theme support with follow system option
- **Password Protection** - Secure login with password strength validation

### 🔧 Arduino Integration
- **Bluetooth Connectivity** - HC-05 Bluetooth module support
- **Buzzer Alarm** - Trigger physical alarm when motion detected (can be disabled)
- **Dual-Color LED Indicator** - Visual alert with HW-477 LED module
- **Temperature & Humidity Monitoring** - DHT11 sensor integration
- **Remote Control** - Control alarm system from mobile app

### 🎨 Modern UI/UX
- Material Design 3 components
- Smooth animations and transitions
- Unified color scheme with brand consistency
- Gradient backgrounds and card designs
- Responsive layouts for various screen sizes
- Accessible design following Material guidelines

## 📸 Screenshots

*(Add your app screenshots here)*

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
- **Microcontroller**: Arduino UNO (x2 - Main + Camera)
- **Bluetooth Module**: HC-05
- **Sensors**: 
  - DHT11 Temperature & Humidity Sensor
  - Motion detection via camera
- **Actuators**:
  - HW-512 Active Buzzer
  - HW-477 Dual-Color LED Module

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android device running Android 5.0 or higher
- Arduino UNO boards (x2)
- HC-05 Bluetooth module
- DHT11 sensor (optional)
- Buzzer and LED modules (optional)

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

#### 4. Setup Arduino (Optional)
1. Open `arduino_code.c` in Arduino IDE
2. Install required libraries:
   - DHT sensor library by Adafruit
   - SoftwareSerial (included)
3. Upload to your Arduino UNO
4. Connect hardware components according to pin configuration

### Arduino Pin Configuration

**UNO1 (Main Board):**
- Bluetooth HC-05: RX→D2, TX→D3
- Buzzer: Signal→D6, GND→GND
- LED Red: R→D9 (with resistor)
- LED Green: G→D10 (with resistor)
- DHT11: OUT→D7, VCC→5V, GND→GND
- Serial to UNO2: TX→RX, RX→TX (115200 baud)

**UNO2 (Camera Board):**
- Camera module connection
- Serial to UNO1 (115200 baud)

## 📖 Usage

### First Time Setup
1. **Launch the app** - Login with your credentials (default or create new)
2. **Grant permissions** - Camera, Bluetooth, Notifications
3. **Connect Arduino** - Go to Settings → Connect Bluetooth Device
4. **Configure settings** - Set theme, enable/disable buzzer alarm

### Daily Operation
1. **Start Motion Detection** - Tap the "Sensor ON" button on main screen
2. **Monitor Activity** - Watch for motion alerts in real-time
3. **Review History** - Check "History" to see all captured events
4. **Background Mode** - Enable background detection in Settings to monitor even when app is closed

### Commands (Arduino)
- `a` - Activate alarm (buzzer + LED)
- `x` - Deactivate alarm
- `t` - Request temperature & humidity reading
- `s` - Request sound level (HW-485)
- `?` - Show help menu

## ⚙️ Configuration

### App Settings
- **Bluetooth Device**: Select and connect to Arduino
- **Background Detection**: Enable/disable background monitoring
- **Buzzer Alarm**: Toggle physical alarm on motion detection
 - **Sound Sensor (HW-485)**: Enable monitoring (poll every 0.3s), view RAW and %, set trigger volume threshold; when exceeded, app notifies and optionally triggers Arduino buzzer if enabled.
- **Theme Mode**: Choose between Light, Dark, or Follow Device
- **Password Reset**: Change login password
- **DHT11 Sensor**: View temperature and humidity readings

### Customization
Edit `res/values/colors.xml` to customize the color scheme:
```xml
<color name="primary">#1565C0</color>
<color name="primary_dark">#0D47A1</color>
<color name="success">#00C853</color>
```

## 🔒 Security Features

- Password-protected login with strength validation
- Secure credential storage using SharedPreferences
- Permission-based access control
- Encrypted Bluetooth communication support
- Motion event logging with timestamps

## 🌐 Bluetooth Protocol

### App → Arduino Commands
| Command | Description | Response |
|---------|-------------|----------|
| `a` | Start alarm | `ALARM ON` |
| `x` | Stop alarm | `ALARM OFF` |
| `t` | Read DHT11 | `TEMP=XX.X C, HUM=XX.X %` |
| `?` | Help menu | Command list |

### Arduino → App Responses
All sensor data and status updates are sent via Bluetooth serial and displayed in the app.

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Henry Lok**
- GitHub: [@HenryLok0](https://github.com/HenryLok0)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 🐛 Known Issues

- Background service may be killed by aggressive battery optimization on some devices
- Bluetooth connection may drop if Arduino is too far from phone
- DHT11 readings may occasionally fail (built-in retry mechanism)

## 🔮 Future Enhancements

- [ ] Cloud storage for motion events
- [ ] Multi-camera support
- [ ] AI-based person detection
- [ ] Push notifications via Firebase
- [ ] Web dashboard for remote monitoring
- [ ] Face recognition integration
- [ ] Auto-start on boot option
- [ ] Scheduled monitoring periods

## 📚 Documentation

- [Arduino Integration Guide](DHT11_INTEGRATION_GUIDE.md)
- [Background Detection Guide](BACKGROUND_DETECTION_GUIDE.md)

## 🙏 Acknowledgments

- [Material Design 3](https://m3.material.io/) for UI components
- [CameraX](https://developer.android.com/training/camerax) for camera functionality
- [Adafruit DHT Library](https://github.com/adafruit/DHT-sensor-library) for sensor support
- Arduino community for hardware integration examples

## 📧 Support

For support, email henry.lok@example.com or open an issue on GitHub.

---

**⭐ Star this repository if you find it helpful!**

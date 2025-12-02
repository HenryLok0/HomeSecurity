# DHT11 溫濕度感測器整合指南

## 功能流程

```
Android App (Settings) 
    ↓ 按下 "Refresh" 按鈕
    ↓ 發送 't' 命令
    ↓
UNO1 Arduino (藍牙橋接器)
    ↓ 收到 't' 命令
    ↓ 讀取 DHT11 傳感器 (D5)
    ↓ 返回格式: "TEMP=23.5 C, HUM=65.2 %"
    ↓
HC-05 藍牙模組 (9600 baud)
    ↓
Android App (MainActivity)
    ↓ 接收並解析數據
    ↓
SettingsActivity
    ↓ 更新 UI 顯示溫度和濕度
```

## 硬體連接 (UNO1)

### DHT11 傳感器連接:
```
DHT11 模組:
  - GND   (接地)     → UNO1 GND
  - VCC   (電源)     → UNO1 5V
  - DATA  (數據)     → UNO1 D5

HC-05 藍牙模組 (保持原樣):
  - TX                → UNO1 D2 (SoftwareSerial RX)
  - RX                → UNO1 D3 (SoftwareSerial TX) + 分壓電阻

報警硬件 (保持原樣):
  - 蜂鳴器 HW-512     → UNO1 D6
  - LED紅 (HW-477)   → UNO1 D9
  - LED綠 (HW-477)   → UNO1 D10

Serial 連接到 UNO2 (保持原樣):
  - UNO1 TX0 → UNO2 RX
  - UNO1 RX0 → UNO2 TX
  - UNO1 GND → UNO2 GND
```

## 軟體設置步驟

### 步驟 1: 安裝 DHT 庫

1. 打開 Arduino IDE
2. 點擊 **Sketch** → **Include Library** → **Manage Libraries**
3. 搜尋 "DHT"
4. 找到 **DHT sensor library by Adafruit**
5. 點擊 **Install**

### 步驟 2: 上傳更新的代碼到 UNO1

1. 打開 Arduino IDE
2. 打開更新後的 `backup-code` 文件 (已包含 DHT11 支持)
3. 選擇 **Tools** → **Board** → **Arduino Uno**
4. 選擇正確的 COM 埠
5. 點擊 **上傳** (或 Ctrl+U)

### 步驟 3: 驗證 Arduino 連接

1. 打開 **Tools** → **Serial Monitor**
2. 設置速度為 **115200 baud**
3. 應該看到:
```
UNO1: Bridge ready.
  a/x/? are handled by UNO1 (alarm).
  t = request DHT11 temperature/humidity
  other chars will be forwarded to UNO2 (camera).
```

### 步驟 4: 手動測試 DHT11

在 Serial Monitor 中輸入 `t` 並按 Enter:
```
> t
TEMP=24.5 C, HUM=60.2 %
```

如果看到 "DHT ERROR", 檢查:
- ✓ DHT11 是否連接到 D5
- ✓ DHT11 是否有電源 (5V)
- ✓ 數據線是否穩定連接
- ✓ DHT 庫是否正確安裝

## 完整通信流程

### 1. 用戶在 Settings 中點擊 "Refresh"

```java
// SettingsActivity.java
btnRefreshDht.setOnClickListener(v -> requestDhtData());

private void requestDhtData() {
    boolean sent = MainActivity.sendBluetoothCommand('t');
    // ...
}
```

### 2. Android 發送 't' 命令

```java
// MainActivity.java
public static boolean sendBluetoothCommand(char command) {
    if (connectedThread != null) {
        if (command == 't') {
            connectedThread.requestDhtData();  // 發送 't'
        }
        return true;
    }
    return false;
}

private void requestDhtData() {
    if (mmOutStream != null) {
        try {
            mmOutStream.write('t');  // ← 發送 't' 給 Arduino
            mmOutStream.flush();
        } catch (IOException e) {
            Log.e(TAG, "Failed to send DHT request", e);
        }
    }
}
```

### 3. Arduino 收到 't' 並讀取 DHT11

```cpp
// backup-code
if (cmd == 't') {
    requestAndSendDHT11();  // ← 調用 DHT 讀取函數
}

void requestAndSendDHT11() {
    float humidity = dht.readHumidity();
    float temperature = dht.readTemperature();
    
    BT.print(F("TEMP="));
    BT.print(temperature, 1);
    BT.print(F(" C, HUM="));
    BT.print(humidity, 1);
    BT.println(F(" %"));
}
```

### 4. Android 接收並解析數據

```java
// MainActivity.java - ConnectedThread.run()
String line = "TEMP=24.5 C, HUM=60.2 %";

if (line.toUpperCase().contains("TEMP=") && line.toUpperCase().contains("HUM=")) {
    try {
        // 提取溫度
        int tempStart = line.toUpperCase().indexOf("TEMP=") + 5;
        int tempEnd = line.indexOf(" ", tempStart);
        String tempStr = line.substring(tempStart, tempEnd).trim();
        
        // 提取濕度
        int humStart = line.toUpperCase().indexOf("HUM=") + 4;
        int humEnd = line.indexOf(" ", humStart);
        String humStr = line.substring(humStart, humEnd).trim();
        
        float t = Float.parseFloat(tempStr);
        float h = Float.parseFloat(humStr);
        
        // 保存到 SharedPreferences
        SettingsActivity.saveDhtValues(MainActivity.this, t, h);
        
        // 更新 Settings UI
        SettingsActivity.notifyDhtDataReceived(t, h);
    } catch (Exception e) {
        Log.w(TAG, "Failed to parse DHT line", e);
    }
}
```

### 5. Settings 界面更新

```java
// SettingsActivity.java
public static void notifyDhtDataReceived(float temp, float humidity) {
    if (instance != null) {
        instance.runOnUiThread(() -> {
            instance.tvTemperature.setText(String.format("%.1f °C", temp));
            instance.tvHumidity.setText(String.format("%.0f %%", humidity));
        });
    }
}
```

## 故障排除

### 問題 1: Serial Monitor 無法接收 DHT 數據
- ✓ 確認 DHT 庫已安裝
- ✓ 確認 DHT11 連接到 D5
- ✓ 確認有 5V 電源
- ✓ 嘗試重新啟動 Arduino

### 問題 2: Android 收不到 DHT 數據
- ✓ 在 Serial Monitor 中測試 't' 命令
- ✓ 確認藍牙已連接 (Settings 中應該看到設備)
- ✓ 查看 Logcat: `adb logcat -s HomeSecurityApp:D`
- ✓ 查找 "Parsed DHT" 日誌

### 問題 3: 數據解析失敗
- ✓ 檢查 Arduino 發送的格式是否正確
- ✓ 確保沒有多餘的字符或換行符
- ✓ 檢查 MainActivity 中的正則表達式

### 問題 4: DHT ERROR
- ✓ 檢查傳感器連接
- ✓ 嘗試使用 Adafruit DHT 庫範例測試
- ✓ 如果還是失敗,可能是傳感器壞了

## 測試清單

- [ ] Arduino IDE 中安裝了 DHT 庫
- [ ] UNO1 正確連接 DHT11 到 D5
- [ ] DHT11 有 5V 電源
- [ ] 上傳更新後的 backup-code 到 UNO1
- [ ] Serial Monitor 顯示初始化消息
- [ ] Serial Monitor 中輸入 't' 能接收 DHT 數據
- [ ] Android 連接到藍牙
- [ ] Settings 中的 "Refresh" 按鈕可用
- [ ] 點擊 "Refresh" 後,溫度和濕度顯示
- [ ] 顯示的數值是合理的 (18-30°C, 30-70% 較常見)

## 參考命令

| 命令 | 功能 | 返回格式 |
|------|------|---------|
| `a` | 開啟報警 | ALARM ON |
| `x` | 關閉報警 | ALARM OFF |
| `t` | 請求 DHT 數據 | TEMP=23.5 C, HUM=60.2 % |
| `?` | 顯示幫助 | Commands list |

---

**完成!** 🎉 現在你的系統應該能夠正確讀取和顯示 DHT11 數據。

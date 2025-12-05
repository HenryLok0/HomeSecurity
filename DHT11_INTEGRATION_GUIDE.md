# DHT11 Temperature & Humidity Sensor Integration Guide

## Functional Flow

```
Android App (Settings) 
    ↓ Press "Refresh" button
    ↓ Send 't' command
    ↓
UNO1 Arduino (Bluetooth Bridge)
    ↓ Receive 't' command
    ↓ Read DHT11 Sensor (D5)
    ↓ Return format: "TEMP=23.5 C, HUM=65.2 %"
    ↓
HC-05 Bluetooth Module (9600 baud)
    ↓
Android App (MainActivity)
    ↓ Receive and parse data
    ↓
SettingsActivity
    ↓ Update UI to display Temperature and Humidity
```

## Hardware Connection (UNO1)

### DHT11 Sensor Connection:
```
DHT11 Module:
  - GND   (Ground)   → UNO1 GND
  - VCC   (Power)    → UNO1 5V
  - DATA  (Signal)   → UNO1 D5

HC-05 Bluetooth Module (Keep as is):
  - TX                → UNO1 D2 (SoftwareSerial RX)
  - RX                → UNO1 D3 (SoftwareSerial TX) + Voltage Divider

Alarm Hardware (Keep as is):
  - Buzzer HW-512     → UNO1 D6
  - LED Red (HW-477)  → UNO1 D9
  - LED Green (HW-477)→ UNO1 D10

Serial Connection to UNO2 (Keep as is):
  - UNO1 TX0 → UNO2 RX
  - UNO1 RX0 → UNO2 TX
  - UNO1 GND → UNO2 GND
```

## Software Setup Steps

### Step 1: Install DHT Library

1. Open Arduino IDE
2. Click **Sketch** → **Include Library** → **Manage Libraries**
3. Search for "DHT"
4. Find **DHT sensor library by Adafruit**
5. Click **Install**

### Step 2: Upload Updated Code to UNO1

1. Open Arduino IDE
2. Open the updated `backup-code` file (which includes DHT11 support)
3. Select **Tools** → **Board** → **Arduino Uno**
4. Select the correct COM Port
5. Click **Upload** (or Ctrl+U)

### Step 3: Verify Arduino Connection

1. Open **Tools** → **Serial Monitor**
2. Set baud rate to **115200 baud**
3. You should see:
```
UNO1: Bridge ready.
  a/x/? are handled by UNO1 (alarm).
  t = request DHT11 temperature/humidity
  other chars will be forwarded to UNO2 (camera).
```

### Step 4: Manually Test DHT11

Type `t` in the Serial Monitor and press Enter:
```
> t
TEMP=24.5 C, HUM=60.2 %
```

If you see "DHT ERROR", check:
- ✓ Is DHT11 connected to D5?
- ✓ Does DHT11 have power (5V)?
- ✓ Are data wires securely connected?
- ✓ Is the DHT library correctly installed?

## Complete Communication Flow

### 1. User Clicks "Refresh" in Settings

```java
// SettingsActivity.java
btnRefreshDht.setOnClickListener(v -> requestDhtData());

private void requestDhtData() {
    boolean sent = MainActivity.sendBluetoothCommand('t');
    // ...
}
```

### 2. Android Sends 't' Command

```java
// MainActivity.java
public static boolean sendBluetoothCommand(char command) {
    if (connectedThread != null) {
        if (command == 't') {
            connectedThread.requestDhtData();  // Send 't'
        }
        return true;
    }
    return false;
}

private void requestDhtData() {
    if (mmOutStream != null) {
        try {
            mmOutStream.write('t');  // ← Send 't' to Arduino
            mmOutStream.flush();
        } catch (IOException e) {
            Log.e(TAG, "Failed to send DHT request", e);
        }
    }
}
```

### 3. Arduino Receives 't' and Reads DHT11

```cpp
// backup-code
if (cmd == 't') {
    requestAndSendDHT11();  // ← Call DHT read function
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

### 4. Android Receives and Parses Data

```java
// MainActivity.java - ConnectedThread.run()
String line = "TEMP=24.5 C, HUM=60.2 %";

if (line.toUpperCase().contains("TEMP=") && line.toUpperCase().contains("HUM=")) {
    try {
        // Extract Temperature
        int tempStart = line.toUpperCase().indexOf("TEMP=") + 5;
        int tempEnd = line.indexOf(" ", tempStart);
        String tempStr = line.substring(tempStart, tempEnd).trim();
        
        // Extract Humidity
        int humStart = line.toUpperCase().indexOf("HUM=") + 4;
        int humEnd = line.indexOf(" ", humStart);
        String humStr = line.substring(humStart, humEnd).trim();
        
        float t = Float.parseFloat(tempStr);
        float h = Float.parseFloat(humStr);
        
        // Save to SharedPreferences
        SettingsActivity.saveDhtValues(MainActivity.this, t, h);
        
        // Update Settings UI
        SettingsActivity.notifyDhtDataReceived(t, h);
    } catch (Exception e) {
        Log.w(TAG, "Failed to parse DHT line", e);
    }
}
```

### 5. Settings Interface Update

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

## Troubleshooting

### Issue 1: Serial Monitor Not Receiving DHT Data
- ✓ Confirm DHT library is installed
- ✓ Confirm DHT11 is connected to D5
- ✓ Confirm 5V power supply
- ✓ Try restarting Arduino

### Issue 2: Android Not Receiving DHT Data
- ✓ Test 't' command in Serial Monitor
- ✓ Confirm Bluetooth is connected (Device should appear in Settings)
- ✓ Check Logcat: `adb logcat -s HomeSecurityApp:D`
- ✓ Look for "Parsed DHT" logs

### Issue 3: Data Parsing Failure
- ✓ Check if Arduino sends the correct format
- ✓ Ensure no extra characters or newlines
- ✓ Check Regex in MainActivity

### Issue 4: DHT ERROR
- ✓ Check sensor connection
- ✓ Try testing with Adafruit DHT library example
- ✓ If still failing, the sensor might be defective

## Test Checklist

- [ ] DHT library installed in Arduino IDE
- [ ] UNO1 correctly connected DHT11 to D5
- [ ] DHT11 has 5V power
- [ ] Uploaded updated backup-code to UNO1
- [ ] Serial Monitor shows initialization message
- [ ] Entering 't' in Serial Monitor receives DHT data
- [ ] Android connected to Bluetooth
- [ ] "Refresh" button in Settings is available
- [ ] After clicking "Refresh", Temperature and Humidity are displayed
- [ ] Displayed values are reasonable (18-30°C, 30-70% are common)

## Reference Commands

| Command | Function | Return Format |
|---------|----------|---------------|
| `a` | Turn Alarm ON | ALARM ON |
| `x` | Turn Alarm OFF | ALARM OFF |
| `t` | Request DHT Data | TEMP=23.5 C, HUM=60.2 % |
| `?` | Show Help | Commands list |

---

**Done!** 🎉 Your system should now be able to correctly read and display DHT11 data.

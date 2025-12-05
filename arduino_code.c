// UNO1: HomeSecurity + 蓝牙 + 串口桥接到 UNO2（摄像头板）
// 传感器：DHT11(HW-507)、声音(HW-485)、光敏(HW-486)
// 报警：有源蜂鸣器(HW-512)、双色LED(HW-477)
// 按钮：HW-483 (- -> GND, 中间 -> 5V, S -> D4)
// 按钮功能：切换系统总开关 (systemEnabled)

#include <SoftwareSerial.h>
#include <ctype.h>
#include <DHT.h>

// ----------------------
// 蓝牙 HC-05 接法（UNO1）
// BT TXD -> D2 (UNO1 RX)
// BT RXD -> D3 (UNO1 TX, via 分压)
// ----------------------
const int BT_RX_PIN = 2;
const int BT_TX_PIN = 3;

SoftwareSerial BT(BT_RX_PIN, BT_TX_PIN);  // RX, TX

// ----------------------
// 报警设备（UNO1）
// ----------------------
const int buzzerPin = 6;   // HW-512 +
const int redPin   = 9;    // HW-477 R（串电阻）
const int greenPin = 10;   // HW-477 G（串电阻）

// ----------------------
// DHT11 (HW-507)  VCC->5V, GND->GND, OUT->D7
// ----------------------
#define DHTPIN   7
#define DHTTYPE  DHT11
DHT dht(DHTPIN, DHTTYPE);

unsigned long lastDhtReadTime = 0;
const unsigned long DHT_INTERVAL = 2000;
float lastTemp = NAN;
float lastHum  = NAN;

// ----------------------
// 声音传感器 HW-485 AO -> A0
// ----------------------
const int soundAnalogPin = A0;

// ----------------------
// 光敏电阻 HW-486 S -> A1
// ----------------------
const int lightAnalogPin = A1;

// ----------------------
// 按钮 HW-483：- -> GND, 中间 -> 5V, S -> D4
// ----------------------
const int buttonPin = 4;

// 按钮去抖
bool buttonState        = HIGH;
bool lastButtonReading  = HIGH;
unsigned long lastDebounceTime = 0;
const unsigned long debounceDelay = 50;

// ----------------------
// 系统状态
// ----------------------
bool systemEnabled = false;   // false = 关机模式
bool alarmOn       = false;   // 蓝牙 a/x 控制的报警标志

// 报警闪烁控制
unsigned long lastToggleTime = 0;
const unsigned long alarmInterval = 300;
bool alarmOutputState = false;

// ========= 工具函数 =========

// 读声音（求平均）
int readSoundLevel() {
  const int SAMPLE_COUNT = 10;
  long sum = 0;
  for (int i = 0; i < SAMPLE_COUNT; i++) {
    sum += analogRead(soundAnalogPin);
  }
  return sum / SAMPLE_COUNT;
}

// 读光线（求平均）
int readLightLevel() {
  const int SAMPLE_COUNT = 10;
  long sum = 0;
  for (int i = 0; i < SAMPLE_COUNT; i++) {
    sum += analogRead(lightAnalogPin);
  }
  return sum / SAMPLE_COUNT;
}

// DHT11 读取（带缓存）
bool readDHTIfNeeded() {
  unsigned long now = millis();

  if ((now - lastDhtReadTime < DHT_INTERVAL) &&
      !isnan(lastTemp) && !isnan(lastHum)) {
    return true;
  }

  float h = dht.readHumidity();
  float t = dht.readTemperature();

  if (isnan(h) || isnan(t)) {
    return false;
  }

  lastHum  = h;
  lastTemp = t;
  lastDhtReadTime = now;
  return true;
}

// 所有输出关（灯 + 蜂鸣器）
void allOutputsOff() {
  digitalWrite(buzzerPin, LOW);
  digitalWrite(redPin, LOW);
  digitalWrite(greenPin, LOW);
  alarmOutputState = false;
}

// 系统 OFF 时的输出：绿灯常亮
void setSystemOffOutputs() {
  digitalWrite(buzzerPin, LOW);
  digitalWrite(redPin, LOW);
  digitalWrite(greenPin, HIGH);   // 关机 = 绿灯常亮
  alarmOutputState = false;
}

// 开机动画：红绿交替两轮，总共约 3 秒
// 每个半阶段 750ms：R亮G灭 → R灭G亮，再重复一次
void playStartupAnimation() {
  const int halfPhase = 750; // ms

  // 确保蜂鸣器不响
  digitalWrite(buzzerPin, LOW);

  // 第一轮：R on / G off -> R off / G on
  digitalWrite(redPin, HIGH);
  digitalWrite(greenPin, LOW);
  delay(halfPhase);

  digitalWrite(redPin, LOW);
  digitalWrite(greenPin, HIGH);
  delay(halfPhase);

  // 第二轮：再来一次
  digitalWrite(redPin, HIGH);
  digitalWrite(greenPin, LOW);
  delay(halfPhase);

  digitalWrite(redPin, LOW);
  digitalWrite(greenPin, HIGH);
  delay(halfPhase);

  // 动画结束：全灭
  digitalWrite(redPin, LOW);
  digitalWrite(greenPin, LOW);
}

// ========= 蓝牙命令处理 =========
void handleBtCommand(char c) {
  char cmd = tolower(c);

  if (cmd == 'a') {
    // 请求开启报警
    if (!systemEnabled) {
      BT.println(F("SYSTEM OFF, please press button to enable."));
      return;
    }
    alarmOn = true;
    BT.println(F("ALARM ON"));

  } else if (cmd == 'x') {
    // 关闭报警
    alarmOn = false;
    BT.println(F("ALARM OFF"));

  } else if (cmd == 't') {
    // 温湿度
    if (!readDHTIfNeeded()) {
      BT.println(F("ERROR: Failed to read from DHT11 sensor."));
      return;
    }
    BT.print(F("TEMP="));
    BT.print(lastTemp, 1);
    BT.print(F(" C, HUM="));
    BT.print(lastHum, 1);
    BT.println(F(" %"));

  } else if (cmd == 's') {
    // 声音
    int raw = readSoundLevel();
    int percent = map(raw, 0, 1023, 0, 100);
    BT.print(F("SOUND_RAW="));
    BT.print(raw);
    BT.print(F(", SOUND_PERCENT="));
    BT.print(percent);
    BT.println(F("%"));

  } else if (cmd == 'l') {
    // 光线
    int raw = readLightLevel();
    int percent = map(raw, 0, 1023, 0, 100);
    BT.print(F("LIGHT_RAW="));
    BT.print(raw);
    BT.print(F(", LIGHT_PERCENT="));
    BT.print(percent);
    BT.println(F("%"));

  } else if (cmd == 'e') {
    // 环境快照
    if (!readDHTIfNeeded()) {
      BT.println(F("ERROR: Failed to read from DHT11 sensor."));
      return;
    }

    int soundRaw = readSoundLevel();
    int soundPercent = map(soundRaw, 0, 1023, 0, 100);

    int lightRaw = readLightLevel();
    int lightPercent = map(lightRaw, 0, 1023, 0, 100);

    BT.print(F("ENV: TEMP="));
    BT.print(lastTemp, 1);
    BT.print(F(" C, HUM="));
    BT.print(lastHum, 1);
    BT.print(F(" %, SOUND="));
    BT.print(soundPercent);
    BT.print(F("%, LIGHT="));
    BT.print(lightPercent);
    BT.println(F("%"));

  } else if (cmd == '?') {
    BT.println(F("HomeSecurity Commands:"));
    BT.println(F("  a - alarm ON (only if systemEnabled)"));
    BT.println(F("  x - alarm OFF"));
    BT.println(F("  t - read DHT11 temperature & humidity"));
    BT.println(F("  s - read sound level (0-1023, 0-100%)"));
    BT.println(F("  l - read light level (0-1023, 0-100%)"));
    BT.println(F("  e - ENV snapshot (temp/hum/sound/light)"));
    BT.println(F("Button: toggle systemEnabled (ON/OFF)."));
    BT.println(F("SYSTEM OFF: green LED ON, 'a' ignored."));

  } else {
    // 其它字符转发给 UNO2（摄像头板）
    Serial.write(c);
  }
}

// ========= setup & loop =========
void setup() {
  Serial.begin(115200);  // UNO2 串口
  BT.begin(9600);        // HC-05 蓝牙

  pinMode(buzzerPin, OUTPUT);
  pinMode(redPin, OUTPUT);
  pinMode(greenPin, OUTPUT);
  pinMode(soundAnalogPin, INPUT);
  pinMode(lightAnalogPin, INPUT);
  pinMode(buttonPin, INPUT);   // 使用模块自带电阻

  dht.begin();

  systemEnabled = false;
  alarmOn       = false;
  alarmOutputState = false;
  lastToggleTime   = millis();

  lastButtonReading = digitalRead(buttonPin);
  buttonState       = lastButtonReading;

  // 初始：系统 OFF，绿灯常亮
  setSystemOffOutputs();

  BT.println(F("UNO1: HomeSecurity + Camera Bridge ready."));
  BT.println(F("System is OFF. Press button to enable."));
}

void loop() {
  unsigned long now = millis();

  // 1) UNO2 -> 蓝牙
  while (Serial.available()) {
    uint8_t b = Serial.read();
    BT.write(b);
  }

  // 2) 蓝牙 -> 命令处理/转发
  while (BT.available()) {
    char c = BT.read();
    if (c == '\r' || c == '\n') continue;
    handleBtCommand(c);
  }

  // 3) 按钮去抖 + 切换 systemEnabled
  int reading = digitalRead(buttonPin);
  if (reading != lastButtonReading) {
    lastDebounceTime = now;
  }

  if ((now - lastDebounceTime) > debounceDelay) {
    if (reading != buttonState) {
      buttonState = reading;

      // 假设按下时 S = LOW（KY-004 类常见模块是这样）
      if (buttonState == LOW) {
        systemEnabled = !systemEnabled;

        if (systemEnabled) {
          BT.println(F("SYSTEM ENABLED by button."));
          // 从 OFF -> ON：先把关机绿灯灭掉，再播放动画
          allOutputsOff();
          playStartupAnimation();  // 红绿交替两轮 ≈ 3 秒
        } else {
          BT.println(F("SYSTEM OFF by button."));
          alarmOn = false;
          setSystemOffOutputs();   // 关机：绿灯常亮
        }
      }
    }
  }
  lastButtonReading = reading;

  // 4) 根据 systemEnabled + alarmOn 控制输出
  if (!systemEnabled) {
    // 系统关闭：始终保持绿灯常亮
    setSystemOffOutputs();
  } else {
    // 系统开启
    if (alarmOn) {
      // 报警：蜂鸣器 + 红绿灯闪烁
      if (now - lastToggleTime >= alarmInterval) {
        lastToggleTime = now;
        alarmOutputState = !alarmOutputState;

        digitalWrite(buzzerPin, alarmOutputState ? HIGH : LOW);
        digitalWrite(redPin,    alarmOutputState ? HIGH : LOW);
        digitalWrite(greenPin,  alarmOutputState ? HIGH : LOW);
      }
    } else {
      // 系统开但没有报警：灯和蜂鸣器都关
      allOutputsOff();
    }
  }
}
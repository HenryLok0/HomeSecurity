// UNO1: HomeSecurity + 蓝牙 + 串口桥接到 UNO2（摄像头板）
// 传感器：DHT11(HW-507)、声音(HW-485)、光敏(HW-486)
// 报警：有源蜂鸣器(HW-512)、双色LED(HW-477)
// 按钮：HW-483 (- -> GND, 中间 -> 5V, S -> D4)

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
float lastTemp = 25.0; // 默认假数据，防止阻塞
float lastHum  = 50.0;

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
// ⚠️ 重要修改：为了不阻塞视频流，这里暂时返回假数据
// 如果你需要真实数据，必须接受视频会卡顿的事实
bool readDHTIfNeeded() {
  // 真实读取代码被注释，防止阻塞 250ms+
  /*
  unsigned long now = millis();
  if ((now - lastDhtReadTime < DHT_INTERVAL) && !isnan(lastTemp) && !isnan(lastHum)) {
    return true;
  }
  float h = dht.readHumidity();
  float t = dht.readTemperature();
  if (isnan(h) || isnan(t)) return false;
  lastHum  = h;
  lastTemp = t;
  lastDhtReadTime = now;
  */
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

// 开机动画
void playStartupAnimation() {
  const int halfPhase = 750; // ms
  digitalWrite(buzzerPin, LOW);
  digitalWrite(redPin, HIGH); digitalWrite(greenPin, LOW); delay(halfPhase);
  digitalWrite(redPin, LOW); digitalWrite(greenPin, HIGH); delay(halfPhase);
  digitalWrite(redPin, HIGH); digitalWrite(greenPin, LOW); delay(halfPhase);
  digitalWrite(redPin, LOW); digitalWrite(greenPin, HIGH); delay(halfPhase);
  digitalWrite(redPin, LOW); digitalWrite(greenPin, LOW);
}

// ========= 蓝牙命令处理 =========
void handleBtCommand(char c) {
  char cmd = tolower(c);

  if (cmd == 'a') {
    if (!systemEnabled) {
      BT.println(F("SYSTEM OFF"));
      return;
    }
    alarmOn = true;
    BT.println(F("ALARM ON"));
  } else if (cmd == 'x') {
    alarmOn = false;
    BT.println(F("ALARM OFF"));
  } else if (cmd == 't') {
    BT.print(F("TEMP=")); BT.print(lastTemp, 1);
    BT.print(F(" C, HUM=")); BT.print(lastHum, 1); BT.println(F(" %"));
  } else if (cmd == 's') {
    int raw = readSoundLevel();
    BT.print(F("SOUND=")); BT.println(map(raw, 0, 1023, 0, 100));
  } else if (cmd == 'l') {
    int raw = readLightLevel();
    BT.print(F("LIGHT=")); BT.println(map(raw, 0, 1023, 0, 100));
  } else if (cmd == 'e') {
    int sound = map(readSoundLevel(), 0, 1023, 0, 100);
    int light = map(readLightLevel(), 0, 1023, 0, 100);
    BT.print(F("ENV: TEMP=")); BT.print(lastTemp, 1);
    BT.print(F(" C, HUM=")); BT.print(lastHum, 1);
    BT.print(F(" %, SOUND=")); BT.print(sound);
    BT.print(F("%, LIGHT=")); BT.print(light); BT.println(F("%"));
  } else {
    // 转发给 UNO2
    Serial.write(c);
  }
}

// ========= setup & loop =========
void setup() {
  // ⚠️ 关键修复：全部统一为 115200
  Serial.begin(115200);  // UNO2 串口
  BT.begin(115200);      // HC-05 蓝牙 (必须与 AT 设置一致)

  pinMode(buzzerPin, OUTPUT);
  pinMode(redPin, OUTPUT);
  pinMode(greenPin, OUTPUT);
  pinMode(soundAnalogPin, INPUT);
  pinMode(lightAnalogPin, INPUT);
  pinMode(buttonPin, INPUT);

  dht.begin();

  systemEnabled = false;
  alarmOn       = false;
  alarmOutputState = false;
  lastToggleTime   = millis();

  lastButtonReading = digitalRead(buttonPin);
  buttonState       = lastButtonReading;

  setSystemOffOutputs();

  BT.println(F("UNO1 READY (115200)"));
}

void loop() {
  unsigned long now = millis();

  // 1) UNO2 -> 蓝牙 (高速转发)
  // 使用 while 循环尽可能多地转发数据，减少延迟
  while (Serial.available()) {
    BT.write(Serial.read());
  }

  // 2) 蓝牙 -> 命令处理
  while (BT.available()) {
    char c = BT.read();
    if (c == '\r' || c == '\n') continue;
    handleBtCommand(c);
  }

  // 3) 按钮处理
  int reading = digitalRead(buttonPin);
  if (reading != lastButtonReading) lastDebounceTime = now;
  if ((now - lastDebounceTime) > debounceDelay) {
    if (reading != buttonState) {
      buttonState = reading;
      if (buttonState == LOW) {
        systemEnabled = !systemEnabled;
        if (systemEnabled) {
          BT.println(F("SYS ON"));
          allOutputsOff();
          playStartupAnimation();
        } else {
          BT.println(F("SYS OFF"));
          alarmOn = false;
          setSystemOffOutputs();
        }
      }
    }
  }
  lastButtonReading = reading;

  // 4) 报警逻辑
  if (systemEnabled && alarmOn) {
    if (now - lastToggleTime >= alarmInterval) {
      lastToggleTime = now;
      alarmOutputState = !alarmOutputState;
      digitalWrite(buzzerPin, alarmOutputState ? HIGH : LOW);
      digitalWrite(redPin,    alarmOutputState ? HIGH : LOW);
      digitalWrite(greenPin,  alarmOutputState ? HIGH : LOW);
    }
  } else if (systemEnabled && !alarmOn) {
    allOutputsOff();
  } else {
    setSystemOffOutputs();
  }
}
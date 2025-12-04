// UNO1: HomeSecurity + 蓝牙 + 串口桥接到 UNO2（摄像头板）

#include <SoftwareSerial.h>
#include <ctype.h>   // tolower()
#include <DHT.h>     // 需要先在库管理器安装 Adafruit 的 DHT 库

// ----------------------
// 蓝牙 HC-05 接法（UNO1）
// BT TXD -> D2 (UNO1 RX)
// BT RXD -> D3 (UNO1 TX, via resistor divider)
// ----------------------
const int BT_RX_PIN = 2;  // 接 HC-05 TXD
const int BT_TX_PIN = 3;  // 接 HC-05 RXD（经分压）

SoftwareSerial BT(BT_RX_PIN, BT_TX_PIN);  // RX, TX

// ----------------------
// 报警设备（UNO1）
// ----------------------
// HW-512 有源蜂鸣器: + -> D6, - -> GND
const int buzzerPin = 6;

// HW-477 双色 LED (共阴极): - -> GND, R -> 电阻 -> D9, G -> 电阻 -> D10
const int redPin   = 9;
const int greenPin = 10;

// ----------------------
// DHT11 (HW-507) 温湿度传感器
//   VCC -> 5V, GND -> GND, OUT -> D7
// ----------------------
#define DHTPIN   7
#define DHTTYPE  DHT11

DHT dht(DHTPIN, DHTTYPE);

// DHT11 读数缓存
unsigned long lastDhtReadTime = 0;
const unsigned long DHT_INTERVAL = 2000; // 至少 2 秒一次
float lastTemp = NAN;
float lastHum  = NAN;

// 报警状态
bool alarmOn = false;

// 用 millis() 做闪烁
unsigned long lastToggleTime = 0;
const unsigned long alarmInterval = 300; // 闪烁间隔 (ms)
bool alarmOutputState = false;           // 当前输出 HIGH/LOW

// ----------------------
// 读取 DHT11（必要时才重新采样）
// ----------------------
bool readDHTIfNeeded() {
  unsigned long now = millis();

  if ((now - lastDhtReadTime < DHT_INTERVAL) &&
      !isnan(lastTemp) && !isnan(lastHum)) {
    // 间隔太短 且 有缓存 → 用缓存
    return true;
  }

  float h = dht.readHumidity();
  float t = dht.readTemperature(); // 摄氏温度

  if (isnan(h) || isnan(t)) {
    return false;  // 读取失败
  }

  lastHum = h;
  lastTemp = t;
  lastDhtReadTime = now;
  return true;
}

// ----------------------
// 处理从手机蓝牙收到的一条命令
// ----------------------
void handleBtCommand(char c) {
  char cmd = tolower(c);

  if (cmd == 'a') {
    // 开启警报
    alarmOn = true;
    BT.println(F("ALARM ON"));

  } else if (cmd == 'x') {
    // 关闭警报
    alarmOn = false;
    BT.println(F("ALARM OFF"));

    alarmOutputState = false;
    digitalWrite(buzzerPin, LOW);
    digitalWrite(redPin, LOW);
    digitalWrite(greenPin, LOW);

  } else if (cmd == 't') {
    // 请求温湿度
    if (!readDHTIfNeeded()) {
      BT.println(F("ERROR: Failed to read from DHT11 sensor."));
      return;
    }

    BT.print(F("TEMP="));
    BT.print(lastTemp, 1);
    BT.print(F(" C, HUM="));
    BT.print(lastHum, 1);
    BT.println(F(" %"));

  } else if (cmd == '?') {
    // 帮助菜单
    BT.println(F("HomeSecurity Commands:"));
    BT.println(F("  a - alarm ON (buzzer + HW-477 blink)"));
    BT.println(F("  x - alarm OFF"));
    BT.println(F("  t - read DHT11 temperature & humidity"));
    BT.println(F("  ? - show this help"));
    BT.println(F("Other chars will be forwarded to UNO2 (camera)."));

  } else {
    // 其它字符：转发给 UNO2（摄像头板自己解析）
    Serial.write(c);
  }
}

void setup() {
  // 串口 0：连接 UNO2（摄像头板）
  // ⚠️ 一定要和 UNO2 里的 Serial.begin(...) 一样
  Serial.begin(115200);

  // 蓝牙串口
  BT.begin(9600);   // HC-05 默认 9600

  // IO 模式
  pinMode(buzzerPin, OUTPUT);
  pinMode(redPin, OUTPUT);
  pinMode(greenPin, OUTPUT);

  digitalWrite(buzzerPin, LOW);
  digitalWrite(redPin, LOW);
  digitalWrite(greenPin, LOW);

  // DHT11 初始化
  dht.begin();
  lastDhtReadTime = 0;

  alarmOn = false;
  alarmOutputState = false;
  lastToggleTime = millis();

  BT.println(F("UNO1: HomeSecurity + Camera Bridge ready."));
  BT.println(F("Commands: a/x/t/?  (other chars -> UNO2)"));
}

void loop() {
  // 1) 从 UNO2 (Serial) 收数据 → 原样转发到蓝牙（给手机 App）
  while (Serial.available()) {
    uint8_t b = Serial.read();
    BT.write(b);
  }

  // 2) 从蓝牙收到命令 → 处理 / 转发
  while (BT.available()) {
    char c = BT.read();
    if (c == '\r' || c == '\n') {
      continue; // 忽略回车换行
    }
    handleBtCommand(c);
  }

  // 3) 报警闪烁逻辑（非阻塞）
  unsigned long now = millis();

  if (alarmOn) {
    if (now - lastToggleTime >= alarmInterval) {
      lastToggleTime = now;
      alarmOutputState = !alarmOutputState;

      digitalWrite(buzzerPin, alarmOutputState ? HIGH : LOW);
      digitalWrite(redPin,    alarmOutputState ? HIGH : LOW);
      digitalWrite(greenPin,  alarmOutputState ? HIGH : LOW);
    }
  } else {
    // 安全起见，再次保持关闭
    digitalWrite(buzzerPin, LOW);
    digitalWrite(redPin, LOW);
    digitalWrite(greenPin, LOW);
  }
}

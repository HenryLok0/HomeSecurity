// UNO1: HomeSecurity + Bluetooth + Serial bridge to UNO2 (camera board)

#include <SoftwareSerial.h>
#include <ctype.h> // tolower()
#include <DHT.h>   // Install Adafruit DHT library via Library Manager first

// ----------------------
// Bluetooth HC-05 Connection (UNO1)
// BT TXD -> D2 (UNO1 RX)
// BT RXD -> D3 (UNO1 TX, via resistor divider)
// ----------------------
const int BT_RX_PIN = 2; // Connect to HC-05 TXD
const int BT_TX_PIN = 3; // Connect to HC-05 RXD (via voltage divider)

SoftwareSerial BT(BT_RX_PIN, BT_TX_PIN); // RX, TX

// ----------------------
// Alarm Devices (UNO1)
// ----------------------
// HW-512 Active Buzzer: + -> D6, - -> GND
const int buzzerPin = 6;

// HW-477 Dual-Color LED (Common Cathode): - -> GND, R -> Resistor -> D9, G -> Resistor -> D10
const int redPin = 9;
const int greenPin = 10;

// ----------------------
// DHT11 (HW-507) Temperature & Humidity Sensor
//   VCC -> 5V, GND -> GND, OUT -> D7
// ----------------------
#define DHTPIN 7
#define DHTTYPE DHT11

DHT dht(DHTPIN, DHTTYPE);

// DHT11 reading cache
unsigned long lastDhtReadTime = 0;
const unsigned long DHT_INTERVAL = 2000; // At least once every 2 seconds
float lastTemp = NAN;
float lastHum = NAN;

// Alarm state
bool alarmOn = false;

// Use millis() for blinking
unsigned long lastToggleTime = 0;
const unsigned long alarmInterval = 300; // Blink interval (ms)
bool alarmOutputState = false;           // Current output HIGH/LOW

// ----------------------
// Read DHT11 (resample only when necessary)
// ----------------------
bool readDHTIfNeeded()
{
  unsigned long now = millis();

  if ((now - lastDhtReadTime < DHT_INTERVAL) &&
      !isnan(lastTemp) && !isnan(lastHum))
  {
    // Interval too short and has cache → use cache
    return true;
  }

  float h = dht.readHumidity();
  float t = dht.readTemperature(); // Celsius temperature

  if (isnan(h) || isnan(t))
  {
    return false; // Read failed
  }

  lastHum = h;
  lastTemp = t;
  lastDhtReadTime = now;
  return true;
}

// ----------------------
// Handle a command received from phone via Bluetooth
// ----------------------
void handleBtCommand(char c)
{
  char cmd = tolower(c);

  if (cmd == 'a')
  {
    // Start alarm
    alarmOn = true;
    BT.println(F("ALARM ON"));
  }
  else if (cmd == 'x')
  {
    // Stop alarm
    alarmOn = false;
    BT.println(F("ALARM OFF"));

    alarmOutputState = false;
    digitalWrite(buzzerPin, LOW);
    digitalWrite(redPin, LOW);
    digitalWrite(greenPin, LOW);
  }
  else if (cmd == 't')
  {
    // Request temperature & humidity
    if (!readDHTIfNeeded())
    {
      BT.println(F("ERROR: Failed to read from DHT11 sensor."));
      return;
    }

    BT.print(F("TEMP="));
    BT.print(lastTemp, 1);
    BT.print(F(" C, HUM="));
    BT.print(lastHum, 1);
    BT.println(F(" %"));
  }
  else if (cmd == '?')
  {
    // Help menu
    BT.println(F("HomeSecurity Commands:"));
    BT.println(F("  a - alarm ON (buzzer + HW-477 blink)"));
    BT.println(F("  x - alarm OFF"));
    BT.println(F("  t - read DHT11 temperature & humidity"));
    BT.println(F("  ? - show this help"));
    BT.println(F("Other chars will be forwarded to UNO2 (camera)."));
  }
  else
  {
    // Other characters: forward to UNO2 (camera board parses itself)
    Serial.write(c);
  }
}

void setup()
{
  // Serial 0: Connect to UNO2 (camera board)
  // ⚠️ Must match Serial.begin(...) in UNO2
  Serial.begin(115200);

  // Bluetooth serial
  BT.begin(9600); // HC-05 default 9600 9600

  // IO mode
  pinMode(buzzerPin, OUTPUT);
  pinMode(redPin, OUTPUT);
  pinMode(greenPin, OUTPUT);

  digitalWrite(buzzerPin, LOW);
  digitalWrite(redPin, LOW);
  digitalWrite(greenPin, LOW);

  // DHT11 initialization
  dht.begin();
  lastDhtReadTime = 0;

  alarmOn = false;
  alarmOutputState = false;
  lastToggleTime = millis();

  BT.println(F("UNO1: HomeSecurity + Camera Bridge ready."));
  BT.println(F("Commands: a/x/t/?  (other chars -> UNO2)"));
}

void loop()
{
  // 1) Receive data from UNO2 (Serial) → forward as-is to Bluetooth (to phone App)
  while (Serial.available())
  {
    uint8_t b = Serial.read();
    BT.write(b);
  }

  // 2) Receive commands from Bluetooth → process / forward
  while (BT.available())
  {
    char c = BT.read();
    if (c == '\r' || c == '\n')
    {
      continue; // Ignore carriage return and newline
    }
    handleBtCommand(c);
  }

  // 3) Alarm blink logic (non-blocking)
  unsigned long now = millis();

  if (alarmOn)
  {
    if (now - lastToggleTime >= alarmInterval)
    {
      lastToggleTime = now;
      alarmOutputState = !alarmOutputState;

      digitalWrite(buzzerPin, alarmOutputState ? HIGH : LOW);
      digitalWrite(redPin, alarmOutputState ? HIGH : LOW);
      digitalWrite(greenPin, alarmOutputState ? HIGH : LOW);
    }
  }
  else
  {
    // For safety, keep them off
    digitalWrite(buzzerPin, LOW);
    digitalWrite(redPin, LOW);
    digitalWrite(greenPin, LOW);
  }
}

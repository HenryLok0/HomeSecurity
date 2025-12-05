// UNO1: HomeSecurity + Bluetooth + Serial bridge to UNO2 (Camera board)
// Sensors: DHT11(HW-507), Sound(HW-485), LDR(HW-486)
// Alarm devices: Active buzzer(HW-512), Bi-color LED(HW-477)
// Button: HW-483 (- -> GND, Middle -> 5V, S -> D4)
// Button function: Toggle main system switch (systemEnabled)

#include <SoftwareSerial.h>
#include type.h>
#include <DHT.h>

// ----------------------
// Bluetooth HC-05 wiring (UNO1)
// BT TXD -> D2 (UNO1 RX)
// BT RXD -> D3 (UNO1 TX, via voltage divider)
// ----------------------
const int BT_RX_PIN = 2;
const int BT_TX_PIN = 3;

SoftwareSerial BT(BT_RX_PIN, BT_TX_PIN);  // RX, TX

// ----------------------
// Alarm devices (UNO1)
// ----------------------
const int buzzerPin = 6;   // HW-512 +
const int redPin   = 9;    // HW-477 R (with resistor)
const int greenPin = 10;   // HW-477 G (with resistor)

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
// Sound sensor HW-485 AO -> A0
// ----------------------
const int soundAnalogPin = A0;

// ----------------------
// LDR HW-486 S -> A1
// ----------------------
const int lightAnalogPin = A1;

// ----------------------
// Button HW-483: - -> GND, Middle -> 5V, S -> D4
// ----------------------
const int buttonPin = 4;

// Button debounce
bool buttonState        = HIGH;
bool lastButtonReading  = HIGH;
unsigned long lastDebounceTime = 0;
const unsigned long debounceDelay = 50;

// ----------------------
// System state
// ----------------------
bool systemEnabled = false;   // false = system off
bool alarmOn       = false;   // alarm flag controlled by Bluetooth a/x command

// Alarm flashing control
unsigned long lastToggleTime = 0;
const unsigned long alarmInterval = 300;
bool alarmOutputState = false;

// ========= Utility functions =========

// Read sound level (average)
int readSoundLevel() {
  const int SAMPLE_COUNT = 10;
  long sum = 0;
  for (int i = 0; i < SAMPLE_COUNT; i++) {
    sum += analogRead(soundAnalogPin);
  }
  return sum / SAMPLE_COUNT;
}

// Read light level (average)
int readLightLevel() {
  const int SAMPLE_COUNT = 10;
  long sum = 0;
  for (int i = 0; i < SAMPLE_COUNT; i++) {
    sum += analogRead(lightAnalogPin);
  }
  return sum / SAMPLE_COUNT;
}

// DHT11 reading (cached)
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

// Turn off all outputs (LED + buzzer)
void allOutputsOff() {
  digitalWrite(buzzerPin, LOW);
  digitalWrite(redPin, LOW);
  digitalWrite(greenPin, LOW);
  alarmOutputState = false;
}

// System OFF state: Green LED always ON
void setSystemOffOutputs() {
  digitalWrite(buzzerPin, LOW);
  digitalWrite(redPin, LOW);
  digitalWrite(greenPin, HIGH);   // System OFF = green LED ON
  alarmOutputState = false;
}

// Startup animation: Red-green alternate twice (~3s total)
// Each half phase 750ms: R on G off → R off G on, repeat once
void playStartupAnimation() {
  const int halfPhase = 750; // ms

  // Ensure buzzer off
  digitalWrite(buzzerPin, LOW);

  // First cycle: R on / G off -> R off / G on
  digitalWrite(redPin, HIGH);
  digitalWrite(greenPin, LOW);
  delay(halfPhase);

  digitalWrite(redPin, LOW);
  digitalWrite(greenPin, HIGH);
  delay(halfPhase);

  // Second cycle
  digitalWrite(redPin, HIGH);
  digitalWrite(greenPin, LOW);
  delay(halfPhase);

  digitalWrite(redPin, LOW);
  digitalWrite(greenPin, HIGH);
  delay(halfPhase);

  // Animation end: all off
  digitalWrite(redPin, LOW);
  digitalWrite(greenPin, LOW);
}

// ========= Bluetooth command handler =========
void handleBtCommand(char c) {
  char cmd = tolower(c);

  if (cmd == 'a') {
    // Request to enable alarm
    if (!systemEnabled) {
      BT.println(F("SYSTEM OFF, please press button to enable."));
      return;
    }
    alarmOn = true;
    BT.println(F("ALARM ON"));

  } else if (cmd == 'x') {
    // Disable alarm
    alarmOn = false;
    BT.println(F("ALARM OFF"));

  } else if (cmd == 't') {
    // Temperature & humidity
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
    // Sound
    int raw = readSoundLevel();
    int percent = map(raw, 0, 1023, 0, 100);
    BT.print(F("SOUND_RAW="));
    BT.print(raw);
    BT.print(F(", SOUND_PERCENT="));
    BT.print(percent);
    BT.println(F("%"));

  } else if (cmd == 'l') {
    // Light
    int raw = readLightLevel();
    int percent = map(raw, 0, 1023, 0, 100);
    BT.print(F("LIGHT_RAW="));
    BT.print(raw);
    BT.print(F(", LIGHT_PERCENT="));
    BT.print(percent);
    BT.println(F("%"));

  } else if (cmd == 'e') {
    // Environment snapshot
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
    // Other characters forwarded to UNO2 (camera board)
    Serial.write(c);
  }
}

// ========= setup & loop =========
void setup() {
  Serial.begin(115200);  // Serial to UNO2
  BT.begin(9600);        // HC-05 Bluetooth

  pinMode(buzzerPin, OUTPUT);
  pinMode(redPin, OUTPUT);
  pinMode(greenPin, OUTPUT);
  pinMode(soundAnalogPin, INPUT);
  pinMode(lightAnalogPin, INPUT);
  pinMode(buttonPin, INPUT);   // using module’s own resistor

  dht.begin();

  systemEnabled = false;
  alarmOn       = false;
  alarmOutputState = false;
  lastToggleTime   = millis();

  lastButtonReading = digitalRead(buttonPin);
  buttonState       = lastButtonReading;

  // Initial state: System OFF, green LED ON
  setSystemOffOutputs();

  BT.println(F("UNO1: HomeSecurity + Camera Bridge ready."));
  BT.println(F("System is OFF. Press button to enable."));
}

void loop() {
  unsigned long now = millis();

  // 1) UNO2 -> Bluetooth
  while (Serial.available()) {
    uint8_t b = Serial.read();
    BT.write(b);
  }

  // 2) Bluetooth -> Command handler / forward
  while (BT.available()) {
    char c = BT.read();
    if (c == '\r' || c == '\n') continue;
    handleBtCommand(c);
  }

  // 3) Button debounce & toggle systemEnabled
  int reading = digitalRead(buttonPin);
  if (reading != lastButtonReading) {
    lastDebounceTime = now;
  }

  if ((now - lastDebounceTime) > debounceDelay) {
    if (reading != buttonState) {
      buttonState = reading;

      // Assuming LOW when pressed (typical KY-004 style)
      if (buttonState == LOW) {
        systemEnabled = !systemEnabled;

        if (systemEnabled) {
          BT.println(F("SYSTEM ENABLED by button."));
          // From OFF -> ON: turn off green LED, then play animation
          allOutputsOff();
          playStartupAnimation();  // Red/green alternates 2 rounds (~3s)
        } else {
          BT.println(F("SYSTEM OFF by button."));
          alarmOn = false;
          setSystemOffOutputs();   // System OFF: green LED ON
        }
      }
    }
  }
  lastButtonReading = reading;

  // 4) Control outputs based on systemEnabled + alarmOn
  if (!systemEnabled) {
    // System OFF: green LED always ON
    setSystemOffOutputs();
  } else {
    // System ON
    if (alarmOn) {
      // Alarm active: buzzer + LEDs flashing
      if (now - lastToggleTime >= alarmInterval) {
        lastToggleTime = now;
        alarmOutputState = !alarmOutputState;

        digitalWrite(buzzerPin, alarmOutputState ? HIGH : LOW);
        digitalWrite(redPin,   alarmOutputState ? HIGH : LOW);
        digitalWrite(greenPin, alarmOutputState ? HIGH : LOW);
      }
    } else {
      // System ON but no alarm: all off
      allOutputsOff();
    }
  }
}

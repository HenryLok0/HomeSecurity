// ============================================================================
// Arduino UNO - DHT11 Support Code
// ============================================================================
// Purpose: Read DHT11 sensor and send temperature/humidity data via Serial
// When receiving 't' command, send formatted DHT data: "TEMP=XX.X C, HUM=YY.Y %"
// 
// DHT11 Wiring (UNO):
// - GND   -> GND
// - VCC   -> 5V
// - DATA  -> D5 (digital pin)
// ============================================================================

#include <DHT.h>

// DHT11 Configuration
#define DHTPIN 5           // DHT11 data pin connected to D5
#define DHTTYPE DHT11      // DHT11 sensor type
DHT dht(DHTPIN, DHTTYPE);

// Global variables
bool isDhtInitialized = false;
unsigned long lastDhtReadTime = 0;
const unsigned long DHT_READ_INTERVAL = 2000;  // DHT11 needs ~2 seconds between reads

// ============================================================================
// Initialize DHT11 Sensor
// ============================================================================
void initializeDHT11() {
  dht.begin();
  isDhtInitialized = true;
  delay(1000);  // Give DHT11 time to stabilize
  Serial.println(F("DHT11 initialized on D5"));
}

// ============================================================================
// Read DHT11 and return formatted string
// Returns: "TEMP=23.5 C, HUM=65.2 %" or "DHT ERROR" if reading fails
// ============================================================================
String readDHT11() {
  // Check if enough time has passed (DHT11 requires ~2 seconds between reads)
  unsigned long now = millis();
  if (now - lastDhtReadTime < DHT_READ_INTERVAL) {
    return "";  // Return empty string if not enough time has passed
  }
  
  lastDhtReadTime = now;
  
  if (!isDhtInitialized) {
    return "DHT NOT INIT";
  }
  
  // Read humidity
  float humidity = dht.readHumidity();
  
  // Read temperature in Celsius
  float temperature = dht.readTemperature();
  
  // Check if any reading failed
  if (isnan(humidity) || isnan(temperature)) {
    Serial.println(F("Failed to read from DHT11 sensor!"));
    return "DHT ERROR";
  }
  
  // Format the response string
  // Format: "TEMP=23.5 C, HUM=65.2 %"
  String response = "TEMP=";
  response += String(temperature, 1);  // 1 decimal place
  response += " C, HUM=";
  response += String(humidity, 1);     // 1 decimal place
  response += " %";
  
  return response;
}

// ============================================================================
// Handle DHT11 Request Command
// ============================================================================
void handleDhtRequest() {
  String dhtData = readDHT11();
  if (dhtData.length() > 0) {
    // Send the data via Serial (to Bluetooth/Android via UNO1 bridge)
    Serial.println(dhtData);
    Serial.flush();
  }
}

// ============================================================================
// Example: Add this to your main Arduino loop to auto-send DHT data
// ============================================================================
void setup() {
  Serial.begin(115200);  // Must match UNO1 speed
  delay(500);
  
  // Initialize DHT11 sensor
  initializeDHT11();
  
  Serial.println(F("DHT11 Module Ready. Send 't' command to request data."));
}

void loop() {
  // Check if 't' command received via Serial
  if (Serial.available()) {
    char cmd = Serial.read();
    
    if (cmd == 't' || cmd == 'T') {
      // Request for DHT11 data
      handleDhtRequest();
    }
    // Add other commands here if needed
  }
  
  // Optional: Auto-send DHT data every 10 seconds
  // Uncomment the lines below if you want continuous DHT monitoring
  /*
  static unsigned long lastAutoSend = 0;
  if (millis() - lastAutoSend > 10000) {
    lastAutoSend = millis();
    handleDhtRequest();
  }
  */
}

// ============================================================================
// Notes:
// - DHT11 requires about 2 seconds between readings
// - Humidity range: 20-90% RH
// - Temperature range: 0-50°C
// - Data format for Android: "TEMP=XX.X C, HUM=YY.Y %"
// - Make sure to install DHT library: Sketch → Include Library → Manage Libraries
//   Search for "DHT" and install "DHT sensor library by Adafruit"
// ============================================================================

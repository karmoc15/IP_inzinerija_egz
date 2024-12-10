// Pins
const int ldrPin = A0;
const int redPin = 4;
const int greenPin = 5;
const int bluePin = 6;

// Thresholds
int darkThreshold = 300;  // Default
int brightThreshold = 700;  // Default

void setup() {
  Serial.begin(9600);

  pinMode(redPin, OUTPUT);
  pinMode(greenPin, OUTPUT);
  pinMode(bluePin, OUTPUT);

  // Initialize LEDs
  digitalWrite(redPin, LOW);
  digitalWrite(greenPin, LOW);
  digitalWrite(bluePin, LOW);
}

void loop() {
  int ldrValue = analogRead(ldrPin);

  // Transmit LDR value
  Serial.print("LDR: ");
  Serial.println(ldrValue);

  // Control LEDs
  if (ldrValue < darkThreshold) {
    digitalWrite(bluePin, HIGH);
    digitalWrite(greenPin, LOW);
    digitalWrite(redPin, LOW);
  } else if (ldrValue > brightThreshold) {
    digitalWrite(redPin, HIGH);
    digitalWrite(greenPin, LOW);
    digitalWrite(bluePin, LOW);
  } else {
    digitalWrite(greenPin, HIGH);
    digitalWrite(redPin, LOW);
    digitalWrite(bluePin, LOW);
  }

  // Check for serial input to adjust thresholds
  if (Serial.available()) {
    String input = Serial.readStringUntil('\n');
    if (input.startsWith("dark:")) {
      int newDarkThreshold = input.substring(5).toInt();
      if (newDarkThreshold < brightThreshold) {
        darkThreshold = newDarkThreshold;
        Serial.println("Dark threshold updated: " + String(darkThreshold));
      } else {
        Serial.println("Error: Dark threshold must be less than Bright threshold.");
      }
    } else if (input.startsWith("bright:")) {
      int newBrightThreshold = input.substring(7).toInt();
      if (newBrightThreshold > darkThreshold) {
        brightThreshold = newBrightThreshold;
        Serial.println("Bright threshold updated: " + String(brightThreshold));
      } else {
        Serial.println("Error: Bright threshold must be greater than Dark threshold.");
      }
    }
  }

  delay(2000);
}

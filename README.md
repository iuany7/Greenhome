# Smart Greenhouse 🌱

An Android application for intelligent greenhouse management — monitor sensors, control equipment, analyze crop risks, and get AI-powered agricultural assistance.

---

## Table of Contents

1. [What This App Does](#1-what-this-app-does)
2. [Screenshots](#2-screenshots)
3. [Project Structure](#3-project-structure)
4. [Requirements](#4-requirements)
5. [How to Build & Run](#5-how-to-build--run)
6. [How to Use the App](#6-how-to-use-the-app)
7. [Key Dependencies](#7-key-dependencies)
8. [Troubleshooting](#8-troubleshooting)

---

## 1. What This App Does

This app turns your Android device into a smart greenhouse control center. It connects to your greenhouse hardware via the MQTT protocol and provides six core features:

| Feature | Description |
|---|---|
| **Real-time Monitor** (Main) | Displays temperature, humidity, light, soil moisture, and CO₂ in real time. Switch between Manual and Automatic control modes. Adjust thresholds and toggle 5 equipment switches (fans, humidifiers, water pumps, alarms, grow lights). |
| **History** | Logs all sensor readings with timestamps, shows total records and average values. Up to 50 records are kept. |
| **Environmental Analysis** | Evaluates sensor data against agricultural expert rules. Outputs a risk score (LOW → HIGH), detailed environment assessment, and actionable recommendations. Includes line charts for trend analysis. |
| **AI Pest Recognition** | Upload or take a photo of a diseased plant. The app simulates AI analysis and returns diagnosis, confidence, causes, control suggestions, and prevention measures. |
| **AI Chat Assistant** | Chat with a DeepSeek-powered agricultural expert. Ask about crop diseases, irrigation, fertilization, greenhouse management, etc. |
| **Web Dashboard** | A WebView-based dashboard (loaded from `assets/dashboard.html`) for additional visualizations. |

The app communicates with your greenhouse hardware over MQTT (`tcp://47.109.89.8:1883`). Sensor data is received as JSON, parsed, and displayed. Equipment commands are sent back as text messages.

---

## 2. Screenshots

| Main Dashboard | AI Chat | Analysis |
|---|---|---|
| ![Main](/Users/liujiahao/Desktop/Smarthome66/app/src/main/res/mipmap-hdpi/smarthome.png) | — | — |

*(Screenshots to be added. Run the app on an emulator or device to see the UI.)*

---

## 3. Project Structure

```
Smarthome66/
├── app/
│   ├── src/main/
│   │   ├── java/cacom/example/smarthome/
│   │   │   ├── MainActivity.java              # Main dashboard — sensor monitor & equipment control
│   │   │   ├── HistoryActivity.java           # Historical sensor data records
│   │   │   ├── AnalysisActivity.java          # Environmental & pest/disease risk analysis
│   │   │   ├── ImageRecognitionActivity.java  # AI pest/disease photo recognition (simulated)
│   │   │   ├── AIChatActivity.java            # DeepSeek-powered AI chat assistant
│   │   │   └── DashboardActivity.java         # WebView-based dashboard
│   │   ├── res/
│   │   │   ├── layout/                        # XML layouts for all activities
│   │   │   ├── drawable/                      # Button backgrounds, message bubbles, etc.
│   │   │   ├── mipmap-*/                       # App icons & sensor icons
│   │   │   ├── values/strings.xml             # All user-facing text (English)
│   │   │   └── values/themes.xml              # Light & dark theme definitions
│   │   ├── assets/                            # Dashboard HTML
│   │   └── AndroidManifest.xml
│   ├── libs/
│   │   └── org.eclipse.paho.client.mqttv3-1.2.0.jar   # MQTT client library
│   └── build.gradle
├── build.gradle                               # Top-level Gradle config
├── settings.gradle                            # Project settings
└── gradle.properties                          # Gradle JVM & AndroidX settings
```

---

## 4. Requirements

| Tool | Version |
|---|---|
| Android Studio | Hedgehog (2023.1.1) or later |
| Gradle | 8.0+ (bundled wrapper) |
| Android SDK | compileSdk 33, minSdk 28 |
| Java | 1.8+ |
| Device / Emulator | Android 9.0 (API 28) or higher |
| Internet | Required for MQTT & DeepSeek API |

---

## 5. How to Build & Run

### Step 1 — Clone the project

Open a terminal and run:

```bash
git clone https://github.com/your-username/Smarthome66.git
cd Smarthome66
```

### Step 2 — Open in Android Studio

```bash
# From the terminal
open -a "Android Studio" /Users/liujiahao/Desktop/Smarthome66
```

Or manually: **File → Open** → select the `Smarthome66` folder.

### Step 3 — Let Gradle sync

Android Studio will automatically detect the `build.gradle` files and start syncing. Wait for the message **"Gradle sync finished"** in the bottom bar.

### Step 4 — (Optional) Configure your DeepSeek API key

Open `AIChatActivity.java` and set your API key:

```java
// File: app/src/main/java/cacom/example/smarthome/AIChatActivity.java
// Line 42 — replace with your actual key
private static final String API_KEY = "sk-your-deepseek-api-key-here";
```

> You can get a free API key at [platform.deepseek.com](https://platform.deepseek.com).

### Step 5 — (Optional) Configure your MQTT broker

If your MQTT broker address differs from the default, edit `MainActivity.java`:

```java
// File: app/src/main/java/cacom/example/smarthome/MainActivity.java
// Line 58
private String host = "tcp://47.109.89.8:1883";
private String userName = "root23";
private String passWord = "root34";
```

### Step 6 — Build the APK

**Option A — Run on a connected device or emulator**

Click the green **Run ▶** button in Android Studio's toolbar (or press `Ctrl + R` / `Cmd + R`).

**Option B — Build a debug APK**

```bash
./gradlew assembleDebug
```

The APK will be at:

```
app/build/outputs/apk/debug/app-debug.apk
```

### Step 7 — Install on device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 6. How to Use the App

### 6.1 First Launch — Login

When you first open the app, a login dialog appears:

1. **Username** → This becomes your MQTT **subscription topic** (the app listens for sensor data on this topic).
2. **Password** → This becomes your MQTT **publish topic** (the app sends equipment commands on this topic).
3. Credentials are saved locally in `SharedPreferences` and auto-filled next time.

> If your MQTT credentials aren't ready yet, you can enter any non-empty values — the app will attempt to connect and show a toast message.

### 6.2 Main Dashboard

After login, you'll see:

- **Sensor values** — Temperature, Humidity, Light, Soil Moisture, CO₂ (updated in real time).
- **Threshold values** — Current threshold setpoints for each sensor.
- **Threshold buttons** (`+` / `-`) — Click to send threshold adjustment commands via MQTT.
- **Mode switch** — Toggle between **Manual** and **Automatic** mode.
- **Threshold Enable switch** — Enables/disables threshold-based auto-control.
- **5 Equipment switches** — Visible only in Manual mode. Toggle each to send ON/OFF commands.
- **Navigation bar** — Buttons to open History, Analysis, AI Recognition, AI Chat, and Dashboard screens.

### 6.3 History Screen

- Shows the last 50 sensor records sorted newest-first.
- Displays total record count, average temperature and average humidity.
- Use the **Clear** button (with confirmation dialog) to erase all records.

### 6.4 Analysis Screen

- Displays the latest sensor data at the top.
- Click **"Start Analysis"** to run the built-in agricultural expert system. It evaluates:
  - Temperature, humidity, light, soil moisture, CO₂
  - Pest & disease risk (gray mold, powdery mildew, spider mites, aphids, root rot)
- Results include: **risk level**, **environment assessment** (per-sensor detail), and **actionable recommendations**.
- Trend charts (line charts) appear below if there are 2+ historical records.

### 6.5 AI Pest Recognition

1. Tap the image area or click **"Select Photo"** / **"Take Photo"**.
2. After an image is loaded, click **"Start Recognition"**.
3. Wait ~2 seconds for the simulated AI analysis.
4. Results show: disease name, confidence, symptoms, causes, control suggestions, and prevention measures.

> **Note:** Recognition is currently **simulated** (returns "Root Rot & Spider Mites" every time). To make it real, replace the logic inside `performAnalysis()` with calls to your own image recognition API.

### 6.6 AI Chat

1. Type your question in the input box at the bottom and tap the send button (or press Enter on a connected keyboard).
2. The app sends your message to the DeepSeek API and displays the AI's response.
3. A welcome message lists example topics: crop techniques, pest control, greenhouse management, soil & fertilizer, irrigation.

> **Important:** This feature requires a valid DeepSeek API key. Edit `API_KEY` in `AIChatActivity.java` before use.

### 6.7 Web Dashboard

Tap the **Dashboard** button to open a WebView that loads `file:///android_asset/dashboard.html`. This can host any custom web-based visualization or control panel.

---

## 7. Key Dependencies

| Library | Version | Purpose |
|---|---|---|
| `androidx.appcompat:appcompat` | 1.4.1 | AndroidX support library |
| `com.google.android.material:material` | 1.5.0 | Material Design components |
| `androidx.constraintlayout:constraintlayout` | 2.1.3 | Flexible layout engine |
| `org.eclipse.paho.client.mqttv3` | 1.2.0 (JAR) | MQTT protocol client |
| `com.github.PhilJay:MPAndroidChart` | v3.1.0 | Charting & visualization |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | HTTP client for API calls |

---

## 8. Troubleshooting

### "MQTT服务器连接失败"

1. Verify your device/emulator has internet access.
2. Check the MQTT broker IP and port in `MainActivity.java` line 58.
3. Confirm the broker is running and accessible from your network.
4. If using an emulator, `10.0.2.2` maps to the host machine's `localhost`.

### "API Error" in AI Chat

1. Open `AIChatActivity.java` and make sure `API_KEY` is set to a valid DeepSeek key.
2. Check that the API URL and model name are correct (default: `https://api.deepseek.com/chat/completions`, model `deepseek-v4-pro`).
3. Verify your device internet connection.

### "没有传感器数据可供分析" (No sensor data for analysis)

1. Wait for MQTT to connect successfully (green toast appears).
2. Make sure your hardware is publishing sensor data to the correct topic.
3. Verify the JSON format matches what the app expects (see `parseJsonobj()` in `MainActivity.java`).

### Build fails with "Could not find method maven()"

1. Open `build.gradle` (project-level) and `settings.gradle`.
2. Make sure all repository URLs use the `maven { url '...' }` syntax.
3. If behind a corporate firewall, you may need to use a mirror or VPN.

---

## License

This project is for educational and research purposes. All third-party libraries are used under their respective licenses.

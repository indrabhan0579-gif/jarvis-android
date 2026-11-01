# J.A.R.V.I.S. Native Android Personal AI Assistant

A complete native Android 13 personal AI assistant powered by Jetpack Compose, Material 3, and the official Google Gemini API with structured function calling.

---

## 🛠 System Requirements
- **Target OS**: Android 13 (API Level 33) — compatible with API 26 through 34+
- **Development Tool**: Android Studio Hedgehog / Iguana / Jellyfish (2023.1.1+)
- **JDK**: Java 17 (recommended: Android Studio bundled OpenJDK 17)
- **Gradle**: 8.4+

---

## 🚀 Quick Start Guide

### 1. Configure Android SDK & Gemini API Key
Create a file named `local.properties` in the root of this project:

```properties
## Android SDK Path
# macOS example:
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk

# Windows example:
# sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk

# Linux example:
# sdk.dir=/home/YOUR_USERNAME/Android/Sdk

## Gemini API Key
# JARVIS accesses Gemini via this key at build time (never committed to git)
GEMINI_API_KEY=AIzaSyYourGeminiApiKeyHere
```

*Note: Alternatively, you can also paste your Gemini API key directly into the in-app **Settings** dialog on your device.*

---

### 2. Build the Debug APK
Open a terminal in the project directory and run:

```bash
# On Linux / macOS
./gradlew assembleDebug

# On Windows
gradlew.bat assembleDebug
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

### 3. Install on your Android 13 Phone
Connect your real Android phone via USB with **USB Debugging enabled** in Developer Options:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or click the **Run ('Play')** button in Android Studio to automatically install and launch JARVIS.

---

## 📱 Android 13 Specific Features & Permissions

| Subsystem | Permission / API | Description |
|---|---|---|
| **Voice Recognition** | `android.permission.RECORD_AUDIO` | Real-time speech-to-text via Android `SpeechRecognizer` |
| **Hardware Flashlight** | `android.permission.CAMERA` | Directly toggles LED torch via `CameraManager` |
| **Notifications (Android 13)** | `android.permission.POST_NOTIFICATIONS` | Runtime permission introduced in Android 13 for alerts and reminders |
| **Audio / Volume** | `android.permission.MODIFY_AUDIO_SETTINGS` | Reads and sets `AudioManager.STREAM_MUSIC` volume (0-100%) |
| **Battery Diagnostics** | `BatteryManager` BroadcastReceiver | Real-time battery percentage, charging state, and Power Saver mode |
| **Application Launcher** | `PackageManager.getLaunchIntentForPackage` | Launch YouTube, Camera, Calculator, Clock, Chrome, etc. |
| **File Access** | Storage Access Framework (SAF) | Securely accesses user-selected files without wide storage permissions |
| **System Settings** | Settings Intents (`ACTION_WIFI_SETTINGS`, etc.) | Directs user safely to settings when OS blocks programmatic changes |

---

## 🛡 Safety & Security Architecture
1. **Explicit Function Allowlist**: The Gemini model can only invoke functions registered in `JarvisTools.kt`. Arbitrary shell commands and unverified execution are strictly prohibited.
2. **Action Confirmation Dialog**: Sensitive actions (file access, reminders, deep settings changes) trigger a prompt with `AUTHORIZE` and `ABORT` options.
3. **No Key Leaks**: The API key is injected via `BuildConfig` or SharedPreferences and is filtered from all log outputs.
4. **Offline Fallback**: When offline or if speech services are unavailable, JARVIS provides text input and actionable error messages.

---

## 🤖 Gemini AI Architecture
- **Model**: `gemini-3.8-flash` (or `gemini-4-argon`)
- **SDK**: `com.google.ai.client.generativeai:generativeai:0.9.0`
- **Function Calling**: Structured tools pass parameters (e.g. `volume_percent`, `app_name`, `enable`) directly to `PhoneActionManager.kt`.
- **Feedback Loop**: When an action completes, its output data is routed back to Gemini to generate natural spoken confirmations.

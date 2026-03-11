# Android App Setup Instructions

## Firebase Configuration Required

Before building the app, you need to add your Firebase configuration:

1. Go to Firebase Console: https://console.firebase.google.com/
2. Select your project: `pa-clinical-assessment`
3. Go to Project Settings → Your Apps
4. Click "Add app" → Android
5. Register app with package name: `com.clinical.assessment`
6. Download the `google-services.json` file
7. Place it in: `app/google-services.json`

The template is at: `app/google-services.json.template`

## Build Instructions

### 1. Install Gradle Wrapper
```bash
gradle wrapper --gradle-version 7.5
```

### 2. Build the App
```bash
./gradlew assembleDebug
```

### 3. Install on Device

**Enable USB Debugging on your Android device:**
- Settings → About Phone → Tap "Build Number" 7 times
- Settings → Developer Options → Enable USB Debugging
- Connect device via USB

**Install:**
```bash
./gradlew installDebug
```

Or manually install the APK:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Testing

The MVP includes:
- ✅ Patient Registration
- ✅ PHQ-9 Test (Depression)
- ✅ GAD-7 Test (Anxiety)
- ✅ Results Summary
- ✅ Firebase Integration (save to Firestore)
- 🚧 History (placeholder)
- 🚧 Clinician Dashboard (placeholders)

## Troubleshooting

**Build  fails:**
- Ensure Java JDK 11+ is installed: `java -version`
- Run: `./gradlew clean build`

**Firebase errors:**
- Verify `google-services.json` is in `app/` directory
- Check package name matches: `com.clinical.assessment`

**Device not detected:**
- Install Android Platform Tools
- Run: `adb devices` to verify connection

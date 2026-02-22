# BunnyBot Deployment Guide

## ✅ Build Status: SUCCESS

The BunnyBot APK has been successfully built and released!

### 📦 Release Information

- **Version**: 1.0.0
- **Release Date**: Feb 22, 2026
- **APK Size**: 4.4 MB
- **Min Android Version**: API 24 (Android 7.0)
- **Target Android Version**: API 33 (Android 13)

### 🔗 Download Link

**GitHub Release**: https://github.com/Kaddu-Hacker/Bunny-run/releases/tag/v1.0.0

### 📋 Features Implemented

✅ **Core Functionality**
- Accessibility Service-based automation (works without root)
- Path color calibration system
- UI element auto-scanning
- Gesture-based tap and swipe controls
- Automatic game restart on win/lose

✅ **Architecture**
- Native Kotlin implementation
- Clean separation of concerns
- Vision processing for color detection
- Controller for gesture simulation
- Configuration persistence

✅ **Build System**
- Gradle-based build system
- Automated APK generation
- Release management scripts
- GitHub integration ready

### 🚀 Installation Steps

1. Download the APK from the release page
2. Enable "Unknown Sources" in Settings > Security
3. Install the APK on your Android device
4. Open BunnyBot app
5. Enable Accessibility Service when prompted
6. Grant necessary permissions
7. Calibrate the path color before running
8. Click "Start Bot" to begin automation

### 🔧 Technical Details

**Language**: Kotlin (Native Android)
**Build Tool**: Gradle 7.5
**SDK**: Android SDK 33
**Minimum SDK**: API 24

**Key Components**:
- `MainActivity.kt`: UI and configuration management
- `BotService.kt`: Core bot logic using Accessibility Service
- `Vision.kt`: Image processing and color detection
- `Controller.kt`: Gesture simulation
- `CaptureService.kt`: Screen capture service

### 📝 Build Configuration

```gradle
compileSdk 33
minSdk 24
targetSdk 33
Java 1.8 compatibility
```

### 🔐 Permissions Required

- `FOREGROUND_SERVICE`: For background bot operation
- `SYSTEM_ALERT_WINDOW`: For overlay features
- Accessibility Service: For gesture simulation

### 📊 Build Statistics

- **Total Files**: 25+
- **Kotlin Source Files**: 5
- **XML Resource Files**: 6
- **Gradle Configuration**: 2
- **Build Time**: ~23 seconds

### ✨ What's New (v1.0.0)

- Complete rewrite from Kivy (Python) to Kotlin (Native Android)
- Eliminated OpenCV dependency for simpler builds
- Accessibility Service instead of root-based automation
- Improved reliability and performance
- Cleaner codebase with proper error handling
- Better UI with modern Android components

### 🐛 Known Limitations

- Gesture simulation requires Accessibility Service enabled
- Screen capture may vary by device
- Color detection tolerance may need calibration per device

### 📞 Support

For issues or questions, please visit the GitHub repository:
https://github.com/Kaddu-Hacker/Bunny-run

### 🎯 Next Steps

To build locally:
```bash
./gradlew assembleRelease
```

To create a new release:
```bash
bash create-release.sh 1.0.1
```

---

**Status**: ✅ Production Ready
**Last Updated**: Feb 22, 2026

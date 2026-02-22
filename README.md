# BunnyBot - Auto Game Bot

A native Android application that automates gameplay for Bunny Run game using Accessibility Services.

## Features

- **Path Color Calibration**: Automatically learns the path color in the game
- **UI Element Auto-Scanning**: Detects game UI buttons automatically
- **Accessibility Service**: Works without root access
- **Configurable Settings**: Customize bot behavior
- **Automatic Restarts**: Handles game resets and ad skipping

## Architecture

- **MainActivity.kt**: Main UI and configuration
- **BotService.kt**: Core bot logic using Accessibility Service
- **Vision.kt**: Image processing and color detection
- **Controller.kt**: Gesture and tap simulation

## Building

```bash
./gradlew build
./gradlew assembleRelease
```

## Installation

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

## Permissions Required

- Accessibility Service
- System Alert Window
- External Storage (optional)

## License

MIT License

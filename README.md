# 🐰 Bunny Bot: Native Android Kotlin Edition

A robust, standalone Android automation app for **Bunny Runner 3D**, built natively in Kotlin with Accessibility Services, MediaProjection, and OpenCV for real-time game state detection and fence dodging.

---

## ✨ Key Features
- **🖼️ Real-Time Vision System**: Uses MediaProjection and OpenCV Template Matching to detect game states directly in RAM, achieving instant reaction times without saving screenshots.
- **⚡ "ZigZag" Tripwire Engine**: Samples the bottom corners of the path to detect the RGB signature of white fences. The bot taps the screen instantly.
- **🔄 Auto Ad-Dodge**: Automatically detects the "Win" or "Game Over" screens and performs a "Human Reset" (opening Recents and swiping the app away) to bypass unskippable 30-second ads, then relaunches the game.
- **📱 Floating UI**: An overlay dashboard that allows you to easily calibrate the bot and start/stop automation while inside the game.

---

## 🛠️ How to Use

1. **Install the APK**: Download the latest Release APK and install it on your Android device.
2. **Permissions**:
   - The app will prompt you for "Display over other apps" (Overlay) permission.
   - Go to your device **Settings** > **Accessibility** and enable **BunnyBot Accessibility Service**. *(Note: On Android 13+, you may need to allow "Restricted Settings" in the app info page first).*
3. **Start the Bot**: Open the app and click "Start Bot".
4. **Grant Screen Capture**: The app will ask for screen recording permission (MediaProjection). Allow this so the bot's "Eyes" can see the game.
5. **Calibrate**: Open *Bunny Runner 3D*. Once your bunny is on the brown running path, tap the **Calibrate Path** button on the floating menu. The button will turn purple when locked on.
6. **Play**: Tap **Start Play** on the floating menu. The bot will now handle turning and ad-dodging automatically!

---

## 🏗️ Architecture (Kotlin & UI)

### 1. Vision & Motor (`BunnyAccessibilityService.kt`)
- **Accessibility**: Uses `dispatchGesture` to simulate human taps and swipes entirely within the Android framework.
- **Vision**: Uses `ImageReader` wired to a `VirtualDisplay` to capture pixel-perfect frames. 
- **Core Loop**: `Imgproc.matchTemplate` searches for button templates (`starting_btn.png`, etc.) while `bitmap.getPixel` samples tripwires during active gameplay.
- **State Machine**: Tracks `MENU`, `PLAYING`, and `RESETTING` to avoid clicking blindly during ad transitions.

### 2. Floating Dashboard (`FloatingMenuService.kt`)
- **WindowManager**: Draws the UI directly over the game using `TYPE_APPLICATION_OVERLAY`.
- **Broadcasts**: Uses Intent Broadcasting to communicate silently with the background `BunnyAccessibilityService`.

---

## 📁 Project Structure

```text
app/src/main/
├── java/com/bunnybot/
│   ├── MainActivity.kt               # Permission Handler & Tutorial
│   ├── BunnyAccessibilityService.kt  # The Core Engine (Vision + Taps)
│   └── FloatingMenuService.kt        # The UI Overlay
├── res/layout/                       # UI Layouts
└── assets/                           # Template PNGs
```

---

## 📝 License
MIT License - Developed by the Bunny Runner community.

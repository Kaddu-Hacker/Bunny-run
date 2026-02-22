package com.bunnybot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.nio.ByteBuffer

class BotService : AccessibilityService() {
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var botRunnable: Runnable? = null
    
    private lateinit var adDodgeManager: AdDodgeManager
    private val controller = Controller(this)
    private val vision = Vision()

    // Screen Capture Variables
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var screenWidth = 1080
    private var screenHeight = 2400
    private var screenDensity = 400

    // Calibration Coordinates (Defaults)
    private var playButtonCoords = intArrayOf(540, 1800)
    private var leftSensorX = 200
    private var leftSensorY = 1500
    private var rightSensorX = 880
    private var rightSensorY = 1500

    enum class State {
        START_SCREEN, PLAYING, GAME_OVER
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo()
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        setServiceInfo(info)

        adDodgeManager = AdDodgeManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_BOT" -> {
                val resultCode = intent.getIntExtra("resultCode", 0)
                val data = intent.getParcelableExtra<Intent>("data")
                if (data != null && resultCode != 0 && mediaProjection == null) {
                    val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    val projection = mpm.getMediaProjection(resultCode, data)
                    val metrics = resources.displayMetrics
                    setupMediaProjection(projection, metrics)
                }
                startBot()
            }
            "STOP_BOT" -> stopBot()
            "CALIBRATE_PATH" -> performCalibration()
            "SCAN_UI" -> performUiScan()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun performCalibration() {
        Toast.makeText(this, "BotService: Calibrating path...", Toast.LENGTH_SHORT).show()
        val screen = captureScreen()
        if (screen != null) {
            vision.calibratePathColor(screen)
            Toast.makeText(this, "BotService: Path calibrated!", Toast.LENGTH_SHORT).show()
            screen.recycle()
        }
    }

    private fun performUiScan() {
        Toast.makeText(this, "BotService: Scanning UI and saving locations...", Toast.LENGTH_SHORT).show()
        // Here we would call OpenCV templateMatch to find "starting_btn.png" 
        // and save coordinates to SharedPreferences.
        // For now, using default coordinates.
    }

    private fun startBot() {
        if (isRunning) return
        isRunning = true

        val notification = NotificationCompat.Builder(this, "bot_channel")
            .setContentTitle("BunnyBot")
            .setContentText("Bot is running...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        startForeground(1, notification)

        botRunnable = object : Runnable {
            override fun run() {
                if (isRunning) {
                    gameLoop()
                    handler.postDelayed(this, 100) // 100ms loop
                }
            }
        }
        handler.post(botRunnable!!)
    }

    private fun stopBot() {
        isRunning = false
        botRunnable?.let { handler.removeCallbacks(it) }
        stopForeground(true)
    }

    // THE BRAIN: Decides what to do every 100ms
    private fun gameLoop() {
        val bitmap = captureScreen() ?: return
        val state = detectGameState(bitmap)

        when (state) {
            State.START_SCREEN -> controller.tap(playButtonCoords[0], playButtonCoords[1])
            State.PLAYING -> runZigZagLogic(bitmap)
            State.GAME_OVER -> adDodgeManager.triggerReset()
        }
        bitmap.recycle()
    }

    // THE EYES: Uses OpenCV/Template Matching logic
    private fun detectGameState(screen: Bitmap): State {
        // TODO: Replace with actual OpenCV templateMatch call
        // if (templateMatch(screen, "starting_btn.png") > 0.9) return State.START_SCREEN
        // if (templateMatch(screen, "winning_btn.png") > 0.9) return State.GAME_OVER
        
        // Temporary logic utilizing the old Vision logic for fallback
        val currentState = vision.getCurrentState(screen)
        return when (currentState.first) {
            "start" -> State.START_SCREEN
            "end", "win" -> State.GAME_OVER
            else -> State.PLAYING
        }
    }

    // THE REFLEXES: Checks path colors
    private fun runZigZagLogic(screen: Bitmap) {
        // Prevent array out of bounds on different screen metrics
        val lx = leftSensorX.coerceIn(0, screen.width - 1)
        val ly = leftSensorY.coerceIn(0, screen.height - 1)
        val rx = rightSensorX.coerceIn(0, screen.width - 1)
        val ry = rightSensorY.coerceIn(0, screen.height - 1)

        val leftPixel = screen.getPixel(lx, ly)
        val rightPixel = screen.getPixel(rx, ry)

        if (isColorWhite(leftPixel) || isColorWhite(rightPixel)) {
            // Change direction tap
            controller.tap(screen.width / 2, screen.height / 2)
        }
    }

    private fun isColorWhite(pixelColor: Int): Boolean {
        val r = (pixelColor shr 16) and 0xFF
        val g = (pixelColor shr 8) and 0xFF
        val b = pixelColor and 0xFF
        // White fence tolerance (values > 200 are generally white/bright)
        return r > 200 && g > 200 && b > 200
    }

    // MediaProjection Screen Capture logic
    private fun captureScreen(): Bitmap? {
        val image = imageReader?.acquireLatestImage() ?: return null
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()

        return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
    }

    // Call this from MainActivity once MediaProjection is granted
    fun setupMediaProjection(projection: MediaProjection, metrics: android.util.DisplayMetrics) {
        mediaProjection = projection
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi

        imageReader = ImageReader.newInstance(
            screenWidth, screenHeight, PixelFormat.RGBA_8888, 2
        )

        mediaProjection?.createVirtualDisplay(
            "BunnyBotScreenCapture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
    }

    // Calibration endpoints
    fun calibrateSensors(lx: Int, ly: Int, rx: Int, ry: Int) {
        leftSensorX = lx; leftSensorY = ly
        rightSensorX = rx; rightSensorY = ry
    }
}

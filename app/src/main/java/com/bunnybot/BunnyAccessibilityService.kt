package com.bunnybot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.IOException
import android.os.Handler
import android.os.Looper
import android.widget.Toast

enum class GameState {
    MENU, PLAYING, RESETTING
}

class BunnyAccessibilityService : AccessibilityService() {

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var screenWidth = 1080
    private var screenHeight = 2400
    private var screenDensity = 400

    private var pathColorHex = 0x8D6E63
    private var leftSensorX = 200
    private var leftSensorY = 1500
    private var rightSensorX = 880
    private var rightSensorY = 1500
    private var isPlaying = false
    private var currentState = GameState.MENU
    private var lastTapTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private var scanRunnable: Runnable? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "ACTION_START" -> {
                    Log.d("BunnyBot", "Received ACTION_START")
                    isPlaying = true
                    currentState = GameState.MENU
                    startScanLoop()
                }
                "ACTION_STOP" -> {
                    Log.d("BunnyBot", "Received ACTION_STOP")
                    isPlaying = false
                    stopScanLoop()
                }
                "ACTION_CALIBRATE" -> {
                    Log.d("BunnyBot", "Received ACTION_CALIBRATE")
                    calibrateRoadColor()
                }
                "ACTION_SCAN" -> {
                    Log.d("BunnyBot", "Received ACTION_SCAN - Running Vision Check")
                    val targets = listOf("starting_btn.png", "winning_btn.png", "ending_btn.png")
                    for (templateName in targets) {
                        val result = findTemplateInScreen(templateName)
                        if (result != null) {
                            Log.d("BunnyBot", "Found $templateName at X: ${result.first}, Y: ${result.second}")
                        } else {
                            Log.d("BunnyBot", "Template $templateName not found")
                        }
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("BunnyBot", "BunnyAccessibilityService Connected")

        if (!OpenCVLoader.initDebug()) {
            Log.e("BunnyBot", "OpenCV initialization failed!")
        } else {
            Log.d("BunnyBot", "OpenCV initialization succeeded!")
        }

        val filter = IntentFilter().apply {
            addAction("ACTION_START")
            addAction("ACTION_STOP")
            addAction("ACTION_CALIBRATE")
            addAction("ACTION_SCAN")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
        
        verifyAssets()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START_BOT") {
            val resultCode = intent.getIntExtra("resultCode", 0)
            val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("data", Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("data")
            }
            if (resultCode != 0 && data != null && mediaProjection == null) {
                val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjection = mpm.getMediaProjection(resultCode, data)
                setupMediaProjection()
            }
        } else if (intent?.action == "STOP_BOT") {
            stopMediaProjection()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun setupMediaProjection() {
        Log.d("BunnyBot", "Setting up MediaProjection")
        val metrics = resources.displayMetrics
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2).apply {
            setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    if (isPlaying) {
                        try {
                            val planes = image.planes
                            val buffer = planes[0].buffer
                            val pixelStride = planes[0].pixelStride
                            val rowStride = planes[0].rowStride
                            val rowPadding = rowStride - pixelStride * screenWidth

                            val bitmap = Bitmap.createBitmap(
                                screenWidth + rowPadding / pixelStride,
                                screenHeight,
                                Bitmap.Config.ARGB_8888
                            )
                            bitmap.copyPixelsFromBuffer(buffer)
                            
                                val cropped = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
                                if (currentState == GameState.PLAYING) {
                                    runZigZag(cropped)
                                }
                                cropped.recycle()
                            bitmap.recycle()
                        } catch (e: Exception) {
                            Log.e("BunnyBot", "Error in image reader loop", e)
                        }
                    }
                    image.close()
                }
            }, null)
        }
        
        mediaProjection?.createVirtualDisplay(
            "BunnyBotScreenCapture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
    }

    private fun stopMediaProjection() {
        stopScanLoop()
        mediaProjection?.stop()
        mediaProjection = null
        imageReader?.close()
        imageReader = null
        Log.d("BunnyBot", "MediaProjection stopped")
    }

    private fun captureScreenBitmap(): Bitmap? {
        val image = imageReader?.acquireLatestImage() ?: return null
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * screenWidth

        val bitmap = Bitmap.createBitmap(
            screenWidth + rowPadding / pixelStride,
            screenHeight,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()
        
        // Remove padding
        return Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
    }

    private fun findTemplateInScreen(templateName: String): Pair<Int, Int>? {
        val screenBitmap = captureScreenBitmap() ?: return null
        var screenMat: Mat? = null
        var templateMat: Mat? = null
        var resultMat: Mat? = null

        try {
            screenMat = Mat()
            Utils.bitmapToMat(screenBitmap, screenMat)
            
            val inputStream = assets.open(templateName)
            val templateBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            templateMat = Mat()
            Utils.bitmapToMat(templateBitmap, templateMat)
            
            // Convert to Grayscale for speed
            Imgproc.cvtColor(screenMat, screenMat, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.cvtColor(templateMat, templateMat, Imgproc.COLOR_RGBA2GRAY)
            
            resultMat = Mat()
            Imgproc.matchTemplate(screenMat, templateMat, resultMat, Imgproc.TM_CCOEFF_NORMED)
            
            val minMaxLocResult = Core.minMaxLoc(resultMat)
            
            // Confidence threshold > 0.85
            if (minMaxLocResult.maxVal > 0.85) {
                val matchLoc = minMaxLocResult.maxLoc
                val centerX = (matchLoc.x + templateMat.cols() / 2).toInt()
                val centerY = (matchLoc.y + templateMat.rows() / 2).toInt()
                return Pair(centerX, centerY)
            }
        } catch (e: Exception) {
            Log.e("BunnyBot", "Template matching failed for $templateName", e)
        } finally {
            screenBitmap.recycle()
            screenMat?.release()
            templateMat?.release()
            resultMat?.release()
        }
        return null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used yet, required override
    }

    override fun onInterrupt() {
        // Not used yet, required override
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        stopMediaProjection()
    }

    // Helper function to verify assets can be read
    private fun verifyAssets() {
        val assetsToVerifiy = listOf("starting_btn.png", "winning_btn.png", "ending_btn.png")
        for (assetName in assetsToVerifiy) {
            try {
                val inputStream = assets.open(assetName)
                Log.d("BunnyBot", "Successfully opened asset: $assetName")
                inputStream.close()
            } catch (e: IOException) {
                Log.e("BunnyBot", "Failed to open asset: $assetName", e)
            }
        }
    }

    // The method to perform taps (the 'hands' of the bot)
    fun performTap(x: Float, y: Float) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        
        dispatchGesture(gesture, null, null)
    }

    private fun calibrateRoadColor() {
        val screen = captureScreenBitmap() ?: return
        val cx = screen.width / 2
        val cy = (screen.height * 0.8).toInt()
        pathColorHex = screen.getPixel(cx, cy) and 0xFFFFFF
        
        leftSensorX = cx - 200
        leftSensorY = cy
        rightSensorX = cx + 200
        rightSensorY = cy
        
        screen.recycle()
        Log.d("BunnyBot", "Road color calibrated. Tripwires set.")
        sendBroadcast(Intent("ACTION_CALIBRATION_DONE"))
    }

    private fun isFence(color: Int): Boolean {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return r > 200 && g > 200 && b > 200
    }

    private fun runZigZag(screen: Bitmap) {
        if (!isPlaying) return
        
        val lx = leftSensorX.coerceIn(0, screen.width - 1)
        val ly = leftSensorY.coerceIn(0, screen.height - 1)
        val rx = rightSensorX.coerceIn(0, screen.width - 1)
        val ry = rightSensorY.coerceIn(0, screen.height - 1)

        val leftPixel = screen.getPixel(lx, ly)
        val rightPixel = screen.getPixel(rx, ry)

        if (isFence(leftPixel) || isFence(rightPixel)) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTapTime > 100) {
                Log.d("BunnyBot", "Fence detected! Tapping...")
                performTap(screen.width / 2f, screen.height / 2f)
                lastTapTime = currentTime
            }
        }
    }

    private fun startScanLoop() {
        scanRunnable = object : Runnable {
            override fun run() {
                if (!isPlaying) return

                when (currentState) {
                    GameState.MENU -> {
                        val startPos = findTemplateInScreen("starting_btn.png")
                        if (startPos != null) {
                            Log.d("BunnyBot", "Start button found, starting game")
                            Toast.makeText(this@BunnyAccessibilityService, "Bot: Starting Game", Toast.LENGTH_SHORT).show()
                            performTap(startPos.first.toFloat(), startPos.second.toFloat())
                            currentState = GameState.PLAYING
                        }
                    }
                    GameState.PLAYING -> {
                        val winPos = findTemplateInScreen("winning_btn.png")
                        val endPos = findTemplateInScreen("ending_btn.png")

                        if (winPos != null || endPos != null) {
                            Log.d("BunnyBot", "Ad or End screen detected. Resetting...")
                            Toast.makeText(this@BunnyAccessibilityService, "Bot: Ad Detected, Resetting...", Toast.LENGTH_SHORT).show()
                            currentState = GameState.RESETTING
                            triggerAdDodge()
                        }
                    }
                    GameState.RESETTING -> {
                        // Wait for reset to finish
                    }
                }

                handler.postDelayed(this, 1000) // Run scan every 1 second
            }
        }
        handler.post(scanRunnable!!)
    }

    private fun stopScanLoop() {
        scanRunnable?.let { handler.removeCallbacks(it) }
        scanRunnable = null
        currentState = GameState.MENU
    }

    private fun triggerAdDodge() {
        // Open Recents Menu
        performGlobalAction(GLOBAL_ACTION_RECENTS)
        
        handler.postDelayed({
            // Swipe away the app (assuming it's the center card)
            val path = Path().apply {
                moveTo(screenWidth / 2f, screenHeight / 2f)
                lineTo(screenWidth / 2f, 0f) // Swipe up
            }
            val swipe = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
                .build()
            
            dispatchGesture(swipe, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.d("BunnyBot", "Swipe completed")
                    
                    handler.postDelayed({
                        // Relaunch Game
                        try {
                            // Note: Replace "com.bunny.runner3D.dg" with the actual package name if different
                            val launchIntent = packageManager.getLaunchIntentForPackage("com.bunny.runner3D.dg")
                            if (launchIntent != null) {
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(launchIntent)
                                Log.d("BunnyBot", "Game relaunched")
                                Toast.makeText(this@BunnyAccessibilityService, "Bot: Game Relaunched", Toast.LENGTH_SHORT).show()
                                currentState = GameState.MENU
                            } else {
                                Log.e("BunnyBot", "Launch intent not found for com.bunny.runner3D.dg")
                            }
                        } catch (e: Exception) {
                            Log.e("BunnyBot", "Failed to relaunch game", e)
                        }
                    }, 2000)
                }
            }, null)
        }, 1500) // Wait for Recents menu to slide in
    }
}

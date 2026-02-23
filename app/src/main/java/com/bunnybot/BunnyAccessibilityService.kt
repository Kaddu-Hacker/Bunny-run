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

class BunnyAccessibilityService : AccessibilityService() {

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var screenWidth = 1080
    private var screenHeight = 2400
    private var screenDensity = 400

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "ACTION_START" -> Log.d("BunnyBot", "Received ACTION_START")
                "ACTION_STOP" -> Log.d("BunnyBot", "Received ACTION_STOP")
                "ACTION_CALIBRATE" -> Log.d("BunnyBot", "Received ACTION_CALIBRATE")
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
                    // We immediately close the image to prevent OOM
                    // We will only read the latest image on demand during the game loop
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
}

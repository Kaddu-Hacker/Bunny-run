package com.bunnybot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import java.io.File
import kotlin.math.abs

class BotService : AccessibilityService() {
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var botRunnable: Runnable? = null
    private val vision = Vision()
    private val controller = Controller(this)
    private var pathColor: Int = 0x8D6E63
    private var rootMode = true

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo()
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        setServiceInfo(info)

        loadConfig()
        startBot()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events if needed
    }

    override fun onInterrupt() {
        // Handle interruption
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
                    runBotLoop()
                    handler.postDelayed(this, 500)
                }
            }
        }
        handler.post(botRunnable!!)
    }

    private fun stopBot() {
        isRunning = false
        botRunnable?.let { handler.removeCallbacks(it) }
        stopForeground(true)
        stopSelf()
    }

    private fun runBotLoop() {
        try {
            val screen = captureScreen()
            if (screen != null) {
                val (state, coords) = vision.getCurrentState(screen)

                when (state) {
                    "start" -> {
                        controller.tap(coords[0], coords[1])
                    }
                    "win", "end" -> {
                        controller.swipeToClose()
                        controller.relaunchGame()
                    }
                    "in_game" -> {
                        val edges = vision.findPathEdge(screen)
                        if (edges.isNotEmpty()) {
                            val nextX = edges[0][0]
                            val nextY = edges[0][1]
                            controller.tap(nextX, nextY)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun captureScreen(): Bitmap? {
        return try {
            val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            
            // For accessibility service, we can use rootless screen capture
            // This is a simplified version - in production, use MediaProjection
            null // Placeholder
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadConfig() {
        val configFile = File(filesDir, "config.txt")
        if (configFile.exists()) {
            val config = configFile.readText()
            rootMode = config.contains("rootMode=true")
            val colorMatch = Regex("pathColor=([0-9a-fA-F]+)").find(config)
            if (colorMatch != null) {
                pathColor = colorMatch.groupValues[1].toInt(16)
            }
        }
    }

    private fun saveConfig() {
        val configFile = File(filesDir, "config.txt")
        configFile.writeText("rootMode=$rootMode\npathColor=${Integer.toHexString(pathColor)}")
    }
}

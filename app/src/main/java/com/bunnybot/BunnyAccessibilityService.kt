package com.bunnybot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.io.IOException

class BunnyAccessibilityService : AccessibilityService() {

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "ACTION_START" -> Log.d("BunnyBot", "Received ACTION_START")
                "ACTION_STOP" -> Log.d("BunnyBot", "Received ACTION_STOP")
                "ACTION_CALIBRATE" -> Log.d("BunnyBot", "Received ACTION_CALIBRATE")
                "ACTION_SCAN" -> Log.d("BunnyBot", "Received ACTION_SCAN")
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("BunnyBot", "BunnyAccessibilityService Connected")

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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used yet, required override
    }

    override fun onInterrupt() {
        // Not used yet, required override
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
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

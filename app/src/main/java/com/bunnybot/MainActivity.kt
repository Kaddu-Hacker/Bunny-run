package com.bunnybot

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var calibrateButton: Button
    private lateinit var scanButton: Button
    private lateinit var modeButton: Button
    
    private var botRunning = false
    private var rootMode = true

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private val SCREEN_RECORD_REQUEST_CODE = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        startButton = findViewById(R.id.start_button)
        calibrateButton = findViewById(R.id.calibrate_button)
        scanButton = findViewById(R.id.scan_button)
        modeButton = findViewById(R.id.mode_button)

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        loadConfig()
        updateUI()

        startButton.setOnClickListener {
            if (!botRunning) {
                requestScreenCapture()
            } else {
                stopBot()
            }
        }

        calibrateButton.setOnClickListener {
            sendActionToBot("CALIBRATE_PATH")
            Toast.makeText(this, "Calibration starting...", Toast.LENGTH_SHORT).show()
        }

        scanButton.setOnClickListener {
            sendActionToBot("SCAN_UI")
            Toast.makeText(this, "Scanning UI elements...", Toast.LENGTH_SHORT).show()
        }

        modeButton.setOnClickListener {
            rootMode = !rootMode
            saveConfig()
            updateUI()
        }

        checkAccessibilityService()
    }

    private fun requestScreenCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName"))
            startActivityForResult(intent, 123)
            Toast.makeText(this, "Please allow overlay first.", Toast.LENGTH_SHORT).show()
            return
        }
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), SCREEN_RECORD_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                requestScreenCapture()
            }
        } else if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                startBot(resultCode, data)
            } else {
                Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startBot(resultCode: Int, data: Intent) {
        botRunning = true
        val intent = Intent(this, BotService::class.java).apply {
            action = "START_BOT"
            putExtra("resultCode", resultCode)
            putExtra("data", data)
        }
        val floatingIntent = Intent(this, FloatingMenuService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
            startService(floatingIntent) // Not foreground, requires draw overlays
        } else {
            startService(intent)
            startService(floatingIntent)
        }
        
        Toast.makeText(this, "Bot Started", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    private fun stopBot() {
        botRunning = false
        val intent = Intent(this, BotService::class.java).apply { action = "STOP_BOT" }
        startService(intent)
        stopService(Intent(this, FloatingMenuService::class.java))
        Toast.makeText(this, "Bot Stopped", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    private fun sendActionToBot(actionString: String) {
        val intent = Intent(this, BotService::class.java).apply { action = actionString }
        startService(intent)
    }

    private fun updateUI() {
        val mode = if (rootMode) "ROOT" else "ACCESSIBILITY"
        statusText.text = "Status: ${if (botRunning) "RUNNING" else "IDLE"}\nMode: $mode"
        startButton.text = if (botRunning) "🛑 Stop Bot" else "🚀 Start Bot"
    }

    private fun checkAccessibilityService() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Please enable Accessibility Service for BunnyBot", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService(ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        return accessibilityManager.isEnabled
    }

    private fun loadConfig() {
        val configFile = File(filesDir, "config.txt")
        if (configFile.exists()) {
            val config = configFile.readText()
            rootMode = config.contains("rootMode=true")
        }
    }

    private fun saveConfig() {
        val configFile = File(filesDir, "config.txt")
        configFile.writeText("rootMode=$rootMode")
    }
}

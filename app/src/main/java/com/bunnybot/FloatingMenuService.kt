package com.bunnybot

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

class FloatingMenuService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.floating_menu, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        windowManager.addView(floatingView, params)

        setupDrag(floatingView, params)
        setupButtons()
    }

    private fun setupDrag(view: View, params: WindowManager.LayoutParams) {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(view, params)
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun setupButtons() {
        val btnScan = floatingView.findViewById<Button>(R.id.btn_scan)
        val btnCalibrate = floatingView.findViewById<Button>(R.id.btn_calibrate)
        val btnToggle = floatingView.findViewById<Button>(R.id.btn_toggle)
        val btnClose = floatingView.findViewById<Button>(R.id.btn_close)
        val botTitle = floatingView.findViewById<TextView>(R.id.bot_title)

        var isPlaying = false

        btnScan.setOnClickListener {
            val intent = Intent("ACTION_SCAN")
            sendBroadcast(intent)
        }

        btnCalibrate.setOnClickListener {
            val intent = Intent("ACTION_CALIBRATE")
            sendBroadcast(intent)
        }

        btnToggle.setOnClickListener {
            isPlaying = !isPlaying
            if (isPlaying) {
                btnToggle.text = "⏸ Stop Play"
                botTitle.text = "BunnyBot: RUNNING"
                btnToggle.setBackgroundColor(0xFFE53935.toInt()) // Red
                sendBroadcast(Intent("ACTION_START"))
            } else {
                btnToggle.text = "▶ Start Play"
                botTitle.text = "BunnyBot: IDLE"
                btnToggle.setBackgroundColor(0xFF4CAF50.toInt()) // Green
                sendBroadcast(Intent("ACTION_STOP"))
            }
        }

        btnClose.setOnClickListener {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}

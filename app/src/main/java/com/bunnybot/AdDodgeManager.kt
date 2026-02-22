package com.bunnybot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper

class AdDodgeManager(private val service: AccessibilityService) {

    private val handler = Handler(Looper.getMainLooper())
    private val GAME_PACKAGE = "com.bunny.runner3D.dg"

    fun triggerReset() {
        // Step 1: Open the Recent Apps screen
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        
        // Step 2: Wait 800ms for the animation to finish, then swipe the app away
        handler.postDelayed({
            swipeAppAway()
        }, 800)

        // Step 3: Wait 1.5s for the app to die, then relaunch
        handler.postDelayed({
            relaunchGame()
        }, 2300)
    }

    private fun swipeAppAway() {
        val swipePath = Path().apply {
            // Adjust these coordinates based on your phone's Recents layout
            moveTo(540f, 1200f) // Start at the middle-bottom of the card
            lineTo(540f, 200f)  // Swipe it straight up to kill it
        }

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(swipePath, 0, 300))
        
        service.dispatchGesture(gestureBuilder.build(), null, null)
    }

    private fun relaunchGame() {
        val launchIntent = service.packageManager.getLaunchIntentForPackage(GAME_PACKAGE)
        launchIntent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            service.startActivity(it)
        }
    }
}

package com.bunnybot

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

class Controller(private val service: AccessibilityService) {

    /**
     * Performs a tap at the specified coordinates
     */
    fun tap(x: Int, y: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = android.graphics.Path()
            path.moveTo(x.toFloat(), y.toFloat())
            val stroke = android.view.GestureDescription.StrokeDescription(path, 0, 50)
            val gesture = android.view.GestureDescription.Builder()
                .addStroke(stroke)
                .build()
            service.dispatchGesture(gesture, null, null)
        }
    }

    /**
     * Performs a swipe gesture to close the app
     */
    fun swipeToClose() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = android.graphics.Path()
            path.moveTo(540f, 100f)
            path.lineTo(540f, 1800f)
            val stroke = android.view.GestureDescription.StrokeDescription(path, 0, 300)
            val gesture = android.view.GestureDescription.Builder()
                .addStroke(stroke)
                .build()
            service.dispatchGesture(gesture, null, null)
        }
    }

    /**
     * Relaunches the game by clicking the app icon
     */
    fun relaunchGame() {
        Thread.sleep(500)
        tap(540, 450)
    }

    /**
     * Finds a clickable element by text and clicks it
     */
    fun clickByText(text: String): Boolean {
        val rootNode = service.rootInActiveWindow ?: return false
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        findNodesByText(rootNode, text, nodes)

        if (nodes.isNotEmpty()) {
            nodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }
        return false
    }

    /**
     * Recursively finds nodes by text
     */
    private fun findNodesByText(node: AccessibilityNodeInfo, text: String, result: MutableList<AccessibilityNodeInfo>) {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true) {
            result.add(node)
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let {
                findNodesByText(it, text, result)
            }
        }
    }
}

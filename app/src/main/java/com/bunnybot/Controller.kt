package com.bunnybot

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

class Controller(private val service: AccessibilityService) {

    /**
     * Performs a tap at the specified coordinates using accessibility service
     */
    fun tap(x: Int, y: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val path = android.graphics.Path()
                path.moveTo(x.toFloat(), y.toFloat())
                
                // Use reflection to handle GestureDescription which may not be available
                val gestureDescClass = Class.forName("android.view.GestureDescription")
                val strokeDescClass = Class.forName("android.view.GestureDescription\$StrokeDescription")
                val builderClass = Class.forName("android.view.GestureDescription\$Builder")
                
                val strokeDescConstructor = strokeDescClass.getConstructor(android.graphics.Path::class.java, Long::class.javaPrimitiveType, Long::class.javaPrimitiveType)
                val stroke = strokeDescConstructor.newInstance(path, 0L, 50L)
                
                val builderConstructor = builderClass.getConstructor()
                val builder = builderConstructor.newInstance()
                
                val addStrokeMethod = builderClass.getMethod("addStroke", strokeDescClass)
                addStrokeMethod.invoke(builder, stroke)
                
                val buildMethod = builderClass.getMethod("build")
                val gesture = buildMethod.invoke(builder)
                
                val dispatchGestureMethod = AccessibilityService::class.java.getMethod("dispatchGesture", gestureDescClass, AccessibilityService.GestureResultCallback::class.java)
                dispatchGestureMethod.invoke(service, gesture, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Performs a swipe gesture to close the app
     */
    fun swipeToClose() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val path = android.graphics.Path()
                path.moveTo(540f, 100f)
                path.lineTo(540f, 1800f)
                
                val gestureDescClass = Class.forName("android.view.GestureDescription")
                val strokeDescClass = Class.forName("android.view.GestureDescription\$StrokeDescription")
                val builderClass = Class.forName("android.view.GestureDescription\$Builder")
                
                val strokeDescConstructor = strokeDescClass.getConstructor(android.graphics.Path::class.java, Long::class.javaPrimitiveType, Long::class.javaPrimitiveType)
                val stroke = strokeDescConstructor.newInstance(path, 0L, 300L)
                
                val builderConstructor = builderClass.getConstructor()
                val builder = builderConstructor.newInstance()
                
                val addStrokeMethod = builderClass.getMethod("addStroke", strokeDescClass)
                addStrokeMethod.invoke(builder, stroke)
                
                val buildMethod = builderClass.getMethod("build")
                val gesture = buildMethod.invoke(builder)
                
                val dispatchGestureMethod = AccessibilityService::class.java.getMethod("dispatchGesture", gestureDescClass, AccessibilityService.GestureResultCallback::class.java)
                dispatchGestureMethod.invoke(service, gesture, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

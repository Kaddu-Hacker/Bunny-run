package com.bunnybot

import android.graphics.Bitmap
import kotlin.math.abs

class Vision {
    private val pathColor: Int = 0x8D6E63
    private val tolerance: Int = 5000

    /**
     * Detects the current game state from the screen bitmap
     * Returns: Pair of (state, coordinates)
     * States: "start", "win", "end", "in_game"
     */
    fun getCurrentState(screen: Bitmap): Pair<String, IntArray> {
        val width = screen.width
        val height = screen.height

        // Scan the bottom 30% of the screen for UI buttons
        val scanStartY = (height * 0.7).toInt()
        val centerX = width / 2

        for (y in scanStartY until height step 10) {
            val pixel = screen.getPixel(centerX, y)
            val color = pixel and 0xFFFFFF

            // Check for button colors
            when {
                isColorMatch(color, 0x2196F3) -> return Pair("start", intArrayOf(centerX, y))
                isColorMatch(color, 0xFFAA00) -> return Pair("win", intArrayOf(centerX, y))
                isColorMatch(color, 0xFF4444) -> return Pair("end", intArrayOf(centerX, y))
                isColorMatch(color, 0x8BC34A) -> return Pair("end", intArrayOf(centerX, y))
            }
        }

        return Pair("in_game", intArrayOf(0, 0))
    }

    /**
     * Finds the path edges in the game using color detection
     */
    fun findPathEdge(screen: Bitmap): List<IntArray> {
        val edges = mutableListOf<IntArray>()
        val width = screen.width
        val height = screen.height

        // Scan for path color in the game area (top 70% of screen)
        val scanEndY = (height * 0.7).toInt()

        for (y in 100 until scanEndY step 5) {
            for (x in 50 until width - 50 step 10) {
                val pixel = screen.getPixel(x, y)
                val color = pixel and 0xFFFFFF

                if (isColorMatch(color, pathColor)) {
                    edges.add(intArrayOf(x, y))
                }
            }
        }

        return edges
    }

    /**
     * Calibrates the path color by sampling pixels while the user plays
     */
    fun calibratePathColor(screen: Bitmap): Int {
        val colorVotes = mutableMapOf<Int, Int>()
        val width = screen.width
        val height = screen.height
        val scanEndY = (height * 0.7).toInt()

        // Sample pixels from the game area
        for (y in 100 until scanEndY step 20) {
            for (x in 50 until width - 50 step 20) {
                val pixel = screen.getPixel(x, y)
                val color = pixel and 0xFFFFFF

                // Filter out white and black pixels
                if (!isColorMatch(color, 0xFFFFFF) && !isColorMatch(color, 0x000000)) {
                    colorVotes[color] = (colorVotes[color] ?: 0) + 1
                }
            }
        }

        // Return the most frequent color
        return colorVotes.maxByOrNull { it.value }?.key ?: 0x8D6E63
    }

    /**
     * Checks if two colors match within tolerance
     */
    private fun isColorMatch(color1: Int, color2: Int): Boolean {
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF

        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF

        val diff = abs(r1 - r2) + abs(g1 - g2) + abs(b1 - b2)
        return diff < 40
    }
}

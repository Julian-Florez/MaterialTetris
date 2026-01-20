package com.myg.materialtetris.shared

import java.util.prefs.Preferences

class DesktopGameStorage : GameStorage {
    private val prefs: Preferences = Preferences.userNodeForPackage(DesktopGameStorage::class.java)

    override fun saveScore(score: Int) {
        prefs.putInt("score", score)
    }

    override fun getScore(): Int {
        return prefs.getInt("score", -1)
    }

    override fun saveBoard(boardStr: String) {
        prefs.put("board", boardStr)
    }

    override fun getBoard(): String? {
        return prefs.get("board", null)
    }

    // Window state persistence
    fun saveWindowState(width: Int, height: Int, isMaximized: Boolean, x: Int, y: Int) {
        prefs.putInt("window_width", width)
        prefs.putInt("window_height", height)
        prefs.putBoolean("window_maximized", isMaximized)
        prefs.putInt("window_x", x)
        prefs.putInt("window_y", y)
    }

    fun getWindowWidth(): Int = prefs.getInt("window_width", 500) // Default width
    fun getWindowHeight(): Int = prefs.getInt("window_height", 700) // Default height
    fun isWindowMaximized(): Boolean = prefs.getBoolean("window_maximized", false)
    fun getWindowX(): Int = prefs.getInt("window_x", -1)
    fun getWindowY(): Int = prefs.getInt("window_y", -1)
}

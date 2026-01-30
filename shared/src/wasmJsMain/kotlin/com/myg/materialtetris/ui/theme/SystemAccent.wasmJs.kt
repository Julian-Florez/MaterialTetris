package com.myg.materialtetris.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
internal actual fun getSystemAccent1Tone700(): Color {
    // Valor hardcodeado concordante con Theme.wasmJs.kt forcedAccent1
    return Color(0xFF6C3A00)
}

@Composable
internal actual fun getSystemAccent1Tone800(): Color {
    // system_accent1_800 corresponde a #FF4C2700 en Theme.wasmJs.kt
    return Color(0xFF4C2700)
}

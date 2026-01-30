package com.myg.materialtetris.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
internal actual fun getSystemAccent1Tone700(): Color {
    // getPlatformTetrisColors already maps tone 700 to color6
    return getPlatformTetrisColors().color6
}

@Composable
internal actual fun getSystemAccent1Tone800(): Color {
    // tone 800 is mapped to color7
    return getPlatformTetrisColors().color7
}

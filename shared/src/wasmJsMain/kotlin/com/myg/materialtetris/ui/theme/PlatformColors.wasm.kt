package com.myg.materialtetris.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
internal actual fun getPlatformTetrisColors(): TetrisColors {
    // Hardcoded mapping to the system_accent1 tones from the provided color string
    // Using exact hex values to ensure web matches the desired palette
    return TetrisColors(
        color1 = Color(0xFFFFB779), // system_accent1_200
        color2 = Color(0xFFFE8F00), // system_accent1_300
        color3 = Color(0xFFD87900), // system_accent1_400
        color4 = Color(0xFFB26300), // system_accent1_500
        color5 = Color(0xFF8F4E00), // system_accent1_600
        color6 = Color(0xFF6C3A00), // system_accent1_700
        color7 = Color(0xFF4C2700)  // system_accent1_800
    )
}

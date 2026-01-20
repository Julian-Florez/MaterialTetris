package com.myg.materialtetris.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class TetrisColors(
    val color1: Color,
    val color2: Color,
    val color3: Color,
    val color4: Color,
    val color5: Color,
    val color6: Color,
    val color7: Color
) {
    fun getColor(type: Int): Color {
        return when (type) {
            1 -> color1
            2 -> color2
            3 -> color3
            4 -> color4
            5 -> color5
            6 -> color6
            7 -> color7
            else -> Color.Transparent
        }
    }
}

@Composable
fun getTetrisColors(): TetrisColors {
    return getPlatformTetrisColors()
}

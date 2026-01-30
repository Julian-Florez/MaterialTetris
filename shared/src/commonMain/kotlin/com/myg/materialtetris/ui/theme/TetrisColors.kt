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
    // Obtener la paleta de la plataforma y forzar que todas las piezas usen el mismo color
    val platform = getPlatformTetrisColors()
    // Si por alguna razón platform es nulo (no debería en las actual implementaciones), usar un fallback
    val baseColor = platform?.color1 ?: Color(0xFFFFB779)
    return TetrisColors(
        color1 = baseColor,
        color2 = baseColor,
        color3 = baseColor,
        color4 = baseColor,
        color5 = baseColor,
        color6 = baseColor,
        color7 = baseColor
    )
}

package com.myg.materialtetris.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
internal actual fun getPlatformTetrisColors(): TetrisColors {
    val context = LocalContext.current
    val tones = listOf(200, 300, 400, 500, 600, 700, 800)
    val colors = tones.map {
        getColorForTone(context, it)
    }

//    return TetrisColors(
//        color1 = colors[0],
//        color2 = colors[1],
//        color3 = colors[2],
//        color4 = colors[3],
//        color5 = colors[4],
//        color6 = colors[5],
//        color7 = colors[6]
//    )
    return TetrisColors(
        color1 = colors[0].copy(alpha = 0.5f),
        color2 = colors[1].copy(alpha = 0.5f),
        color3 = colors[2].copy(alpha = 0.5f),
        color4 = colors[3].copy(alpha = 0.5f),
        color5 = colors[4].copy(alpha = 0.5f),
        color6 = colors[5].copy(alpha = 0.5f),
        color7 = colors[6].copy(alpha = 0.5f)
    )
}

private fun getColorForTone(context: Context, tone: Int): Color {
    val colorId = context.resources.getIdentifier("system_accent1_$tone", "color", "android")
    return if (colorId != 0) {
        Color(ContextCompat.getColor(context, colorId))
    } else {
        // Fallback for older Android versions or if the color is not found
        val defaultShade = (tone / 100) * 0.1f
        Color.Gray.copy(alpha = defaultShade)
    }
}

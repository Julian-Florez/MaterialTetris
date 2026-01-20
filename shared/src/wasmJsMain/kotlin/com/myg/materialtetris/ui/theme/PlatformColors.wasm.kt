package com.myg.materialtetris.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.lerp

@Composable
internal actual fun getPlatformTetrisColors(): TetrisColors {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surface = MaterialTheme.colorScheme.surface

    return TetrisColors(
        // Mix tertiary with surface for the first two
        color1 = lerp(tertiary, surface, 0.95f),
        color2 = lerp(tertiary, surface, 0.90f),
        // Mix primary with surface for the rest
        color3 = lerp(primary, surface, 0.80f),
        color4 = lerp(primary, surface, 0.70f),
        color5 = lerp(primary, surface, 0.60f),
        color6 = lerp(primary, surface, 0.50f),
        color7 = lerp(primary, surface, 0.40f)
    )
}

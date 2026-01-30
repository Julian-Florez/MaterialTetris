package com.myg.materialtetris.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Composable
internal actual fun getPlatformTetrisColors(): TetrisColors {
    // Intentar usar colores cargados por Theme.wasmJs (cachedColors) si están disponibles
    val colors = cachedColors

    val colorSchemePrimary: Color
    val colorSchemeSecondary: Color
    val colorSchemeTertiary: Color
    val surface: Color

    if (colors != null && colors.isNotEmpty()) {
        colorSchemePrimary = colors["system_accent1_600"] ?: colors["primary"] ?: MaterialTheme.colorScheme.primary
        colorSchemeSecondary = colors["system_accent1_500"] ?: colors["secondary"] ?: MaterialTheme.colorScheme.secondary
        colorSchemeTertiary = colors["tertiary"] ?: MaterialTheme.colorScheme.tertiary
        surface = colors["surface"] ?: MaterialTheme.colorScheme.surface
    } else {
        colorSchemePrimary = MaterialTheme.colorScheme.primary
        colorSchemeSecondary = MaterialTheme.colorScheme.secondary
        colorSchemeTertiary = MaterialTheme.colorScheme.tertiary
        surface = MaterialTheme.colorScheme.surface
    }

    return TetrisColors(
        // Mix tertiary with surface for the first two
        color1 = lerp(colorSchemeTertiary, surface, 0.95f),
        color2 = lerp(colorSchemeTertiary, surface, 0.90f),
        // Mix primary with surface for the rest
        color3 = lerp(colorSchemePrimary, surface, 0.80f),
        color4 = lerp(colorSchemePrimary, surface, 0.70f),
        color5 = lerp(colorSchemeSecondary, surface, 0.60f),
        color6 = lerp(colorSchemeSecondary, surface, 0.50f),
        color7 = lerp(colorSchemePrimary, surface, 0.40f)
    )
}

package com.myg.materialtetris.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import java.io.File
import java.lang.Long

@Composable
actual fun getPlatformColorScheme(darkTheme: Boolean): ColorScheme? {
    return remember(darkTheme) {
        loadCustomColorScheme()
    }
}

private fun parseColorsFromFile(file: File): Map<String, Color> {
    val colors = mutableMapOf<String, Color>()
    if (!file.exists()) return colors
    
    val lines = file.readLines()
    var isTable = false
    
    for (line in lines) {
        val trimmedLine = line.trim()
        if (trimmedLine.isEmpty()) continue
        
        if (trimmedLine.startsWith("--- System Colors Table ---")) {
            isTable = true
            continue
        }
        
        if (trimmedLine.startsWith("---")) continue 

        if (!isTable) {
            // Support both single "key=value" per line and multiple pairs on the same line separated by whitespace
            // e.g. "primary=#FFFFA44E onPrimary=#FF552C00 primaryContainer=#FFFE8F00"
            val tokens = trimmedLine.split(Regex("\\s+"))
            for (token in tokens) {
                if (token.isBlank()) continue
                val kv = token.split("=")
                if (kv.size == 2) {
                    val key = kv[0].trim()
                    val value = kv[1].trim()
                    // Ignore malformed values
                    if (key.isNotEmpty() && value.isNotEmpty()) {
                        colors[key] = parseColor(value)
                    }
                } else if (kv.size > 2) {
                    // Rare case: value contains '=' (unlikely for colors), join the rest back
                    val key = kv[0].trim()
                    val value = kv.subList(1, kv.size).joinToString("=").trim()
                    if (key.isNotEmpty() && value.isNotEmpty()) {
                        colors[key] = parseColor(value)
                    }
                }
            }
        } else {
            // Table parsing
            if (trimmedLine.startsWith("Tone")) continue // Header
            val parts = trimmedLine.split(",")
            if (parts.size >= 2) {
                val tone = parts[0].trim()
                // Accent1 is index 1
                if (parts.size > 1) {
                    colors["system_accent1_$tone"] = parseColor(parts[1].trim())
                }
                // Also parse additional accents and neutrals if present
                if (parts.size > 2) colors["system_accent2_$tone"] = parseColor(parts[2].trim())
                if (parts.size > 3) colors["system_accent3_$tone"] = parseColor(parts[3].trim())
                if (parts.size > 4) colors["system_neutral1_$tone"] = parseColor(parts[4].trim())
                if (parts.size > 5) colors["system_neutral2_$tone"] = parseColor(parts[5].trim())
            }
        }
    }
    return colors
}

private fun loadCustomColorScheme(): ColorScheme? {
    try {
        val file = File("C:\\Program Files\\MaterialYouWindows\\colors")
        val colors = parseColorsFromFile(file)
            
        if (colors.isEmpty()) return null

        return ColorScheme(
            primary = colors["system_accent1_600"] ?: colors["primary"] ?: Color.Magenta,
            onPrimary = colors["onPrimary"] ?: Color.Magenta,
            primaryContainer = colors["primaryContainer"] ?: Color.Magenta,
            onPrimaryContainer = colors["onPrimaryContainer"] ?: Color.Magenta,
            inversePrimary = colors["inversePrimary"] ?: Color.Magenta,
            secondary = colors["system_accent1_500"] ?: colors["secondary"] ?: Color.Magenta,
            onSecondary = colors["onSecondary"] ?: Color.Magenta,
            secondaryContainer = colors["secondaryContainer"] ?: Color.Magenta,
            onSecondaryContainer = colors["onSecondaryContainer"] ?: Color.Magenta,
            tertiary = colors["tertiary"] ?: Color.Magenta,
            onTertiary = colors["onTertiary"] ?: Color.Magenta,
            tertiaryContainer = colors["tertiaryContainer"] ?: Color.Magenta,
            onTertiaryContainer = colors["onTertiaryContainer"] ?: Color.Magenta,
            background = colors["background"] ?: Color.Magenta,
            onBackground = colors["onBackground"] ?: Color.Magenta,
            surface = colors["surface"] ?: Color.Magenta,
            onSurface = colors["onSurface"] ?: Color.Magenta,
            surfaceVariant = colors["surfaceVariant"] ?: Color.Magenta,
            onSurfaceVariant = colors["onSurfaceVariant"] ?: Color.Magenta,
            surfaceTint = colors["surfaceTint"] ?: colors["primary"] ?: Color.Magenta,
            inverseSurface = colors["inverseSurface"] ?: Color.Magenta,
            inverseOnSurface = colors["inverseOnSurface"] ?: Color.Magenta,
            error = colors["error"] ?: Color.Magenta,
            onError = colors["onError"] ?: Color.Magenta,
            errorContainer = colors["errorContainer"] ?: Color.Magenta,
            onErrorContainer = colors["onErrorContainer"] ?: Color.Magenta,
            outline = colors["outline"] ?: Color.Magenta,
            outlineVariant = colors["outlineVariant"] ?: Color.Magenta,
            scrim = colors["scrim"] ?: Color.Magenta,
            surfaceBright = colors["surfaceBright"] ?: colors["surface"] ?: Color.Magenta,
            surfaceDim = colors["surfaceDim"] ?: colors["surface"] ?: Color.Magenta,
            surfaceContainer = colors["surfaceContainer"] ?: colors["surface"] ?: Color.Magenta,
            surfaceContainerHigh = colors["surfaceContainerHigh"] ?: colors["surface"] ?: Color.Magenta,
            surfaceContainerHighest = colors["surfaceContainerHighest"] ?: colors["surface"] ?: Color.Magenta,
            surfaceContainerLow = colors["surfaceContainerLow"] ?: colors["surface"] ?: Color.Magenta,
            surfaceContainerLowest = colors["surfaceContainerLowest"] ?: colors["surface"] ?: Color.Magenta,
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

private fun parseColor(colorString: String): Color {
    return try {
        val hex = colorString.removePrefix("#")
        val longValue = Long.parseLong(hex, 16)
        // Handle 8-digit hex (ARGB) or 6-digit hex (RGB)
        if (hex.length == 8) {
            Color(longValue)
        } else if (hex.length == 6) {
            Color(longValue or 0xFF000000)
        } else {
            Color.Magenta
        }
    } catch (ex: Exception) {
        Color.Magenta 
    }
}

@Composable
actual fun getPlatformTetrisColors(): TetrisColors {
    val colorScheme = MaterialTheme.colorScheme
    val file = File("C:\\Program Files\\MaterialYouWindows\\colors")
    val colors = parseColorsFromFile(file)

    return if (colors.isNotEmpty()) {
        val accent200 = colors["system_accent1_200"] ?: colorScheme.primaryContainer
        val accent300 = colors["system_accent1_300"] ?: colorScheme.primary
        val accent400 = colors["system_accent1_400"] ?: colorScheme.primary
        val accent500 = colors["system_accent1_500"] ?: colorScheme.primary
        val accent600 = colors["system_accent1_600"] ?: colorScheme.primary
        val accent700 = colors["system_accent1_700"] ?: colorScheme.primary
        val accent800 = colors["system_accent1_800"] ?: colorScheme.primary

        // Simple strategy: use tertiary for first two (light blend), then accents for the rest
        return TetrisColors(
            color1 = accent200,
            color2 = accent300,
            color3 = accent400,
            color4 = accent500,
            color5 = accent600,
            color6 = accent700,
            color7 = accent800
        )
    } else {
        // Fallback to default theme-based tetris colors
        val primary = colorScheme.primary
        val tertiary = colorScheme.tertiary
        val surface = colorScheme.surface
        return TetrisColors(
            color1 = tertiary,
            color2 = tertiary,
            color3 = primary,
            color4 = primary,
            color5 = primary,
            color6 = primary,
            color7 = surface
        )
    }
}

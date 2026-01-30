package com.myg.materialtetris.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.Response

// Variable global para almacenar los colores cargados (visibilidad interna para potencial reuso)
internal var cachedColors: Map<String, Color>? = null
private var colorsLoaded = false

@Composable
internal actual fun getPlatformColorScheme(darkTheme: Boolean): ColorScheme? {
    var colorScheme by remember { mutableStateOf<ColorScheme?>(loadCustomColorScheme()) }

    LaunchedEffect(darkTheme) {
        if (cachedColors == null && !colorsLoaded) {
            try {
                // Usar ruta relativa basada en la ubicación actual para soportar subdirectorios (ej: GitHub Pages)
                val basePath = window.location.pathname.substringBeforeLast("/") + "/"
                // Primero intentar 'colors' (sin extensión) en la ruta base, luego fallback a colors.txt por compatibilidad
                val candidate1 = if (basePath == "/") "colors" else "${basePath}colors"
                val candidate2 = if (basePath == "/") "colors.txt" else "${basePath}colors.txt"

                var response: Response? = null
                try {
                    response = window.fetch(candidate1).await<Response>()
                } catch (_: Throwable) {
                    // ignore, try fallback
                }
                if (response == null || !response.ok) {
                    try {
                        response = window.fetch(candidate2).await<Response>()
                    } catch (_: Throwable) {
                        // ignore
                    }
                }

                if (response != null && response.ok) {
                    val content = response.text().await<JsString>().toString()
                    val colors = parseColorsFromString(content)
                    cachedColors = colors
                    colorsLoaded = true
                    // Guardar en localStorage para uso futuro
                    if (content.isNotEmpty()) {
                        localStorage.setItem("materialtetris_colors", content)
                    }
                    colorScheme = buildColorScheme(colors)
                }
            } catch (e: Exception) {
                colorsLoaded = true
                // En caso de error, usar localStorage
                colorScheme = loadCustomColorScheme()
            }
        } else if (cachedColors != null) {
            colorScheme = buildColorScheme(cachedColors!!)
        }
    }

    return colorScheme
}

private fun parseColorsFromString(content: String): Map<String, Color> {
    val colors = mutableMapOf<String, Color>()
    if (content.isEmpty()) return colors

    val lines = content.split("\n")
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
            // Support single or multiple pairs on the same line separated by whitespace
            val tokens = trimmedLine.split(Regex("\\s+"))
            for (token in tokens) {
                if (token.isBlank()) continue
                val kv = token.split("=")
                if (kv.size == 2) {
                    colors[kv[0].trim()] = parseColor(kv[1].trim())
                } else if (kv.size > 2) {
                    val key = kv[0].trim()
                    val value = kv.subList(1, kv.size).joinToString("=").trim()
                    colors[key] = parseColor(value)
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
                if (parts.size > 2) colors["system_accent2_$tone"] = parseColor(parts[2].trim())
                if (parts.size > 3) colors["system_accent3_$tone"] = parseColor(parts[3].trim())
                if (parts.size > 4) colors["system_neutral1_$tone"] = parseColor(parts[4].trim())
                if (parts.size > 5) colors["system_neutral2_$tone"] = parseColor(parts[5].trim())
            }
        }
    }
    return colors
}


private fun buildColorScheme(colors: Map<String, Color>): ColorScheme? {
    if (colors.isEmpty()) return null

    return ColorScheme(
        primary = colors["primary"] ?: Color.Magenta,
        onPrimary = colors["onPrimary"] ?: Color.Magenta,
        primaryContainer = colors["primaryContainer"] ?: Color.Magenta,
        onPrimaryContainer = colors["onPrimaryContainer"] ?: Color.Magenta,
        inversePrimary = colors["inversePrimary"] ?: Color.Magenta,
        secondary = colors["secondary"] ?: Color.Magenta,
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
}

private fun loadCustomColorScheme(): ColorScheme? {
    return try {
        // Cargar colores desde localStorage con la clave "materialtetris_colors"
        val content = localStorage.getItem("materialtetris_colors") ?: return null
        val colors = parseColorsFromString(content)
        buildColorScheme(colors)
    } catch (e: Exception) {
        console.error("Error loading color scheme: ${e.message}")
        null
    }
}

private fun parseColor(colorString: String): Color {
    return try {
        val hex = colorString.removePrefix("#")
        val longValue = hex.toLong(16)
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

// Función auxiliar para guardar colores desde JavaScript
fun saveColors(colorsContent: String) {
    localStorage.setItem("materialtetris_colors", colorsContent)
}

// Función auxiliar para obtener colores guardados
fun getColors(): String? {
    return localStorage.getItem("materialtetris_colors")
}

// Objeto externo para console.error
private external object console {
    fun error(message: String)
}

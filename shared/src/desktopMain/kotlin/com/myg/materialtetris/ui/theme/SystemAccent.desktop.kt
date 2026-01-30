package com.myg.materialtetris.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.io.File

@Composable
actual fun getSystemAccent1Tone700(): Color {
    try {
        val file = File("C:\\Program Files\\MaterialYouWindows\\colors")
        if (file.exists()) {
            val lines = file.readLines()

            // First, try to find explicit key=value tokens in the file
            for (line in lines) {
                if (line.contains("system_accent1_700")) {
                    val tokens = line.split(Regex("\\s+"))
                    for (token in tokens) {
                        val kv = token.split("=")
                        if (kv.size == 2 && kv[0].trim() == "system_accent1_700") {
                            val hex = kv[1].trim().removePrefix("#")
                            val longValue = hex.toLongOrNull(16) ?: continue
                            return if (hex.length == 8) Color(longValue) else Color(longValue or 0xFF000000)
                        }
                    }
                }
            }

            // If key=value not found, try to parse the System Colors Table format
            // Find the table start
            var inTable = false
            var headerCols: List<String>? = null
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("--- System Colors Table ---")) {
                    inTable = true
                    continue
                }
                if (!inTable) continue
                if (trimmed.isEmpty()) continue

                // header line: Tone,Accent1,Accent2,Accent3,Neutral1,Neutral2
                if (headerCols == null && trimmed.startsWith("Tone")) {
                    headerCols = trimmed.split(',').map { it.trim() }
                    continue
                }

                // data rows: toneValue,#hex,...
                if (headerCols != null) {
                    val parts = trimmed.split(',')
                    if (parts.size >= 2) {
                        val tone = parts[0].trim()
                        if (tone == "700") {
                            val accent1 = parts[1].trim().removePrefix("#")
                            val longValue = accent1.toLongOrNull(16) ?: break
                            return if (accent1.length == 8) Color(longValue) else Color(longValue or 0xFF000000)
                        }
                    }
                }
            }
        }
    } catch (_: Exception) { }

    // fallback to theme primary
    return MaterialTheme.colorScheme.primary
}

@Composable
actual fun getSystemAccent1Tone800(): Color {
    try {
        val file = File("C:\\Program Files\\MaterialYouWindows\\colors")
        if (file.exists()) {
            val lines = file.readLines()

            // First, try to find explicit key=value tokens in the file
            for (line in lines) {
                if (line.contains("system_accent1_800")) {
                    val tokens = line.split(Regex("\\s+"))
                    for (token in tokens) {
                        val kv = token.split("=")
                        if (kv.size == 2 && kv[0].trim() == "system_accent1_800") {
                            val hex = kv[1].trim().removePrefix("#")
                            val longValue = hex.toLongOrNull(16) ?: continue
                            return if (hex.length == 8) Color(longValue) else Color(longValue or 0xFF000000)
                        }
                    }
                }
            }

            // Table parsing fallback
            var inTable = false
            var headerCols: List<String>? = null
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("--- System Colors Table ---")) {
                    inTable = true
                    continue
                }
                if (!inTable) continue
                if (trimmed.isEmpty()) continue

                if (headerCols == null && trimmed.startsWith("Tone")) {
                    headerCols = trimmed.split(',').map { it.trim() }
                    continue
                }

                if (headerCols != null) {
                    val parts = trimmed.split(',')
                    if (parts.size >= 2) {
                        val tone = parts[0].trim()
                        if (tone == "800") {
                            val accent1 = parts[1].trim().removePrefix("#")
                            val longValue = accent1.toLongOrNull(16) ?: break
                            return if (accent1.length == 8) Color(longValue) else Color(longValue or 0xFF000000)
                        }
                    }
                }
            }
        }
    } catch (_: Exception) { }

    return MaterialTheme.colorScheme.primary
}

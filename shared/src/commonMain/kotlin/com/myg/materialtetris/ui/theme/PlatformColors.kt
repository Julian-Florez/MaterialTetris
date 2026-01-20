package com.myg.materialtetris.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
expect fun getPlatformColorScheme(darkTheme: Boolean): ColorScheme?
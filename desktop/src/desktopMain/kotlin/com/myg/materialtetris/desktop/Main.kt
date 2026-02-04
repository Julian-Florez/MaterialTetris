package com.myg.materialtetris.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.myg.materialtetris.model.Direction
import com.myg.materialtetris.shared.DesktopGameStorage
import com.myg.materialtetris.ui.GameScreen
import com.myg.materialtetris.ui.theme.MaterialTetrisTheme
import com.myg.materialtetris.viewmodel.GameViewModel
import java.awt.Dimension

fun main() = application {
    val storage = DesktopGameStorage()
    val viewModel = GameViewModel(storage)
    
    // Safety for missing icon
    val icon = if (Thread.currentThread().contextClassLoader?.getResource("icon.png") != null) {
        painterResource("icon.png")
    } else {
        null
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "MaterialTetris",
        state = rememberWindowState(width = 500.dp, height = 700.dp),
        icon = icon,
        onKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown) {
                when (event.key) {
                    Key.DirectionUp -> { viewModel.onMove(Direction.UP); true }
                    Key.DirectionDown -> { viewModel.onMove(Direction.DOWN); true }
                    Key.DirectionLeft -> { viewModel.onMove(Direction.LEFT); true }
                    Key.DirectionRight -> { viewModel.onMove(Direction.RIGHT); true }
                    Key.R -> { viewModel.restartGame(); true }
                    else -> false
                }
            } else {
                false
            }
        }
    ) {
        val density = LocalDensity.current
        
        LaunchedEffect(density) {
            window.minimumSize = Dimension(400, 550)
            window.maximumSize = Dimension(1200, 1000)
        }

        MaterialTetrisTheme {
            val backgroundColor = MaterialTheme.colorScheme.background
            LaunchedEffect(backgroundColor) {
                val argb = backgroundColor.toArgb()
                window.background = java.awt.Color(argb, true)
            }
            
            GameScreen(gameViewModel = viewModel)
        }
    }
}

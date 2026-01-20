package com.myg.materialtetris.web

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.window.CanvasBasedWindow
import com.myg.materialtetris.model.Direction
import com.myg.materialtetris.shared.WasmGameStorage
import com.myg.materialtetris.ui.GameScreen
import com.myg.materialtetris.ui.theme.MaterialTetrisTheme
import com.myg.materialtetris.viewmodel.GameViewModel

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val storage = WasmGameStorage()
    val viewModel = GameViewModel(storage)

    CanvasBasedWindow(canvasElementId = "ComposeTarget", title = "Material Tetris") {
        MaterialTetrisTheme {
            GameScreen(
                gameViewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.DirectionUp, Key.W -> { viewModel.onMove(Direction.UP); true }
                                Key.DirectionDown, Key.S -> { viewModel.onMove(Direction.DOWN); true }
                                Key.DirectionLeft, Key.A -> { viewModel.onMove(Direction.LEFT); true }
                                Key.DirectionRight, Key.D -> { viewModel.onMove(Direction.RIGHT); true }
                                Key.R -> { viewModel.restartGame(); true }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
            )
        }
    }
}


package com.myg.materialtetris.web

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.CanvasBasedWindow
import com.myg.materialtetris.model.Direction
import com.myg.materialtetris.shared.WasmGameStorage
import com.myg.materialtetris.ui.GameScreen
import com.myg.materialtetris.ui.theme.MaterialTetrisTheme
import com.myg.materialtetris.viewmodel.GameViewModel
import kotlinx.browser.window
import org.w3c.dom.events.KeyboardEvent

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val storage = WasmGameStorage()
    val viewModel = GameViewModel(storage)

    // Global key handler for browser to capture arrow keys and WASD
    window.addEventListener("keydown", { ev ->
        val ke = ev as KeyboardEvent
        when (ke.key) {
            "ArrowUp", "w", "W" -> {
                ke.preventDefault()
                viewModel.onMove(Direction.UP)
            }
            "ArrowDown", "s", "S" -> {
                ke.preventDefault()
                viewModel.onMove(Direction.DOWN)
            }
            "ArrowLeft", "a", "A" -> {
                ke.preventDefault()
                viewModel.onMove(Direction.LEFT)
            }
            "ArrowRight", "d", "D" -> {
                ke.preventDefault()
                viewModel.onMove(Direction.RIGHT)
            }
            "r", "R" -> {
                ke.preventDefault()
                viewModel.restartGame()
            }
            else -> {}
        }
    })

    CanvasBasedWindow(canvasElementId = "ComposeTarget", title = "MaterialTetris") {
        MaterialTetrisTheme {
            GameScreen(
                gameViewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

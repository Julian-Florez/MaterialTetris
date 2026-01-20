package com.myg.materialtetris

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.myg.materialtetris.shared.AndroidGameStorage
import com.myg.materialtetris.ui.GameScreen
import com.myg.materialtetris.ui.theme.MaterialTetrisTheme
import com.myg.materialtetris.viewmodel.GameViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return GameViewModel(AndroidGameStorage(applicationContext)) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
    
    private var isMenuVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    isMenuVisible = state.isGameOver
                }
            }
        }

        setContent {
            MaterialTetrisTheme {
                val backgroundColor = MaterialTheme.colorScheme.background
                val view = LocalView.current
                LaunchedEffect(backgroundColor) {
                    val window = (view.context as Activity).window
                    window.setBackgroundDrawable(ColorDrawable(backgroundColor.toArgb()))
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = backgroundColor
                ) {
                    GameScreen(gameViewModel = viewModel)
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return super.onKeyDown(keyCode, event)
        }

        if (isMenuVisible) {
            return super.onKeyDown(keyCode, event)
        }

        return when (keyCode) {
            KeyEvent.KEYCODE_R,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_CTRL_LEFT,
            KeyEvent.KEYCODE_CTRL_RIGHT -> {
                viewModel.restartGame()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                viewModel.rotatePiece()
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                viewModel.movePiece(0, 1)
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                viewModel.movePiece(-1, 0)
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                viewModel.movePiece(1, 0)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            viewModel.restartGame()
            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }
}

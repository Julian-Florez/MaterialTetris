package com.myg.materialtetris.model

import androidx.compose.runtime.Stable

private var tileIdCounter = 0

/**
 * Represents a single block on the game board.
 */
@Stable
data class Tile(
    val type: Int, // 0 for empty, >0 for a color/type of tetromino
    val id: Int = ++tileIdCounter
)

/**
 * Represents the complete state of the UI.
 */
@Stable
data class GameUiState(
    val board: List<List<Tile?>> = emptyList(),
    val activePiece: Tetromino? = null,
    val nextPiece: Tetromino? = null,
    val score: Int = 0,
    val highScore: Int = 0,
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false,
    val currentSpeed: Long = 160L
)

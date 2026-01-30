package com.myg.materialtetris.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myg.materialtetris.model.GameUiState
import com.myg.materialtetris.model.PieceIdProvider
import com.myg.materialtetris.model.Tetromino
import com.myg.materialtetris.shared.GameStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val BOARD_WIDTH = 15
const val BOARD_HEIGHT = 15
const val INITIAL_GAME_SPEED = 500L

class GameViewModel(private val storage: GameStorage) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var gameJob: Job? = null
    private var board = Array(BOARD_HEIGHT) { Array(BOARD_WIDTH) { 0 } }

    init {
        loadHighScore()
        startNewGame()
    }

    private fun loadHighScore() {
        val savedScore = storage.getScore()
        _uiState.update { it.copy(highScore = savedScore) }
    }

    private fun saveHighScore() {
        if (_uiState.value.score > _uiState.value.highScore) {
            storage.saveScore(_uiState.value.score)
            _uiState.update { it.copy(highScore = _uiState.value.score) }
        }
    }

    fun startNewGame() {
        gameJob?.cancel()
        board = Array(BOARD_HEIGHT) { Array(BOARD_WIDTH) { 0 } }
        // Delay the appearance of the first piece slightly
        viewModelScope.launch {
            delay(100)
            val firstPiece = createRandomPiece()
            val secondPiece = createRandomPiece()
            _uiState.update {
                it.copy(
                    board = board.map { row -> row.map { com.myg.materialtetris.model.Tile(it) } },
                    activePiece = firstPiece,
                    nextPiece = secondPiece,
                    score = 0,
                    isGameOver = false,
                    isPaused = false
                )
            }
            startGameLoop()
        }
    }

    private fun startGameLoop() {
        gameJob = viewModelScope.launch {
            while (!_uiState.value.isGameOver) {
                delay(INITIAL_GAME_SPEED)
                movePiece(0, 1)
            }
        }
    }

    fun restartGame() {
        startNewGame()
    }

    private fun createRandomPiece(): Tetromino {
        val type = (1..7).random()
        val shape = getTetrominoShape(type)
        return Tetromino(PieceIdProvider.next(), type, shape, BOARD_WIDTH / 2 - 1, -2)
    }

    fun movePiece(dx: Int, dy: Int) {
        _uiState.value.activePiece?.let { piece ->
            if (isValidPosition(piece, dx, dy)) {
                val newPiece = piece.copy(x = piece.x + dx, y = piece.y + dy)
                _uiState.update { it.copy(activePiece = newPiece) }
            } else if (dy > 0) {
                lockPiece()
            }
        }
    }

    fun rotatePiece() {
        _uiState.value.activePiece?.let { piece ->
            val rotatedShape = Array(piece.shape[0].size) { r ->
                Array(piece.shape.size) { c ->
                    piece.shape[piece.shape.size - 1 - c][r]
                }
            }
            var rotatedPiece = piece.copy(
                shape = rotatedShape,
                rotation = (piece.rotation + 1) % 4
            )

            // Wall kick logic
            val offsets = listOf(0, -1, 1, -2, 2)
            for (offset in offsets) {
                if (isValidPosition(rotatedPiece, offset, 0)) {
                    rotatedPiece = rotatedPiece.copy(x = rotatedPiece.x + offset)
                    _uiState.update { it.copy(activePiece = rotatedPiece) }
                    return@let
                }
            }
        }
    }

    private fun isValidPosition(piece: Tetromino, dx: Int, dy: Int): Boolean {
        for (r in piece.shape.indices) {
            for (c in piece.shape[r].indices) {
                if (piece.shape[r][c] != 0) {
                    val newRow = piece.y + r + dy
                    val newCol = piece.x + c + dx

                    if (newCol !in 0..<BOARD_WIDTH || newRow >= BOARD_HEIGHT) {
                        return false
                    }
                    if (newRow >= 0 && board[newRow][newCol] != 0) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun lockPiece() {
        val pieceToLock = _uiState.value.activePiece ?: return

        // Check for game over condition
        for (r in pieceToLock.shape.indices) {
            for (c in pieceToLock.shape[r].indices) {
                if (pieceToLock.shape[r][c] != 0 && pieceToLock.y + r < 0) {
                    gameOver()
                    return
                }
            }
        }

        // Lock the piece onto the board
        for (r in pieceToLock.shape.indices) {
            for (c in pieceToLock.shape[r].indices) {
                if (pieceToLock.shape[r][c] != 0) {
                    val row = pieceToLock.y + r
                    val col = pieceToLock.x + c
                    if (row >= 0 && row < BOARD_HEIGHT && col >= 0 && col < BOARD_WIDTH) {
                        board[row][col] = pieceToLock.type
                    }
                }
            }
        }

        // Clear lines and update board
        clearLines()
        updateBoardState()

        // Make the current piece disappear
        _uiState.update { it.copy(activePiece = null) }

        // After a delay, spawn the next piece
        viewModelScope.launch {
            delay(500)
            spawnNewPiece()
        }
    }

    private fun spawnNewPiece() {
        val nextPiece = _uiState.value.nextPiece ?: createRandomPiece()
        if (!isValidPosition(nextPiece, 0, 0)) {
            gameOver()
            return
        }
        _uiState.update {
            it.copy(
                activePiece = nextPiece,
                nextPiece = createRandomPiece()
            )
        }
    }

    private fun clearLines() {
        val newBoardWithoutFullLines = board.filter { row -> row.any { it == 0 } }
        val linesCleared = BOARD_HEIGHT - newBoardWithoutFullLines.size

        if (linesCleared > 0) {
            val newBoard = List(linesCleared) { Array(BOARD_WIDTH) { 0 } } + newBoardWithoutFullLines
            board = newBoard.toTypedArray()
            val newScore = _uiState.value.score + linesCleared * 100
            _uiState.update { it.copy(score = newScore) }
        }
    }

    private fun updateBoardState() {
        _uiState.update { it.copy(board = board.map { row -> row.map { com.myg.materialtetris.model.Tile(it) } }) }
    }

    private fun gameOver() {
        gameJob?.cancel()
        _uiState.update { it.copy(isGameOver = true) }
        saveHighScore()
    }

    private fun getTetrominoShape(type: Int): Array<Array<Int>> {
        return when (type) {
            1 -> arrayOf(arrayOf(1, 1, 1, 1)) // I
            2 -> arrayOf(arrayOf(1, 1), arrayOf(1, 1)) // O
            3 -> arrayOf(arrayOf(0, 1, 0), arrayOf(1, 1, 1)) // T
            4 -> arrayOf(arrayOf(0, 1, 1), arrayOf(1, 1, 0)) // S
            5 -> arrayOf(arrayOf(1, 1, 0), arrayOf(0, 1, 1)) // Z
            6 -> arrayOf(arrayOf(1, 0, 0), arrayOf(1, 1, 1)) // J
            7 -> arrayOf(arrayOf(0, 0, 1), arrayOf(1, 1, 1)) // L
            else -> arrayOf(arrayOf())
        }
    }

    // Expose a simple API for desktop/web main functions to trigger moves
    fun onMove(direction: com.myg.materialtetris.model.Direction) {
        when (direction) {
            com.myg.materialtetris.model.Direction.UP -> rotatePiece()
            com.myg.materialtetris.model.Direction.DOWN -> movePiece(0, 1)
            com.myg.materialtetris.model.Direction.LEFT -> movePiece(-1, 0)
            com.myg.materialtetris.model.Direction.RIGHT -> movePiece(1, 0)
        }
    }
}

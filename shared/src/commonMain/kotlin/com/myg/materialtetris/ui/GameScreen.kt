package com.myg.materialtetris.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myg.materialtetris.viewmodel.GameViewModel
import kotlin.math.abs

@Composable
fun GameScreen(
    gameViewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by gameViewModel.uiState.collectAsState()
    var accumulatedDragX by remember { mutableStateOf(0f) }
    var accumulatedDragY by remember { mutableStateOf(0f) }
    var canRotate by remember { mutableStateOf(true) }
    var isDraggingHorizontally by remember { mutableStateOf<Boolean?>(null) }

    val gameModifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = {
                accumulatedDragX = 0f
                accumulatedDragY = 0f
                canRotate = true
                isDraggingHorizontally = null
            },
            onDrag = { change, dragAmount ->
                change.consume()
                accumulatedDragX += dragAmount.x
                accumulatedDragY += dragAmount.y

                if (isDraggingHorizontally == null) {
                    val threshold = 20f
                    if (abs(accumulatedDragX) > threshold || abs(accumulatedDragY) > threshold) {
                        isDraggingHorizontally = abs(accumulatedDragX) > abs(accumulatedDragY)
                    }
                }

                if (isDraggingHorizontally == true) {
                    if (abs(accumulatedDragX) > 40f) {
                        gameViewModel.movePiece(if (accumulatedDragX > 0) 1 else -1, 0)
                        accumulatedDragX = 0f
                    }
                } else if (isDraggingHorizontally == false) {
                    if (accumulatedDragY > 40f) {
                        gameViewModel.movePiece(0, 1)
                        accumulatedDragY = 0f
                    } else if (accumulatedDragY < -40f && canRotate) {
                        gameViewModel.rotatePiece()
                        canRotate = false
                    }
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.then(gameModifier)
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isLandscape = screenWidth > screenHeight
            
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(180.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ScoreCard(score = uiState.score, highScore = uiState.highScore)
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Row {
                            ActionButton(
                                onClick = { gameViewModel.restartGame() },
                                icon = Icons.Default.Refresh,
                                contentDescription = "Restart"
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            NextPiece(piece = uiState.nextPiece)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(32.dp))
                    
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        GameBoardWithOverlay(uiState, gameViewModel)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    
                    val contentWidth = screenWidth - 32.dp
                    
                    Column(
                        modifier = Modifier.width(contentWidth),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom 
                        ) {
                            ScoreCard(score = uiState.score, highScore = uiState.highScore)
                            
                            Row {
                                ActionButton(
                                    onClick = { gameViewModel.restartGame() },
                                    icon = Icons.Default.Refresh,
                                    contentDescription = "Restart"
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                NextPiece(piece = uiState.nextPiece)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            GameBoardWithOverlay(uiState, gameViewModel)
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun GameBoardWithOverlay(uiState: com.myg.materialtetris.model.GameUiState, gameViewModel: GameViewModel) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier.wrapContentSize()
    ) {
        val boardSize = if (maxWidth < maxHeight) maxWidth else maxHeight
        val cornerRadius = boardSize * 0.05f
        
        Box(modifier = Modifier.size(boardSize)) {
            val tetrisBoard: Array<Array<Int>> = if (uiState.board.isNotEmpty() && uiState.board[0].isNotEmpty()) {
                Array(uiState.board.size) { r ->
                    Array(uiState.board[r].size) { c ->
                        val tile = uiState.board[r][c]
                        if (tile != null) tile.type else 0
                    }
                }
            } else {
                arrayOf(arrayOf(0))
            }
            GameBoard(
                board = tetrisBoard,
                activePiece = uiState.activePiece,
                isGameOver = uiState.isGameOver,
                modifier = Modifier.fillMaxSize()
            )

            if (uiState.isGameOver) {
                GameOverOverlay(onRestart = { gameViewModel.restartGame() }, cornerRadius = cornerRadius)
            }
        }
    }
}

@Composable
fun ScoreCard(score: Int, highScore: Int) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val highScoreColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "High Score",
                    tint = highScoreColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = highScore.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = highScoreColor
                )
            }
            Text(
                text = score.toString(), 
                style = MaterialTheme.typography.displaySmall, 
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun ActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun GameOverOverlay(onRestart: () -> Unit, cornerRadius: Dp = 0.dp) {
    Overlay(
        icon = Icons.Default.SentimentVeryDissatisfied,
        buttonIcon = Icons.Default.Refresh,
        onClick = onRestart,
        cornerRadius = cornerRadius
    )
}

@Composable
private fun Overlay(icon: ImageVector, buttonIcon: ImageVector, onClick: () -> Unit, cornerRadius: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Button(onClick = onClick) {
                    Icon(imageVector = buttonIcon, contentDescription = null)
                }
            }
        }
    }
}

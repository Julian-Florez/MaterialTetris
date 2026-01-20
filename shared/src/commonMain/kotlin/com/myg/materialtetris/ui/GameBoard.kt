package com.myg.materialtetris.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.myg.materialtetris.model.Tetromino
import com.myg.materialtetris.ui.theme.getTetrisColors
import com.myg.materialtetris.viewmodel.BOARD_HEIGHT
import com.myg.materialtetris.viewmodel.BOARD_WIDTH
import kotlinx.coroutines.launch

@Composable
fun GameBoard(
    board: Array<Array<Int>>,
    activePiece: Tetromino?,
    isGameOver: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val gridColor = MaterialTheme.colorScheme.surfaceVariant
        val tetrisColors = getTetrisColors()
        val boardSize = if (maxWidth < maxHeight) maxWidth else maxHeight
        val cornerRadius = boardSize * 0.05f

        val animatableX = remember { Animatable(3f) }
        val animatableY = remember { Animatable(-2f) }
        val animatableRotation = remember { Animatable(0f) }

        fun normalizeAngle(angle: Float): Float {
            var a = angle % 360f
            if (a < 0) a += 360f
            return a
        }

        LaunchedEffect(activePiece) {
            if (activePiece == null) {
                animatableX.snapTo(3f)
                animatableY.snapTo(-2f)
                animatableRotation.snapTo(0f)
            } else {
                launch {
                    animatableX.animateTo(activePiece.x.toFloat(), tween(100))
                }
                launch {
                    animatableY.animateTo(activePiece.y.toFloat(), tween(100))
                }
                launch {
                    val current = normalizeAngle(animatableRotation.value)
                    val targetRotation = normalizeAngle(activePiece.rotation * 90f)
                    var diff = targetRotation - current
                    if (diff > 180f) diff -= 360f
                    if (diff < -180f) diff += 360f
                    val finalTarget = current + diff
                    animatableRotation.snapTo(current) // Asegura que el valor esté normalizado
                    animatableRotation.animateTo(finalTarget, tween(200))
                    animatableRotation.snapTo(normalizeAngle(finalTarget)) // Mantén el valor en [0,360)
                }
            }
        }

        Box(
            modifier = Modifier
                .aspectRatio(BOARD_WIDTH.toFloat() / BOARD_HEIGHT.toFloat())
                .clip(RoundedCornerShape(cornerRadius))
                .background(gridColor)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellSize = size.width / BOARD_WIDTH
                val pieceCornerRadius = cellSize * 0.35f

                // Draw the locked pieces
                for (r in board.indices) {
                    for (c in board[r].indices) {
                        if (board[r][c] > 0) {
                            val type = board[r][c]
                            val color = tetrisColors.getColor(type)

                            val getBoard = { ri: Int, ci: Int -> board.getOrNull(ri)?.getOrNull(ci) }

                            val hasTop = (getBoard(r - 1, c) ?: 0) > 0
                            val hasBottom = ((getBoard(r + 1, c) ?: 0) > 0) || r == board.lastIndex
                            val hasLeft = ((getBoard(r, c - 1) ?: 0) > 0) || c == 0
                            val hasRight = ((getBoard(r, c + 1) ?: 0) > 0) || c == board[r].lastIndex

                            drawPieceBlock(
                                x = c * cellSize,
                                y = r * cellSize,
                                cellSize = cellSize,
                                color = color,
                                cornerRadius = pieceCornerRadius,
                                hasTop = hasTop,
                                hasBottom = hasBottom,
                                hasLeft = hasLeft,
                                hasRight = hasRight
                            )
                        }
                    }
                }

                // Draw the active piece with animation
                activePiece?.let { piece ->
                    val color = tetrisColors.getColor(piece.type)
                    val pieceSize = piece.shape.size * cellSize

                    translate(
                        left = animatableX.value * cellSize,
                        top = animatableY.value * cellSize
                    ) {
                        rotate(
                            degrees = animatableRotation.value,
                            pivot = Offset(pieceSize / 2f, pieceSize / 2f)
                        ) {
                            drawTetrominoPiece(piece, cellSize, color)
                        }
                    }
                }
            }

            if (isGameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                }
            }
        }
    }
}

private fun DrawScope.drawTetrominoPiece(
    piece: Tetromino,
    cellSize: Float,
    color: Color
) {
    val pieceCornerRadius = cellSize * 0.35f
    for (r in piece.shape.indices) {
        for (c in piece.shape[r].indices) {
            if (piece.shape[r][c] > 0) {
                val getShapeBlock = { row: Int, col: Int ->
                    piece.shape.getOrNull(row)?.getOrNull(col)?.let { it > 0 } ?: false
                }
                val hasTop = getShapeBlock(r - 1, c)
                val hasBottom = getShapeBlock(r + 1, c)
                val hasLeft = getShapeBlock(r, c - 1)
                val hasRight = getShapeBlock(r, c + 1)
                drawPieceBlock(
                    x = c * cellSize,
                    y = r * cellSize,
                    cellSize = cellSize,
                    color = color,
                    cornerRadius = pieceCornerRadius,
                    hasTop = hasTop,
                    hasBottom = hasBottom,
                    hasLeft = hasLeft,
                    hasRight = hasRight
                )
            }
        }
    }
}

private fun DrawScope.drawPieceBlock(
    x: Float,
    y: Float,
    cellSize: Float,
    color: Color,
    cornerRadius: Float,
    hasTop: Boolean,
    hasBottom: Boolean,
    hasLeft: Boolean,
    hasRight: Boolean
) {
    val overlap = 1f
    val newCellSize = cellSize + overlap
    val newX = x - overlap / 2
    val newY = y - overlap / 2
    val newCornerRadius = cornerRadius * (newCellSize / cellSize)

    val topLeftRadius = if (!hasTop && !hasLeft) CornerRadius(newCornerRadius) else CornerRadius.Zero
    val topRightRadius = if (!hasTop && !hasRight) CornerRadius(newCornerRadius) else CornerRadius.Zero
    val bottomLeftRadius = if (!hasBottom && !hasLeft) CornerRadius(newCornerRadius) else CornerRadius.Zero
    val bottomRightRadius = if (!hasBottom && !hasRight) CornerRadius(newCornerRadius) else CornerRadius.Zero
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = newX, top = newY, right = newX + newCellSize, bottom = newY + newCellSize,
                topLeftCornerRadius = topLeftRadius,
                topRightCornerRadius = topRightRadius,
                bottomLeftCornerRadius = bottomLeftRadius,
                bottomRightCornerRadius = bottomRightRadius
            )
        )
    }
    drawPath(path, color)

    // Draw concave corners
    if (hasTop && hasLeft) {
        drawPath(Path().apply {
            moveTo(newX, newY + newCornerRadius)
            lineTo(newX, newY)
            lineTo(newX + newCornerRadius, newY)
            arcTo(Rect(newX - newCornerRadius, newY - newCornerRadius, newX + newCornerRadius, newY + newCornerRadius), 90f, -90f, false)
            close()
        }, color = color)
    }
    if (hasTop && hasRight) {
        drawPath(Path().apply {
            moveTo(newX + newCellSize - newCornerRadius, newY)
            lineTo(newX + newCellSize, newY)
            lineTo(newX + newCellSize, newY + newCornerRadius)
            arcTo(Rect(newX + newCellSize - newCornerRadius, newY - newCornerRadius, newX + newCellSize + newCornerRadius, newY + newCornerRadius), 180f, -90f, false)
            close()
        }, color = color)
    }
    if (hasBottom && hasLeft) {
        drawPath(Path().apply {
            moveTo(newX, newY + newCellSize - newCornerRadius)
            lineTo(newX, newY + newCellSize)
            lineTo(newX + newCornerRadius, newY + newCellSize)
            arcTo(Rect(newX - newCornerRadius, newY + newCellSize - newCornerRadius, newX + newCornerRadius, newY + newCellSize + newCornerRadius), 0f, 90f, false)
            close()
        }, color = color)
    }
    if (hasBottom && hasRight) {
        drawPath(Path().apply {
            moveTo(newX + newCellSize - newCornerRadius, newY + newCellSize)
            lineTo(newX + newCellSize, newY + newCellSize)
            lineTo(newX + newCellSize, newY + newCellSize - newCornerRadius)
            arcTo(Rect(newX + newCellSize - newCornerRadius, newY + newCellSize - newCornerRadius, newX + newCellSize + newCornerRadius, newY + newCellSize + newCornerRadius), 270f, 90f, false)
            close()
        }, color = color)
    }
}

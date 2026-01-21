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
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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

        LaunchedEffect(activePiece) {
            if (activePiece == null) {
                animatableX.snapTo(3f)
                animatableY.snapTo(-2f)
            } else {
                launch {
                    animatableX.animateTo(activePiece.x.toFloat(), tween(100))
                }
                launch {
                    animatableY.animateTo(activePiece.y.toFloat(), tween(100))
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

                // Draw the locked pieces (single pass)
                for (r in board.indices) {
                    for (c in board[r].indices) {
                        if (board[r][c] > 0) {
                            val type = board[r][c]
                            val color = tetrisColors.getColor(type)

                            val getBoard = { ri: Int, ci: Int -> board.getOrNull(ri)?.getOrNull(ci) ?: 0 }

                            val hasTop = getBoard(r - 1, c) > 0
                            val hasBottom = getBoard(r + 1, c) > 0 || r == board.lastIndex
                            val hasLeft = getBoard(r, c - 1) > 0 || c == 0
                            val hasRight = getBoard(r, c + 1) > 0 || c == board[r].lastIndex
                            val hasTopLeft = getBoard(r - 1, c - 1) > 0
                            val hasTopRight = getBoard(r - 1, c + 1) > 0
                            val hasBottomLeft = getBoard(r + 1, c - 1) > 0
                            val hasBottomRight = getBoard(r + 1, c + 1) > 0

                            drawPieceBlock(
                                x = c * cellSize,
                                y = r * cellSize,
                                cellSize = cellSize,
                                color = color,
                                cornerRadius = pieceCornerRadius,
                                hasTop = hasTop,
                                hasBottom = hasBottom,
                                hasLeft = hasLeft,
                                hasRight = hasRight,
                                hasTopLeft = hasTopLeft,
                                hasTopRight = hasTopRight,
                                hasBottomLeft = hasBottomLeft,
                                hasBottomRight = hasBottomRight,
                                backgroundColor = gridColor
                            )
                        }
                    }
                }

                // Draw the active piece with animation
                activePiece?.let { piece ->
                    val color = tetrisColors.getColor(piece.type)
                    translate(
                        left = animatableX.value * cellSize,
                        top = animatableY.value * cellSize
                    ) {
                        drawTetrominoPiece(piece, cellSize, color, backgroundColor = gridColor)
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
                    androidx.compose.material3.Text(
                        text = "Game Over",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawTetrominoPiece(
    piece: Tetromino,
    cellSize: Float,
    color: Color,
    backgroundColor: Color
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
                val hasTopLeft = getShapeBlock(r - 1, c - 1)
                val hasTopRight = getShapeBlock(r - 1, c + 1)
                val hasBottomLeft = getShapeBlock(r + 1, c - 1)
                val hasBottomRight = getShapeBlock(r + 1, c + 1)

                drawPieceBlock(
                    x = c * cellSize,
                    y = r * cellSize,
                    cellSize = cellSize,
                    color = color,
                    cornerRadius = pieceCornerRadius,
                    hasTop = hasTop,
                    hasBottom = hasBottom,
                    hasLeft = hasLeft,
                    hasRight = hasRight,
                    hasTopLeft = hasTopLeft,
                    hasTopRight = hasTopRight,
                    hasBottomLeft = hasBottomLeft,
                    hasBottomRight = hasBottomRight,
                    backgroundColor = backgroundColor
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
    hasRight: Boolean,
    hasTopLeft: Boolean,
    hasTopRight: Boolean,
    hasBottomLeft: Boolean,
    hasBottomRight: Boolean,
    backgroundColor: Color
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

    // Increase quarter-circle size slightly and align them so the arc's inner corner
    // still meets the block corner. Use backgroundColor for the quarter-circles and draw a small rect outside.
    val arcScale = 2.0f
    val arcRadius = newCornerRadius * arcScale

    val smallRectSize = Size(arcRadius / 2f, arcRadius / 2f)

    if (hasTop && hasLeft && !hasTopLeft) {
        val rectTopLeftTL = Offset(newX - smallRectSize.width, newY - smallRectSize.height)
        drawRect(color = color, topLeft = rectTopLeftTL, size = smallRectSize)

        drawArc(
            color = backgroundColor,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(newX - arcRadius, newY - arcRadius),
            size = Size(arcRadius, arcRadius)
        )
    }
    if (hasTop && hasRight && !hasTopRight) {
        val rectTopLeftTR = Offset(newX + newCellSize, newY - smallRectSize.height)
        drawRect(color = color, topLeft = rectTopLeftTR, size = smallRectSize)

        drawArc(
            color = backgroundColor,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(newX + newCellSize, newY - arcRadius),
            size = Size(arcRadius, arcRadius)
        )
    }
    if (hasBottom && hasLeft && !hasBottomLeft) {
        val rectTopLeftBL = Offset(newX - smallRectSize.width, newY + newCellSize)
        drawRect(color = color, topLeft = rectTopLeftBL, size = smallRectSize)

        drawArc(
            color = backgroundColor,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(newX - arcRadius, newY + newCellSize),
            size = Size(arcRadius, arcRadius)
        )
    }
    if (hasBottom && hasRight && !hasBottomRight) {
        val rectTopLeftBR = Offset(newX + newCellSize, newY + newCellSize)
        drawRect(color = color, topLeft = rectTopLeftBR, size = smallRectSize)

        drawArc(
            color = backgroundColor,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(newX + newCellSize, newY + newCellSize),
            size = Size(arcRadius, arcRadius)
        )
    }
}

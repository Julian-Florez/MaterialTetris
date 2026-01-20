package com.myg.materialtetris.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.myg.materialtetris.model.Tetromino

fun DrawScope.drawPiece(piece: Tetromino, cellSize: Float, color: Color) {
    val shape = piece.shape
    val matrixSize = shape.size
    val cornerRadius = cellSize * 0.25f

    for (r in 0 until matrixSize) {
        for (c in 0 until matrixSize) {
            if (shape[r][c] != 0) {
                val x = c * cellSize
                val y = r * cellSize

                val hasTop = r > 0 && shape[r - 1][c] != 0
                val hasBottom = r < matrixSize - 1 && shape[r + 1][c] != 0
                val hasLeft = c > 0 && shape[r][c - 1] != 0
                val hasRight = c < matrixSize - 1 && shape[r][c + 1] != 0

                val path = Path().apply {
                    addRoundRect(RoundRect(
                        left = x, top = y, right = x + cellSize, bottom = y + cellSize,
                        topLeftCornerRadius = if (!hasTop && !hasLeft) CornerRadius(cornerRadius) else CornerRadius.Zero,
                        topRightCornerRadius = if (!hasTop && !hasRight) CornerRadius(cornerRadius) else CornerRadius.Zero,
                        bottomLeftCornerRadius = if (!hasBottom && !hasLeft) CornerRadius(cornerRadius) else CornerRadius.Zero,
                        bottomRightCornerRadius = if (!hasBottom && !hasRight) CornerRadius(cornerRadius) else CornerRadius.Zero
                    ))
                }
                drawPath(path, color)

                // Draw concave corners
                if (hasTop && hasLeft) {
                    drawPath(Path().apply {
                        moveTo(x, y + cornerRadius)
                        lineTo(x, y)
                        lineTo(x + cornerRadius, y)
                        arcTo(Rect(x - cornerRadius, y - cornerRadius, x + cornerRadius, y + cornerRadius), 90f, -90f, false)
                        close()
                    }, color = color)
                }
                if (hasTop && hasRight) {
                    drawPath(Path().apply {
                        moveTo(x + cellSize - cornerRadius, y)
                        lineTo(x + cellSize, y)
                        lineTo(x + cellSize, y + cornerRadius)
                        arcTo(Rect(x + cellSize - cornerRadius, y - cornerRadius, x + cellSize + cornerRadius, y + cornerRadius), 180f, -90f, false)
                        close()
                    }, color = color)
                }
                if (hasBottom && hasLeft) {
                    drawPath(Path().apply {
                        moveTo(x, y + cellSize - cornerRadius)
                        lineTo(x, y + cellSize)
                        lineTo(x + cornerRadius, y + cellSize)
                        arcTo(Rect(x - cornerRadius, y + cellSize - cornerRadius, x + cornerRadius, y + cellSize + cornerRadius), 0f, 90f, false)
                        close()
                    }, color = color)
                }
                if (hasBottom && hasRight) {
                    drawPath(Path().apply {
                        moveTo(x + cellSize - cornerRadius, y + cellSize)
                        lineTo(x + cellSize, y + cellSize)
                        lineTo(x + cellSize, y + cellSize - cornerRadius)
                        arcTo(Rect(x + cellSize - cornerRadius, y + cellSize - cornerRadius, x + cellSize + cornerRadius, y + cellSize + cornerRadius), 270f, 90f, false)
                        close()
                    }, color = color)
                }
            }
        }
    }
}

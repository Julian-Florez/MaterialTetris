package com.myg.materialtetris.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.myg.materialtetris.model.Tetromino
import com.myg.materialtetris.ui.theme.getTetrisColors

@Composable
fun NextPiece(piece: Tetromino?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier.padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            val tetrisColors = getTetrisColors()
            Canvas(modifier = Modifier.size(48.dp)) {
                piece?.let {
                    val cellSize = size.width / 4

                    var minR = Int.MAX_VALUE
                    var maxR = Int.MIN_VALUE
                    var minC = Int.MAX_VALUE
                    var maxC = Int.MIN_VALUE
                    for (r in it.shape.indices) {
                        for (c in it.shape[r].indices) {
                            if (it.shape[r][c] != 0) {
                                if (r < minR) minR = r
                                if (r > maxR) maxR = r
                                if (c < minC) minC = c
                                if (c > maxC) maxC = c
                            }
                        }
                    }

                    if (maxR != Int.MIN_VALUE) { // If a piece exists
                        val pieceWidth = (maxC - minC + 1) * cellSize
                        val pieceHeight = (maxR - minR + 1) * cellSize

                        val offsetX = (size.width - pieceWidth) / 2
                        val offsetY = (size.height - pieceHeight) / 2

                        translate(
                            left = offsetX - minC * cellSize,
                            top = offsetY - minR * cellSize
                        ) {
                            val color = tetrisColors.getColor(it.type)
                            drawTetrominoPiece(it, cellSize, color)
                        }
                    }
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

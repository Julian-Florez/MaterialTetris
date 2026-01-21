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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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
            // backgroundColor must be read in a @Composable context (outside the Canvas drawing lambda)
            val backgroundColor = MaterialTheme.colorScheme.surfaceVariant

            Canvas(modifier = Modifier.size(48.dp)) {
                piece?.let {
                    val cellSize = size.width / 4

                    // pieceCornerRadius used both for underlay sizing and block corner radii
                    val pieceCornerRadius = cellSize * 0.35f

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

                            // FIRST: draw corner underlays for preview piece so corners are always under blocks
                            val getShapeBlock = { row: Int, col: Int ->
                                it.shape.getOrNull(row)?.getOrNull(col)?.let { it > 0 } ?: false
                            }
                            for (rr in it.shape.indices) {
                                for (cc in it.shape[rr].indices) {
                                    if (it.shape[rr][cc] > 0) {
                                        val hasTop = getShapeBlock(rr - 1, cc)
                                        val hasBottom = getShapeBlock(rr + 1, cc)
                                        val hasLeft = getShapeBlock(rr, cc - 1)
                                        val hasRight = getShapeBlock(rr, cc + 1)
                                        val hasTopLeft = getShapeBlock(rr - 1, cc - 1)
                                        val hasTopRight = getShapeBlock(rr - 1, cc + 1)
                                        val hasBottomLeft = getShapeBlock(rr + 1, cc - 1)
                                        val hasBottomRight = getShapeBlock(rr + 1, cc + 1)

                                        // compute neighbor colors for preview: use piece color when neighbor present, otherwise background
                                        val neighborTopColor = if (hasTop) color else backgroundColor
                                        val neighborBottomColor = if (hasBottom) color else backgroundColor
                                        val neighborLeftColor = if (hasLeft) color else backgroundColor
                                        val neighborRightColor = if (hasRight) color else backgroundColor

                                        drawCornerUnderlay(
                                            x = cc * cellSize,
                                            y = rr * cellSize,
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
                                            colorTop = neighborTopColor,
                                            colorBottom = neighborBottomColor,
                                            colorLeft = neighborLeftColor,
                                            colorRight = neighborRightColor,
                                            backgroundColor = backgroundColor
                                        )
                                     }
                                 }
                             }

                             // Then draw the preview piece on top
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

// Helper: draw corner underlay (rect outside + quarter-circle of background)
@Suppress("UNUSED_PARAMETER")
private fun DrawScope.drawCornerUnderlay(
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
    colorTop: Color,
    colorBottom: Color,
    colorLeft: Color,
    colorRight: Color,
    backgroundColor: Color
) {
     val overlap = 1f
     val newCellSize = cellSize + overlap
     val newX = x - overlap / 2
     val newY = y - overlap / 2
     val newCornerRadius = cornerRadius * (newCellSize / cellSize)

     val arcScale = 2.0f
     val arcRadius = newCornerRadius * arcScale
     val smallRectSize = Size(arcRadius / 2f, arcRadius / 2f)

    if (hasTop && hasLeft && !hasTopLeft) {
        val rectTopLeftTL = Offset(newX - smallRectSize.width, newY - smallRectSize.height)
        // move rectangle 1px diagonally inward (right/down)
        val rectTopLeftTLInset = Offset(rectTopLeftTL.x + 1f, rectTopLeftTL.y + 1f)
        val start = rectTopLeftTLInset + Offset(smallRectSize.width / 2f, 0f)
        val end = rectTopLeftTLInset + Offset(0f, smallRectSize.height / 2f)
        if (colorTop == colorLeft) {
            drawRect(color = colorTop, topLeft = rectTopLeftTLInset, size = smallRectSize)
        } else {
            val brush = Brush.linearGradient(listOf(colorTop, colorLeft), start = start, end = end)
            drawRect(brush = brush, topLeft = rectTopLeftTLInset, size = smallRectSize)
        }

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
        // move rectangle 1px diagonally inward (left/down)
        val rectTopLeftTRInset = Offset(rectTopLeftTR.x - 1f, rectTopLeftTR.y + 1f)
        val start = rectTopLeftTRInset + Offset(smallRectSize.width / 2f, 0f)
        val end = rectTopLeftTRInset + Offset(smallRectSize.width, smallRectSize.height / 2f)
        if (colorTop == colorRight) {
            drawRect(color = colorTop, topLeft = rectTopLeftTRInset, size = smallRectSize)
        } else {
            val brush = Brush.linearGradient(listOf(colorTop, colorRight), start = start, end = end)
            drawRect(brush = brush, topLeft = rectTopLeftTRInset, size = smallRectSize)
        }

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
        // move rectangle 1px diagonally inward (right/up)
        val rectTopLeftBLInset = Offset(rectTopLeftBL.x + 1f, rectTopLeftBL.y - 1f)
        val start = rectTopLeftBLInset + Offset(smallRectSize.width / 2f, smallRectSize.height)
        val end = rectTopLeftBLInset + Offset(0f, smallRectSize.height / 2f)
        if (colorBottom == colorLeft) {
            drawRect(color = colorBottom, topLeft = rectTopLeftBLInset, size = smallRectSize)
        } else {
            val brush = Brush.linearGradient(listOf(colorBottom, colorLeft), start = start, end = end)
            drawRect(brush = brush, topLeft = rectTopLeftBLInset, size = smallRectSize)
        }

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
        // move rectangle 1px diagonally inward (left/up)
        val rectTopLeftBRInset = Offset(rectTopLeftBR.x - 1f, rectTopLeftBR.y - 1f)
        val start = rectTopLeftBRInset + Offset(smallRectSize.width / 2f, smallRectSize.height)
        val end = rectTopLeftBRInset + Offset(smallRectSize.width, smallRectSize.height / 2f)
        if (colorBottom == colorRight) {
            drawRect(color = colorBottom, topLeft = rectTopLeftBRInset, size = smallRectSize)
        } else {
            val brush = Brush.linearGradient(listOf(colorBottom, colorRight), start = start, end = end)
            drawRect(brush = brush, topLeft = rectTopLeftBRInset, size = smallRectSize)
        }

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

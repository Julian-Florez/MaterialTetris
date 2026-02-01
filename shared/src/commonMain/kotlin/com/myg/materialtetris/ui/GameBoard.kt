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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import com.myg.materialtetris.model.Tetromino
import com.myg.materialtetris.ui.theme.getTetrisColors
import com.myg.materialtetris.ui.theme.getSystemAccent1Tone800
import com.myg.materialtetris.viewmodel.BOARD_HEIGHT
import com.myg.materialtetris.viewmodel.BOARD_WIDTH
import kotlinx.coroutines.launch

/**
 * Animatable state holder for a single cell's corner radii.
 */
private class AnimatableCellCorners {
    val topLeft = Animatable(1f)
    val topRight = Animatable(1f)
    val bottomLeft = Animatable(1f)
    val bottomRight = Animatable(1f)
}

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

        // State for animated corner radii of locked pieces
        // Keys are "row,col" strings, values are AnimatableCellCorners
        val cornerAnimations = remember { mutableMapOf<String, AnimatableCellCorners>() }

        // Helper function to get board value safely
        val getBoard = { ri: Int, ci: Int -> board.getOrNull(ri)?.getOrNull(ci) ?: 0 }

        // Animate corner radii for all locked cells when board changes
        LaunchedEffect(board.contentDeepHashCode()) {
            for (r in board.indices) {
                for (c in board[r].indices) {
                    if (board[r][c] > 0) {
                        val key = "$r,$c"

                        // Compute target corner radii using the EXACT same triggers as before
                        val hasTop = getBoard(r - 1, c) > 0
                        val hasBottom = getBoard(r + 1, c) > 0 || r == board.lastIndex
                        val hasLeft = getBoard(r, c - 1) > 0 || c == 0
                        val hasRight = getBoard(r, c + 1) > 0 || c == board[r].lastIndex

                        // Target: 1f = full radius, 0f = no radius (squared corner)
                        val targetTopLeft = if (!hasTop && !hasLeft) 1f else 0f
                        val targetTopRight = if (!hasTop && !hasRight) 1f else 0f
                        val targetBottomLeft = if (!hasBottom && !hasLeft) 1f else 0f
                        val targetBottomRight = if (!hasBottom && !hasRight) 1f else 0f

                        // Check if this is a new cell (not yet in the map)
                        val isNewCell = !cornerAnimations.containsKey(key)

                        // Get or create animatable corners for this cell
                        val corners = cornerAnimations.getOrPut(key) { AnimatableCellCorners() }

                        if (isNewCell) {
                            // New cell: snap to initial values immediately
                            launch { corners.topLeft.snapTo(targetTopLeft) }
                            launch { corners.topRight.snapTo(targetTopRight) }
                            launch { corners.bottomLeft.snapTo(targetBottomLeft) }
                            launch { corners.bottomRight.snapTo(targetBottomRight) }
                        } else {
                            // Existing cell: animate to target values
                            launch { corners.topLeft.animateTo(targetTopLeft, tween(150)) }
                            launch { corners.topRight.animateTo(targetTopRight, tween(150)) }
                            launch { corners.bottomLeft.animateTo(targetBottomLeft, tween(150)) }
                            launch { corners.bottomRight.animateTo(targetBottomRight, tween(150)) }
                        }
                    }
                }
            }

            // Cleanup: remove entries for cells that no longer have pieces
            val validKeys = mutableSetOf<String>()
            for (r in board.indices) {
                for (c in board[r].indices) {
                    if (board[r][c] > 0) {
                        validKeys.add("$r,$c")
                    }
                }
            }
            cornerAnimations.keys.removeAll { it !in validKeys }
        }

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
            // Obtener previewColor en contexto @Composable (no dentro del DrawScope)
            val previewColor = getSystemAccent1Tone800()
             Canvas(modifier = Modifier.fillMaxSize()) {
                // Usar el menor valor entre ancho y alto para cellSize, así nunca sobresale
                val cellSize = minOf(size.width / BOARD_WIDTH, size.height / BOARD_HEIGHT)
                val boardDrawWidth = cellSize * BOARD_WIDTH
                val boardDrawHeight = cellSize * BOARD_HEIGHT
                // Centrar el tablero dentro del Canvas si hay espacio extra
                val offsetX = (size.width - boardDrawWidth) / 2f
                val offsetY = (size.height - boardDrawHeight) / 2f
                val pieceCornerRadius = cellSize * 0.35f

                // helper to access board safely from anywhere inside Canvas
                val getBoard = { ri: Int, ci: Int -> board.getOrNull(ri)?.getOrNull(ci) ?: 0 }

                // FIRST PASS: draw corner underlays (rect + quarter-circle) for all cells (locked + active)
                // Locked pieces underlays
                for (r in board.indices) {
                    for (c in board[r].indices) {
                        if (board[r][c] > 0) {
                            val type = board[r][c]
                            val color = tetrisColors.getColor(type)

                            val hasTop = getBoard(r - 1, c) > 0
                            val hasBottom = getBoard(r + 1, c) > 0 || r == board.lastIndex
                            val hasLeft = getBoard(r, c - 1) > 0 || c == 0
                            val hasRight = getBoard(r, c + 1) > 0 || c == board[r].lastIndex
                            val hasTopLeft = getBoard(r - 1, c - 1) > 0
                            val hasTopRight = getBoard(r - 1, c + 1) > 0
                            val hasBottomLeft = getBoard(r + 1, c - 1) > 0
                            val hasBottomRight = getBoard(r + 1, c + 1) > 0

                            // neighbor colors
                            val topColor = tetrisColors.getColor(getBoard(r - 1, c))
                            val bottomColor = tetrisColors.getColor(getBoard(r + 1, c))
                            val leftColor = tetrisColors.getColor(getBoard(r, c - 1))
                            val rightColor = tetrisColors.getColor(getBoard(r, c + 1))

                            drawCornerUnderlay(
                                x = offsetX + c * cellSize,
                                y = offsetY + r * cellSize,
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
                                colorTop = topColor,
                                colorBottom = bottomColor,
                                colorLeft = leftColor,
                                colorRight = rightColor,
                                backgroundColor = gridColor
                            )
                        }
                    }
                }

                // Active piece underlays (draw translated so they match active position)
                activePiece?.let { piece ->
                    val color = tetrisColors.getColor(piece.type)
                    translate(left = offsetX + animatableX.value * cellSize, top = offsetY + animatableY.value * cellSize) {
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

                                    // compute neighbor colors: prefer piece neighbor if present, otherwise board
                                    val globalR = piece.y + r
                                    val globalC = piece.x + c
                                    val topType = if (getShapeBlock(r - 1, c)) piece.type else getBoard(globalR - 1, globalC)
                                    val bottomType = if (getShapeBlock(r + 1, c)) piece.type else getBoard(globalR + 1, globalC)
                                    val leftType = if (getShapeBlock(r, c - 1)) piece.type else getBoard(globalR, globalC - 1)
                                    val rightType = if (getShapeBlock(r, c + 1)) piece.type else getBoard(globalR, globalC + 1)
                                    val topColor = tetrisColors.getColor(topType)
                                    val bottomColor = tetrisColors.getColor(bottomType)
                                    val leftColor = tetrisColors.getColor(leftType)
                                    val rightColor = tetrisColors.getColor(rightType)

                                    drawCornerUnderlay(
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
                                        colorTop = topColor,
                                        colorBottom = bottomColor,
                                        colorLeft = leftColor,
                                        colorRight = rightColor,
                                        backgroundColor = gridColor
                                    )
                                }
                            }
                        }
                    }
                }

                // Draw the locked pieces (single pass) with animated corner radii
                for (r in board.indices) {
                    for (c in board[r].indices) {
                        if (board[r][c] > 0) {
                            val type = board[r][c]
                            val color = tetrisColors.getColor(type)

                            // Get animated corner radii for this cell
                            val key = "$r,$c"
                            val corners = cornerAnimations[key]

                            if (corners != null) {
                                // Use animated corner factors
                                drawPieceBlockAnimated(
                                    x = offsetX + c * cellSize,
                                    y = offsetY + r * cellSize,
                                    cellSize = cellSize,
                                    color = color,
                                    cornerRadius = pieceCornerRadius,
                                    topLeftFactor = corners.topLeft.value,
                                    topRightFactor = corners.topRight.value,
                                    bottomLeftFactor = corners.bottomLeft.value,
                                    bottomRightFactor = corners.bottomRight.value
                                )
                            } else {
                                // Fallback: compute corner radii directly (should rarely happen)
                                val hasTop = getBoard(r - 1, c) > 0
                                val hasBottom = getBoard(r + 1, c) > 0 || r == board.lastIndex
                                val hasLeft = getBoard(r, c - 1) > 0 || c == 0
                                val hasRight = getBoard(r, c + 1) > 0 || c == board[r].lastIndex

                                drawPieceBlock(
                                    x = offsetX + c * cellSize,
                                    y = offsetY + r * cellSize,
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

                // DIBUJAR PRIMERO LA FICHA FANTASMA (ghost/preview) PARA QUE QUEDE DETRÁS DE TODO
                activePiece?.let { piece ->
                    val landingY = computeLandingY(board, piece)
                    // Solo dibujar preview si hay una fila válida de aterrizaje
                    if (landingY >= piece.y) {
                        // Usar las coordenadas fijas de destino, NO las animadas
                        translate(left = offsetX + piece.x * cellSize, top = offsetY + landingY * cellSize) {
                            drawTetrominoPiece(piece, cellSize, previewColor)
                        }
                    }
                }

                // Draw the active piece on top
                activePiece?.let { piece ->
                    val color = tetrisColors.getColor(piece.type)
                    translate(
                        left = offsetX + animatableX.value * cellSize,
                        top = offsetY + animatableY.value * cellSize
                    ) {
                        drawTetrominoPiece(piece, cellSize, color)
                    }
                }

                // end of Canvas drawing
            }

            // Overlay for game over must be a composable call outside Canvas (DrawScope)
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
    // Default implementation uses binary (0 or 1) corner factors based on has* flags
    val topLeftFactor = if (!hasTop && !hasLeft) 1f else 0f
    val topRightFactor = if (!hasTop && !hasRight) 1f else 0f
    val bottomLeftFactor = if (!hasBottom && !hasLeft) 1f else 0f
    val bottomRightFactor = if (!hasBottom && !hasRight) 1f else 0f

    drawPieceBlockAnimated(
        x = x,
        y = y,
        cellSize = cellSize,
        color = color,
        cornerRadius = cornerRadius,
        topLeftFactor = topLeftFactor,
        topRightFactor = topRightFactor,
        bottomLeftFactor = bottomLeftFactor,
        bottomRightFactor = bottomRightFactor
    )
}

/**
 * Draws a piece block with animated corner radius factors.
 * Each factor is a value from 0f (no radius, squared) to 1f (full radius).
 */
private fun DrawScope.drawPieceBlockAnimated(
    x: Float,
    y: Float,
    cellSize: Float,
    color: Color,
    cornerRadius: Float,
    topLeftFactor: Float,
    topRightFactor: Float,
    bottomLeftFactor: Float,
    bottomRightFactor: Float
) {
    val overlap = 1f
    val newCellSize = cellSize + overlap
    val newX = x - overlap / 2
    val newY = y - overlap / 2
    val newCornerRadius = cornerRadius * (newCellSize / cellSize)

    val topLeftRadius = CornerRadius(newCornerRadius * topLeftFactor)
    val topRightRadius = CornerRadius(newCornerRadius * topRightFactor)
    val bottomLeftRadius = CornerRadius(newCornerRadius * bottomLeftFactor)
    val bottomRightRadius = CornerRadius(newCornerRadius * bottomRightFactor)
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

    // Previously this function also drew small rects and quarter-circle arcs for convex corners.
    // That drawing now happens exclusively in drawCornerUnderlay (first pass) so the block drawing
    // doesn't duplicate or overlay those underlays.
}

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

// Helper para calcular la fila de aterrizaje (landing Y) de una pieza sin modificar el board
private fun computeLandingY(board: Array<Array<Int>>, piece: Tetromino): Int {
    var yTest = piece.y
    var lastValid = piece.y
    val shape = piece.shape

    // iterate until a collision is detected; negative global rows are allowed (spawn area)
    while (true) {
        var collision = false
        loop@ for (r in shape.indices) {
            for (c in shape[r].indices) {
                if (shape[r][c] <= 0) continue
                val globalR = yTest + r
                val globalC = piece.x + c
                // horizontal out-of-bounds is a collision for landing purposes
                if (globalC < 0 || globalC >= BOARD_WIDTH) {
                    collision = true
                    break@loop
                }
                // if below the board -> collision
                if (globalR >= BOARD_HEIGHT) {
                    collision = true
                    break@loop
                }
                // ignore parts above the top (globalR < 0) when checking board occupancy
                if (globalR >= 0 && board[globalR][globalC] > 0) {
                    collision = true
                    break@loop
                }
            }
        }
        if (collision) {
            return lastValid
        } else {
            lastValid = yTest
            yTest += 1
            // safety: if we've tested beyond a reasonable bound, return last valid
            if (yTest > BOARD_HEIGHT + 5) return lastValid
        }
    }
}

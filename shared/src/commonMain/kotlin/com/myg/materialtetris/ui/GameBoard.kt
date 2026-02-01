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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Debug flag for animation logging - set to false for release builds
private const val DEBUG_ANIMATION_LOGGING = true

private fun logAnimation(message: String) {
    if (DEBUG_ANIMATION_LOGGING) {
        println("[CornerAnim] $message")
    }
}

private fun logBoardState(
    board: Array<Array<Int>>,
    cornerAnimations: Map<String, AnimatableCellCorners>,
    convexCornerAnimations: Map<String, AnimatableConvexCorners>
) {
    if (!DEBUG_ANIMATION_LOGGING) return

    val sb = StringBuilder()
    sb.appendLine("========== BOARD STATE ==========")

    // Find occupied cells
    val occupiedCells = mutableListOf<String>()
    for (r in board.indices) {
        for (c in board[r].indices) {
            if (board[r][c] > 0) {
                occupiedCells.add("[$r,$c]")
            }
        }
    }
    sb.appendLine("Occupied cells: ${occupiedCells.joinToString(", ")}")

    // Show animation states for each cell
    for (r in board.indices) {
        for (c in board[r].indices) {
            if (board[r][c] > 0) {
                val key = "$r,$c"
                val corners = cornerAnimations[key]
                val convex = convexCornerAnimations[key]

                if (corners != null || convex != null) {
                    sb.append("  [$key] CONCAVE: ")
                    if (corners != null) {
                        sb.append("TL=${corners.topLeft.animatable.value.format()}${if(corners.topLeft.activeJob?.isActive == true) "*" else ""} ")
                        sb.append("TR=${corners.topRight.animatable.value.format()}${if(corners.topRight.activeJob?.isActive == true) "*" else ""} ")
                        sb.append("BL=${corners.bottomLeft.animatable.value.format()}${if(corners.bottomLeft.activeJob?.isActive == true) "*" else ""} ")
                        sb.append("BR=${corners.bottomRight.animatable.value.format()}${if(corners.bottomRight.activeJob?.isActive == true) "*" else ""}")
                    } else {
                        sb.append("(no state)")
                    }
                    sb.append(" | CONVEX: ")
                    if (convex != null) {
                        sb.append("TL=${convex.topLeft.animatable.value.format()}${if(convex.topLeft.activeJob?.isActive == true) "*" else ""} ")
                        sb.append("TR=${convex.topRight.animatable.value.format()}${if(convex.topRight.activeJob?.isActive == true) "*" else ""} ")
                        sb.append("BL=${convex.bottomLeft.animatable.value.format()}${if(convex.bottomLeft.activeJob?.isActive == true) "*" else ""} ")
                        sb.append("BR=${convex.bottomRight.animatable.value.format()}${if(convex.bottomRight.activeJob?.isActive == true) "*" else ""}")
                    } else {
                        sb.append("(no state)")
                    }
                    sb.appendLine()
                }
            }
        }
    }
    sb.appendLine("==================================")
    println("[CornerAnim] ${sb}")
}

private fun Float.format(): String {
    val intPart = this.toInt()
    val decPart = ((this - intPart) * 10).toInt()
    return "$intPart.$decPart"
}

/**
 * Manages animation for a single corner with proper Job lifecycle.
 */
private class CornerAnimationState(initialValue: Float) {
    val animatable = Animatable(initialValue)

    // The last target that was COMMITTED (animation started or snapTo completed)
    var committedTarget: Float = initialValue

    // Current active job - null means no animation in progress
    var activeJob: Job? = null
}

/**
 * Animatable state holder for a single cell's corner radii.
 */
private class AnimatableCellCorners {
    val topLeft = CornerAnimationState(1f)
    val topRight = CornerAnimationState(1f)
    val bottomLeft = CornerAnimationState(1f)
    val bottomRight = CornerAnimationState(1f)
}

/**
 * Animatable state holder for convex corner arc radius factors.
 */
private class AnimatableConvexCorners {
    val topLeft = CornerAnimationState(0f)
    val topRight = CornerAnimationState(0f)
    val bottomLeft = CornerAnimationState(0f)
    val bottomRight = CornerAnimationState(0f)
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

        // State for animated convex corner arc radii
        // Keys are "row,col" strings, values are AnimatableConvexCorners
        val convexCornerAnimations = remember { mutableMapOf<String, AnimatableConvexCorners>() }

        // Helper function to get board value safely
        val getBoard = { ri: Int, ci: Int -> board.getOrNull(ri)?.getOrNull(ci) ?: 0 }

        // Animate corner radii for all locked cells when board changes
        LaunchedEffect(board.contentDeepHashCode()) {
            // FIRST PASS: Create new cells and snapTo their values
            // This must happen before animation detection so all cells have correct committedTarget
            val newCellKeys = mutableSetOf<String>()

            for (r in board.indices) {
                for (c in board[r].indices) {
                    if (board[r][c] > 0) {
                        val key = "$r,$c"

                        if (!cornerAnimations.containsKey(key)) {
                            newCellKeys.add(key)

                            // Compute target corner radii
                            val hasTop = getBoard(r - 1, c) > 0
                            val hasBottom = getBoard(r + 1, c) > 0 || r == board.lastIndex
                            val hasLeft = getBoard(r, c - 1) > 0 || c == 0
                            val hasRight = getBoard(r, c + 1) > 0 || c == board[r].lastIndex
                            val hasTopLeft = getBoard(r - 1, c - 1) > 0
                            val hasTopRight = getBoard(r - 1, c + 1) > 0
                            val hasBottomLeft = getBoard(r + 1, c - 1) > 0
                            val hasBottomRight = getBoard(r + 1, c + 1) > 0

                            val targetTL = if (!hasTop && !hasLeft) 1f else 0f
                            val targetTR = if (!hasTop && !hasRight) 1f else 0f
                            val targetBL = if (!hasBottom && !hasLeft) 1f else 0f
                            val targetBR = if (!hasBottom && !hasRight) 1f else 0f
                            val convexTargetTL = if (hasTop && hasLeft && !hasTopLeft) 1f else 0f
                            val convexTargetTR = if (hasTop && hasRight && !hasTopRight) 1f else 0f
                            val convexTargetBL = if (hasBottom && hasLeft && !hasBottomLeft) 1f else 0f
                            val convexTargetBR = if (hasBottom && hasRight && !hasBottomRight) 1f else 0f

                            // Create and initialize new cell
                            val corners = AnimatableCellCorners()
                            val convexCorners = AnimatableConvexCorners()

                            logAnimation("[$key] NEW CELL: snapping to concave TL=$targetTL TR=$targetTR BL=$targetBL BR=$targetBR, convex TL=$convexTargetTL TR=$convexTargetTR BL=$convexTargetBL BR=$convexTargetBR")

                            // Snap concave corners synchronously
                            corners.topLeft.committedTarget = targetTL
                            corners.topLeft.animatable.snapTo(targetTL)
                            corners.topRight.committedTarget = targetTR
                            corners.topRight.animatable.snapTo(targetTR)
                            corners.bottomLeft.committedTarget = targetBL
                            corners.bottomLeft.animatable.snapTo(targetBL)
                            corners.bottomRight.committedTarget = targetBR
                            corners.bottomRight.animatable.snapTo(targetBR)

                            // Snap convex corners synchronously
                            convexCorners.topLeft.committedTarget = convexTargetTL
                            convexCorners.topLeft.animatable.snapTo(convexTargetTL)
                            convexCorners.topRight.committedTarget = convexTargetTR
                            convexCorners.topRight.animatable.snapTo(convexTargetTR)
                            convexCorners.bottomLeft.committedTarget = convexTargetBL
                            convexCorners.bottomLeft.animatable.snapTo(convexTargetBL)
                            convexCorners.bottomRight.committedTarget = convexTargetBR
                            convexCorners.bottomRight.animatable.snapTo(convexTargetBR)

                            cornerAnimations[key] = corners
                            convexCornerAnimations[key] = convexCorners
                        }
                    }
                }
            }

            // SECOND PASS: Animate existing cells that may have changed due to new neighbors
            for (r in board.indices) {
                for (c in board[r].indices) {
                    if (board[r][c] > 0) {
                        val key = "$r,$c"

                        // Skip cells that were just created (they already have correct values)
                        if (key in newCellKeys) continue

                        val corners = cornerAnimations[key] ?: continue
                        val convexCorners = convexCornerAnimations[key] ?: continue

                        // Compute target corner radii using the EXACT same triggers as before
                        val hasTop = getBoard(r - 1, c) > 0
                        val hasBottom = getBoard(r + 1, c) > 0 || r == board.lastIndex
                        val hasLeft = getBoard(r, c - 1) > 0 || c == 0
                        val hasRight = getBoard(r, c + 1) > 0 || c == board[r].lastIndex
                        val hasTopLeft = getBoard(r - 1, c - 1) > 0
                        val hasTopRight = getBoard(r - 1, c + 1) > 0
                        val hasBottomLeft = getBoard(r + 1, c - 1) > 0
                        val hasBottomRight = getBoard(r + 1, c + 1) > 0

                        val targetTL = if (!hasTop && !hasLeft) 1f else 0f
                        val targetTR = if (!hasTop && !hasRight) 1f else 0f
                        val targetBL = if (!hasBottom && !hasLeft) 1f else 0f
                        val targetBR = if (!hasBottom && !hasRight) 1f else 0f
                        val convexTargetTL = if (hasTop && hasLeft && !hasTopLeft) 1f else 0f
                        val convexTargetTR = if (hasTop && hasRight && !hasTopRight) 1f else 0f
                        val convexTargetBL = if (hasBottom && hasLeft && !hasBottomLeft) 1f else 0f
                        val convexTargetBR = if (hasBottom && hasRight && !hasBottomRight) 1f else 0f

                        // Helper function to animate a corner with proper lifecycle
                        fun animateCorner(
                            state: CornerAnimationState,
                            target: Float,
                            name: String,
                            durationMs: Int,
                            waitForJob: Job? = null
                        ) {
                            // Skip if already at target
                            if (state.committedTarget == target) {
                                return
                            }

                            val prevTarget = state.committedTarget
                            logAnimation("[$key] $name: flank detected $prevTarget -> $target (current: ${state.animatable.value})")

                            // Cancel previous animation if running
                            state.activeJob?.let { job ->
                                if (job.isActive) {
                                    logAnimation("[$key] $name: cancelling previous animation (was going to $prevTarget)")
                                    job.cancel()
                                }
                            }

                            // Launch new animation job
                            state.activeJob = launch {
                                // Update committedTarget INSIDE the job
                                state.committedTarget = target

                                // Wait for dependency if specified
                                if (waitForJob != null && waitForJob.isActive) {
                                    logAnimation("[$key] $name: waiting for dependency job to complete...")
                                    waitForJob.join()
                                    logAnimation("[$key] $name: dependency completed")
                                }

                                logAnimation("[$key] $name: starting animation from ${state.animatable.value} to $target")
                                state.animatable.animateTo(target, tween(durationMs))
                                logAnimation("[$key] $name: animation completed at ${state.animatable.value}")
                            }
                        }

                        // Animate CONCAVE corners
                        animateCorner(corners.topLeft, targetTL, "CONCAVE TL", 400)
                        animateCorner(corners.topRight, targetTR, "CONCAVE TR", 400)
                        animateCorner(corners.bottomLeft, targetBL, "CONCAVE BL", 400)
                        animateCorner(corners.bottomRight, targetBR, "CONCAVE BR", 400)

                        // Animate CONVEX corners (wait for corresponding concave if appearing 0->1)
                        val convexPairs = listOf(
                            Triple(convexCorners.topLeft, convexTargetTL, corners.topLeft) to "CONVEX TL",
                            Triple(convexCorners.topRight, convexTargetTR, corners.topRight) to "CONVEX TR",
                            Triple(convexCorners.bottomLeft, convexTargetBL, corners.bottomLeft) to "CONVEX BL",
                            Triple(convexCorners.bottomRight, convexTargetBR, corners.bottomRight) to "CONVEX BR"
                        )

                        for ((triple, name) in convexPairs) {
                            val (state, target, concaveState) = triple
                            val wasZero = state.committedTarget < 0.5f
                            val isOne = target > 0.5f
                            val isAppearing = wasZero && isOne

                            // Only wait for concave job if convex corner is APPEARING (0->1)
                            val waitJob = if (isAppearing) concaveState.activeJob else null
                            animateCorner(state, target, name, 600, waitJob)
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
            convexCornerAnimations.keys.removeAll { it !in validKeys }

            // Log board state after processing
            logBoardState(board, cornerAnimations, convexCornerAnimations)
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

                            // Get animated convex corner factors
                            val key = "$r,$c"
                            val convexCorners = convexCornerAnimations[key]

                            drawCornerUnderlayAnimated(
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
                                backgroundColor = gridColor,
                                convexTopLeftFactor = convexCorners?.topLeft?.animatable?.value ?: 0f,
                                convexTopRightFactor = convexCorners?.topRight?.animatable?.value ?: 0f,
                                convexBottomLeftFactor = convexCorners?.bottomLeft?.animatable?.value ?: 0f,
                                convexBottomRightFactor = convexCorners?.bottomRight?.animatable?.value ?: 0f
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
                                    topLeftFactor = corners.topLeft.animatable.value,
                                    topRightFactor = corners.topRight.animatable.value,
                                    bottomLeftFactor = corners.bottomLeft.animatable.value,
                                    bottomRightFactor = corners.bottomRight.animatable.value
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

/**
 * Animated version of drawCornerUnderlay that supports smooth rounded square to quarter-circle transitions
 * for convex corners using animated factors (0f to 1f).
 *
 * Animation: Square (factor=0) -> Rounded corner -> Quarter-circle effect (factor=1)
 * Only the corner touching the piece edges is rounded, creating the convex corner effect.
 */
@Suppress("UNUSED_PARAMETER")
private fun DrawScope.drawCornerUnderlayAnimated(
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
    backgroundColor: Color,
    convexTopLeftFactor: Float,
    convexTopRightFactor: Float,
    convexBottomLeftFactor: Float,
    convexBottomRightFactor: Float
) {
    val overlap = 1f
    val newCellSize = cellSize + overlap
    val newX = x - overlap / 2
    val newY = y - overlap / 2
    val newCornerRadius = cornerRadius * (newCellSize / cellSize)

    val arcScale = 2.0f
    val fullArcRadius = newCornerRadius * arcScale
    val smallRectSize = Size(fullArcRadius / 2f, fullArcRadius / 2f)

    // Make the animated square 1px larger and move it 1px outward
    val animatedRectSize = Size(smallRectSize.width + 1f, smallRectSize.height + 1f)

    // TOP-LEFT convex corner (animates bottom-right corner of the square)
    if (convexTopLeftFactor > 0f) {
        val rectTopLeftTL = Offset(newX - smallRectSize.width, newY - smallRectSize.height)
        val rectTopLeftTLInset = Offset(rectTopLeftTL.x + 1f, rectTopLeftTL.y + 1f)
        val start = rectTopLeftTLInset + Offset(smallRectSize.width / 2f, 0f)
        val end = rectTopLeftTLInset + Offset(0f, smallRectSize.height / 2f)

        // Always draw the colored background rectangle
        if (colorTop == colorLeft) {
            drawRect(color = colorTop, topLeft = rectTopLeftTLInset, size = smallRectSize)
        } else {
            val brush = Brush.linearGradient(listOf(colorTop, colorLeft), start = start, end = end)
            drawRect(brush = brush, topLeft = rectTopLeftTLInset, size = smallRectSize)
        }

        // Draw rounded square on top (background color), 1px larger and moved 1px outward
        // Only animate the bottom-right corner (the one touching the piece)
        val animatedTopLeft = Offset(rectTopLeftTLInset.x - 1f, rectTopLeftTLInset.y - 1f)
        val animatedCornerRadius = animatedRectSize.width * convexTopLeftFactor
        val roundedSquarePath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = animatedTopLeft.x,
                    top = animatedTopLeft.y,
                    right = animatedTopLeft.x + animatedRectSize.width,
                    bottom = animatedTopLeft.y + animatedRectSize.height,
                    topLeftCornerRadius = CornerRadius.Zero,
                    topRightCornerRadius = CornerRadius.Zero,
                    bottomLeftCornerRadius = CornerRadius.Zero,
                    bottomRightCornerRadius = CornerRadius(animatedCornerRadius)
                )
            )
        }
        drawPath(roundedSquarePath, backgroundColor)
    }

    // TOP-RIGHT convex corner (animates bottom-left corner of the square)
    if (convexTopRightFactor > 0f) {
        val rectTopLeftTR = Offset(newX + newCellSize, newY - smallRectSize.height)
        val rectTopLeftTRInset = Offset(rectTopLeftTR.x - 1f, rectTopLeftTR.y + 1f)
        val start = rectTopLeftTRInset + Offset(smallRectSize.width / 2f, 0f)
        val end = rectTopLeftTRInset + Offset(smallRectSize.width, smallRectSize.height / 2f)

        if (colorTop == colorRight) {
            drawRect(color = colorTop, topLeft = rectTopLeftTRInset, size = smallRectSize)
        } else {
            val brush = Brush.linearGradient(listOf(colorTop, colorRight), start = start, end = end)
            drawRect(brush = brush, topLeft = rectTopLeftTRInset, size = smallRectSize)
        }

        // Move 1px right (outward) and 1px up, and make 1px larger
        val animatedTopLeft = Offset(rectTopLeftTRInset.x, rectTopLeftTRInset.y - 1f)
        val animatedCornerRadius = animatedRectSize.width * convexTopRightFactor
        val roundedSquarePath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = animatedTopLeft.x,
                    top = animatedTopLeft.y,
                    right = animatedTopLeft.x + animatedRectSize.width,
                    bottom = animatedTopLeft.y + animatedRectSize.height,
                    topLeftCornerRadius = CornerRadius.Zero,
                    topRightCornerRadius = CornerRadius.Zero,
                    bottomLeftCornerRadius = CornerRadius(animatedCornerRadius),
                    bottomRightCornerRadius = CornerRadius.Zero
                )
            )
        }
        drawPath(roundedSquarePath, backgroundColor)
    }

    // BOTTOM-LEFT convex corner (animates top-right corner of the square)
    if (convexBottomLeftFactor > 0f) {
        val rectTopLeftBL = Offset(newX - smallRectSize.width, newY + newCellSize)
        val rectTopLeftBLInset = Offset(rectTopLeftBL.x + 1f, rectTopLeftBL.y - 1f)
        val start = rectTopLeftBLInset + Offset(smallRectSize.width / 2f, smallRectSize.height)
        val end = rectTopLeftBLInset + Offset(0f, smallRectSize.height / 2f)

        if (colorBottom == colorLeft) {
            drawRect(color = colorBottom, topLeft = rectTopLeftBLInset, size = smallRectSize)
        } else {
            val brush = Brush.linearGradient(listOf(colorBottom, colorLeft), start = start, end = end)
            drawRect(brush = brush, topLeft = rectTopLeftBLInset, size = smallRectSize)
        }

        // Move 1px left (outward) and 1px down, and make 1px larger
        val animatedTopLeft = Offset(rectTopLeftBLInset.x - 1f, rectTopLeftBLInset.y)
        val animatedCornerRadius = animatedRectSize.width * convexBottomLeftFactor
        val roundedSquarePath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = animatedTopLeft.x,
                    top = animatedTopLeft.y,
                    right = animatedTopLeft.x + animatedRectSize.width,
                    bottom = animatedTopLeft.y + animatedRectSize.height,
                    topLeftCornerRadius = CornerRadius.Zero,
                    topRightCornerRadius = CornerRadius(animatedCornerRadius),
                    bottomLeftCornerRadius = CornerRadius.Zero,
                    bottomRightCornerRadius = CornerRadius.Zero
                )
            )
        }
        drawPath(roundedSquarePath, backgroundColor)
    }

    // BOTTOM-RIGHT convex corner (animates top-left corner of the square)
    if (convexBottomRightFactor > 0f) {
        val rectTopLeftBR = Offset(newX + newCellSize, newY + newCellSize)
        val rectTopLeftBRInset = Offset(rectTopLeftBR.x - 1f, rectTopLeftBR.y - 1f)
        val start = rectTopLeftBRInset + Offset(smallRectSize.width / 2f, smallRectSize.height)
        val end = rectTopLeftBRInset + Offset(smallRectSize.width, smallRectSize.height / 2f)

        if (colorBottom == colorRight) {
            drawRect(color = colorBottom, topLeft = rectTopLeftBRInset, size = smallRectSize)
        } else {
            val brush = Brush.linearGradient(listOf(colorBottom, colorRight), start = start, end = end)
            drawRect(brush = brush, topLeft = rectTopLeftBRInset, size = smallRectSize)
        }

        // Move 1px right and 1px down (both outward), and make 1px larger
        val animatedTopLeft = Offset(rectTopLeftBRInset.x, rectTopLeftBRInset.y)
        val animatedCornerRadius = animatedRectSize.width * convexBottomRightFactor
        val roundedSquarePath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = animatedTopLeft.x,
                    top = animatedTopLeft.y,
                    right = animatedTopLeft.x + animatedRectSize.width,
                    bottom = animatedTopLeft.y + animatedRectSize.height,
                    topLeftCornerRadius = CornerRadius(animatedCornerRadius),
                    topRightCornerRadius = CornerRadius.Zero,
                    bottomLeftCornerRadius = CornerRadius.Zero,
                    bottomRightCornerRadius = CornerRadius.Zero
                )
            )
        }
        drawPath(roundedSquarePath, backgroundColor)
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

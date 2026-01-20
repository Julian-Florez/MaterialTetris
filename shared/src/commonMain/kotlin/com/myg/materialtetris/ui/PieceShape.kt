package com.myg.materialtetris.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.myg.materialtetris.model.Tetromino
import kotlin.math.*

// Helper simple 2D point
private data class P(val x: Float, val y: Float) {
    operator fun minus(o: P) = P(x - o.x, y - o.y)
    operator fun plus(o: P) = P(x + o.x, y + o.y)
    operator fun times(s: Float) = P(x * s, y * s)
}

private fun P.length(): Float = sqrt(x * x + y * y)
private fun P.normalize(): P { val l = length(); return if (l == 0f) P(0f,0f) else P(x / l, y / l) }
private fun cross(a: P, b: P) = a.x * b.y - a.y * b.x

fun DrawScope.drawPiece(
    piece: Tetromino,
    cellSize: Float,
    color: Color
) {
    val shape = piece.shape
    val n = shape.size
    val radius = cellSize * 0.25f

    // Build set of edge segments on the grid as integer-coordinates (grid lines)
    data class IPoint(val x: Int, val y: Int)
    data class ISeg(val a: IPoint, val b: IPoint)

    val occupied = { r: Int, c: Int -> if (r < 0 || c < 0 || r >= n || c >= n) false else shape[r][c] != 0 }

    val segments = mutableListOf<ISeg>()

    for (r in 0 until n) {
        for (c in 0 until n) {
            if (!occupied(r, c)) continue
            // top edge: (c, r) -> (c+1, r)
            if (!occupied(r - 1, c)) segments.add(ISeg(IPoint(c, r), IPoint(c + 1, r)))
            // right edge: (c+1, r) -> (c+1, r+1)
            if (!occupied(r, c + 1)) segments.add(ISeg(IPoint(c + 1, r), IPoint(c + 1, r + 1)))
            // bottom edge: (c+1, r+1) -> (c, r+1)
            if (!occupied(r + 1, c)) segments.add(ISeg(IPoint(c + 1, r + 1), IPoint(c, r + 1)))
            // left edge: (c, r+1) -> (c, r)
            if (!occupied(r, c - 1)) segments.add(ISeg(IPoint(c, r + 1), IPoint(c, r)))
        }
    }

    if (segments.isEmpty()) return

    // Build adjacency map
    val adj = mutableMapOf<IPoint, MutableList<IPoint>>()
    fun addAdj(a: IPoint, b: IPoint) { adj.getOrPut(a) { mutableListOf() }.add(b) }
    for (s in segments) {
        addAdj(s.a, s.b)
        addAdj(s.b, s.a)
    }

    // Trace closed loops
    val usedEdges = mutableSetOf<Pair<IPoint, IPoint>>()
    val loops = mutableListOf<List<IPoint>>()

    for (s in segments) {
        val key = Pair(s.a, s.b)
        if (usedEdges.contains(key)) continue
        val loop = mutableListOf<IPoint>()
        var cur = s.a
        var prev: IPoint? = null
        while (true) {
            loop.add(cur)
            val neighbors = adj[cur] ?: break
            // pick next neighbor that's not prev or, if only one, pick it
            val next = neighbors.firstOrNull { it != prev } ?: neighbors.first()
            usedEdges.add(Pair(cur, next))
            prev = cur
            cur = next
            if (cur == loop.first()) break
            // safety
            if (loop.size > 1000) break
        }
        if (loop.size > 1) loops.add(loop)
    }

    if (loops.isEmpty()) return

    // We'll draw all loops (normally there's a single outer loop for tetrominos)
    val path = Path()

    for (loop in loops) {
        // convert to float points scaled by cellSize
        val pts = loop.map { P(it.x * cellSize, it.y * cellSize) }
        if (pts.size < 2) continue

        // compute orientation by signed area
        var area = 0f
        for (i in pts.indices) {
            val a = pts[i]
            val b = pts[(i + 1) % pts.size]
            area += (a.x * b.y - b.x * a.y)
        }
        val orientation = if (area >= 0f) 1 else -1 // 1 == CCW

        // Start at first point
        // For rounded corners we iterate vertices and use quadraticTo with a control on the bisector
        fun cornerIndex(i: Int) = (i + pts.size) % pts.size

        // Move to first adjusted start
        // We'll compute start as first point moved towards next by radius (limited by edge length)
        val first = pts[0]
        val next = pts[1]
        val v01 = (next - first).normalize()
        val start = first + v01 * min(radius, (next - first).length() / 2f)
        path.moveTo(start.x, start.y)

        for (i in 1..pts.size) {
            val iPrev = cornerIndex(i - 1)
            val iCur = cornerIndex(i)
            val iNext = cornerIndex(i + 1)
            val pPrev = pts[iPrev]
            val pCur = pts[iCur]
            val pNext = pts[iNext]

            val vIn = (pCur - pPrev).normalize()
            val vOut = (pNext - pCur).normalize()

            // limit offset to half edge length to avoid crossing
            val lenIn = (pCur - pPrev).length()
            val lenOut = (pNext - pCur).length()
            val offIn = min(radius, lenIn / 2f)
            val offOut = min(radius, lenOut / 2f)

            val startPt = pCur - vIn * offIn
            val endPt = pCur + vOut * offOut

            // bisector direction (points roughly into corner). Normalize sum; if opposite direction (colinear) handle as straight
            var bis = (vIn * -1f + vOut).normalize() // points towards corner bisector
            if (bis.x.isNaN() || bis.y.isNaN()) bis = P(0f,0f)

            // compute angle sign to detect convex vs concave
            val angCross = cross(vIn, vOut)
            val turnSign = if (angCross >= 0f) 1 else -1
            val isConvex = turnSign == orientation

            val control = if (isConvex) {
                // convex corner: control outward (away from interior) to bulge outside
                // move control along bisector outward by radius
                pCur + bis * (radius)
            } else {
                // concave corner: control inward (towards interior) to create inward notch
                pCur - bis * (radius)
            }

            // Draw line to startPt (from previous end)
            path.lineTo(startPt.x, startPt.y)
            // Draw quadratic bezier to endPt with control point
            path.quadraticTo(control.x, control.y, endPt.x, endPt.y)
        }

        path.close()
    }

    // Draw path using canvas
    drawContext.canvas.drawPath(
        path = path,
        paint = Paint().apply { this.color = color }
    )
}

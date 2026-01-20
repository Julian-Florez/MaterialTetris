package com.myg.materialtetris.model

import java.util.concurrent.atomic.AtomicInteger

// Represents a Tetris piece
data class Tetromino(
    val id: Int,
    val type: Int,
    var shape: Array<Array<Int>>,
    var x: Int, // Board position (column)
    var y: Int,  // Board position (row)
    var rotation: Int = 0
) {
    // Added equals and hashCode to handle array content comparison
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Tetromino

        if (id != other.id) return false
        if (type != other.type) return false
        if (!shape.contentDeepEquals(other.shape)) return false
        if (x != other.x) return false
        if (y != other.y) return false
        if (rotation != other.rotation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + type
        result = 31 * result + shape.contentDeepHashCode()
        result = 31 * result + x
        result = 31 * result + y
        result = 31 * result + rotation
        return result
    }
}

object PieceIdProvider {
    private val nextId = AtomicInteger()
    fun next() = nextId.incrementAndGet()
}

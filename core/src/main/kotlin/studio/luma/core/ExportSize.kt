package studio.luma.core

import kotlin.math.roundToInt

data class ExportSize(val width: Int, val height: Int) {
    val longEdge: Int get() = maxOf(width, height)
    val isValid: Boolean get() = width in 64..8192 && height in 64..8192 && width.toLong() * height <= 32_000_000

    fun atLongEdge(edge: Int): ExportSize {
        require(width > 0 && height > 0 && edge in 0..8192)
        if (edge == 0) return this
        val scale = edge.toDouble() / longEdge
        return ExportSize((width * scale).roundToInt().coerceAtLeast(1), (height * scale).roundToInt().coerceAtLeast(1))
    }

    fun fittedLongEdge(container: ExportSize): Int {
        require(width > 0 && height > 0 && container.isValid)
        val scale = minOf(container.width.toDouble() / width, container.height.toDouble() / height)
        return (longEdge * scale).roundToInt().coerceAtLeast(1)
    }
}

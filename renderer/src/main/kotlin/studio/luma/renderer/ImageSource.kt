package studio.luma.renderer

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.ColorSpace
import android.graphics.Matrix
import studio.luma.core.Transform
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

object ImageSource {
    /** ImageDecoder applies EXIF orientation. Software allocation is needed for GL upload. */
    fun decode(file: File, maxEdge: Int = 0): Bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
        val w = info.size.width; val h = info.size.height
        val scale = if (maxEdge > 0) minOf(1.0, maxEdge.toDouble() / max(w, h)) else 1.0
        val tw = (w * scale).roundToInt().coerceAtLeast(1); val th = (h * scale).roundToInt().coerceAtLeast(1)
        require(tw.toLong() * th <= 32_000_000L) { "Imagem muito grande. Escolha uma resolução menor (máximo 32 MP nesta versão)." }
        require(tw.toLong() * th * 4 < Runtime.getRuntime().maxMemory() * .6) { "Memória insuficiente para abrir nesta resolução. Escolha um tamanho de exportação menor." }
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
        decoder.setTargetSize(tw, th)
    }

    fun dimensions(file: File): Pair<Int, Int> {
        var dimensions = 1 to 1
        val tiny = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
            dimensions = info.size.width to info.size.height
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE; decoder.setTargetSize(1, 1)
        }
        tiny.recycle(); return dimensions
    }

    fun transform(input: Bitmap, transform: Transform): Bitmap {
        transform.validate()
        val c = transform.crop
        val x = (c.left * input.width).roundToInt().coerceIn(0, input.width - 1)
        val y = (c.top * input.height).roundToInt().coerceIn(0, input.height - 1)
        val w = ((c.right - c.left) * input.width).roundToInt().coerceIn(1, input.width - x)
        val h = ((c.bottom - c.top) * input.height).roundToInt().coerceIn(1, input.height - y)
        val matrix = Matrix().apply {
            postScale(if (transform.flipX) -1f else 1f, if (transform.flipY) -1f else 1f)
            postRotate(transform.quarterTurns * 90f)
        }
        return Bitmap.createBitmap(input, x, y, w, h, matrix, true)
    }
}

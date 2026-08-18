package xyz.xszq.bot.meme

import korlibs.io.file.VfsFile
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.reply
import xyz.xszq.bot.useTempFile
import kotlin.math.*

class Spherize {
    fun spherize(
        input: SkikoImageData,
        reverse: Boolean = false
    ): SkikoImageData {
        val width = input.width
        val height = input.height
        val cx = width  / 2.0
        val cy = height / 2.0
        val maxRad = min(cx, cy)
        val out = ByteArray(input.pixels.size)

        (0 until height).forEach { y ->
            (0 until width).forEach { x ->
                val dx = (x - cx) / maxRad
                val dy = (y - cy) / maxRad
                val r  = hypot(dx, dy)

                if (r <= 1.0) {
                    val rMapped = if (reverse) {
                        sin(r * PI / 2.0)
                    } else {
                        asin(r) * 2.0 / PI
                    }
                    val scale = if (r != 0.0) rMapped / r else 1.0

                    val srcX = cx + dx * scale * maxRad
                    val srcY = cy + dy * scale * maxRad

                    samplePixel(input, srcX, srcY, out, x, y)
                } else {
                    val offset = (y * width + x) * 4
                    copyPixel(input.pixels, offset, out, offset)
                }
            }
        }
        return SkikoImageData(width, height, out)
    }

    suspend fun handle(
        event: MessageEvent,
        input: VfsFile
    ) = useTempFile { normal ->
        val source = input.readSkikoImage()
        spherize(source, false).toSkiaImage().use { img -> normal.writeBytes(img.encodePNG()) }
        useTempFile { reversed ->
            spherize(source, true).toSkiaImage().use { img -> reversed.writeBytes(img.encodePNG()) }
            event.reply(Image(normal))
            event.reply(Image(reversed))
        }
    }

    private fun samplePixel(
        input: SkikoImageData,
        srcX: Double,
        srcY: Double,
        out: ByteArray,
        x: Int,
        y: Int
    ) {
        val clampedX = srcX.coerceIn(0.0, (input.width - 1).toDouble())
        val clampedY = srcY.coerceIn(0.0, (input.height - 1).toDouble())

        val x0 = floor(clampedX).toInt()
        val y0 = floor(clampedY).toInt()
        val x1 = (x0 + 1).coerceAtMost(input.width - 1)
        val y1 = (y0 + 1).coerceAtMost(input.height - 1)
        val fx = clampedX - x0
        val fy = clampedY - y0

        val i00 = (y0 * input.width + x0) * 4
        val i10 = (y0 * input.width + x1) * 4
        val i01 = (y1 * input.width + x0) * 4
        val i11 = (y1 * input.width + x1) * 4
        val offset = (y * input.width + x) * 4

        (0 until 4).forEach { channel ->
            val c00 = input.pixels[i00 + channel].toInt() and 0xFF
            val c10 = input.pixels[i10 + channel].toInt() and 0xFF
            val c01 = input.pixels[i01 + channel].toInt() and 0xFF
            val c11 = input.pixels[i11 + channel].toInt() and 0xFF

            val top = c00 + (c10 - c00) * fx
            val bottom = c01 + (c11 - c01) * fx
            val value = (top + (bottom - top) * fy).roundToInt()
            out[offset + channel] = value.toByte()
        }
    }
}
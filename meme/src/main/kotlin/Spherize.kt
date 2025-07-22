package xyz.xszq.bot

import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.Bitmap32
import korlibs.image.format.PNG
import korlibs.image.format.encode
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.message.Image
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

class Spherize {
    fun spherize(
        input: Bitmap,
        reverse: Boolean = false
    ): Bitmap {
        val width = input.width
        val height = input.height
        val cx = width  / 2.0
        val cy = height / 2.0
        val maxRad = min(cx, cy)
        val out = Bitmap32(width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {
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

                    val color = input.getRgbaSampled(srcX.toFloat(), srcY.toFloat())
                    out.setRgba(x, y, color)
                } else {
                    val color = input.getRgbaClamped(x, y)
                    out.setRgba(x, y, color)
                }
            }
        }
        return out
    }
    suspend fun handle(
        event: MessageEvent,
        input: Bitmap
    ) = useTempFile { normal ->
        normal.writeBytes(spherize(input, false).encode(PNG))
        useTempFile { reversed ->
            reversed.writeBytes(spherize(input, true).encode(PNG))
            event.reply(Image(normal))
            event.reply(Image(reversed))
        }
    }
}
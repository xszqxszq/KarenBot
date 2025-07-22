package xyz.xszq.bot

import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.Bitmap32
import korlibs.image.format.PNG
import korlibs.image.format.encode
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.message.Image

class ImSoHappy {
    fun flip(
        input: Bitmap32,
    ): Pair<Bitmap32, Bitmap32> {
        val width = input.width
        val height = input.height
        val halfWidth = width / 2
        val srcData = input.ints

        val outL2R = Bitmap32(width, height)
        val outR2L = Bitmap32(width, height)
        val dstL2R = outL2R.ints
        val dstR2L = outR2L.ints

        for (y in 0 until height) {
            val rowStart = y * width
            for (x in 0 until width) {
                val srcIndex: Int
                when {
                    x < halfWidth -> {
                        dstL2R[rowStart + x] = srcData[rowStart + x]
                        srcIndex = rowStart + (width - 1 - x)
                        dstR2L[rowStart + x] = srcData[srcIndex]
                    }
                    else -> {
                        srcIndex = rowStart + (width - 1 - x)
                        dstL2R[rowStart + x] = srcData[srcIndex]
                        dstR2L[rowStart + x] = srcData[rowStart + x]
                    }
                }
            }
        }
        return Pair(outL2R, outR2L)
    }
    suspend fun handle(
        event: MessageEvent,
        input: Bitmap
    ) {
        val (a, b) = flip(input.toBMP32())
        useTempFile { first ->
            first.writeBytes(a.encode(PNG))
            useTempFile { second ->
                second.writeBytes(b.encode(PNG))
                event.reply(Image(first))
                event.reply(Image(second))
            }
        }
    }
}
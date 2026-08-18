package xyz.xszq.bot.meme

import korlibs.io.file.VfsFile
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.reply
import xyz.xszq.bot.useTempFile

class ImSoHappy {
    fun flip(
        input: SkikoImageData,
    ): Pair<SkikoImageData, SkikoImageData> {
        val width = input.width
        val height = input.height
        val halfWidth = width / 2
        val srcData = input.pixels

        val dstL2R = ByteArray(srcData.size)
        val dstR2L = ByteArray(srcData.size)

        (0 until height).forEach { y ->
            val rowStart = y * width * 4
            (0 until halfWidth).forEach { x ->
                val left = rowStart + x * 4
                val right = rowStart + (width - 1 - x) * 4

                copyPixel(srcData, left, dstL2R, left)
                copyPixel(srcData, left, dstL2R, right)
                copyPixel(srcData, right, dstR2L, left)
                copyPixel(srcData, right, dstR2L, right)
            }
            if (width % 2 == 1) {
                val center = rowStart + halfWidth * 4
                copyPixel(srcData, center, dstL2R, center)
                copyPixel(srcData, center, dstR2L, center)
            }
        }
        return Pair(
            SkikoImageData(width, height, dstL2R),
            SkikoImageData(width, height, dstR2L)
        )
    }

    suspend fun handle(
        event: MessageEvent,
        input: VfsFile
    ) {
        val (a, b) = flip(input.readSkikoImage())
        useTempFile { first ->
            a.toSkiaImage().use { img -> first.writeBytes(img.encodePNG()) }
            event.reply(Image(first))
        }
        useTempFile { second ->
            b.toSkiaImage().use { img -> second.writeBytes(img.encodePNG()) }
            event.reply(Image(second))
        }
    }
}

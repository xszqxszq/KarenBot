package xyz.xszq.bot.meme

import korlibs.io.file.VfsFile
import kotlinx.coroutines.withContext
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.reply
import xyz.xszq.bot.util.cpuDispatcher
import xyz.xszq.bot.util.useTempFile

/**
 * 生成我巨爽左右对称图片
 */
class ImSoHappy {
    /**
     * 生成两种类型的左右对称
     *
     * @return 生成的两张结果图
     */
    fun flip(
        input: SkikoImageData
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

    /**
     * 生成并回复用户
     */
    suspend fun handle(
        event: MessageEvent,
        input: VfsFile
    ) {
        val (a, b) = withContext(cpuDispatcher) {
            flip(input.readSkikoImage())
        }
        useTempFile { first ->
            val png = withContext(cpuDispatcher) {
                a.toSkiaImage().use { img -> img.encodePNG() }
            }
            first.writeBytes(png)
            event.reply(Image(first))
        }
        useTempFile { second ->
            val png = withContext(cpuDispatcher) {
                b.toSkiaImage().use { img -> img.encodePNG() }
            }
            second.writeBytes(png)
            event.reply(Image(second))
        }
    }
}
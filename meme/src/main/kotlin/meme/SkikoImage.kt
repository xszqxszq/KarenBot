package xyz.xszq.bot.meme

import korlibs.io.file.VfsFile
import kotlinx.coroutines.withContext
import org.jetbrains.skia.*
import xyz.xszq.bot.util.cpuDispatcher

/**
 * Skiko 图片数据
 */
data class SkikoImageData(
    val width: Int,
    val height: Int,
    val pixels: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SkikoImageData

        if (width != other.width) return false
        if (height != other.height) return false
        if (!pixels.contentEquals(other.pixels)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + pixels.contentHashCode()
        return result
    }
}

/**
 * 读入 Skiko 图片
 *
 * @return SkikoImageData
 */
suspend fun VfsFile.readSkikoImage(): SkikoImageData = withContext(cpuDispatcher) {
    Image.makeFromEncoded(readBytes()).use { image ->
        Bitmap.makeFromImage(image).use { bitmap ->
            val info = ImageInfo(
                image.width,
                image.height,
                ColorType.RGBA_8888,
                ColorAlphaType.UNPREMUL
            )
            SkikoImageData(image.width, image.height, bitmap.readPixels(info)!!)
        }
    }
}

/**
 * 转为 Skia 的 Image
 */
fun SkikoImageData.toSkiaImage(): Image = Image.makeRaster(
    imageInfo = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL),
    bytes = pixels,
    rowBytes = width * 4
)

/**
 * 编码为 PNG 格式
 */
fun Image.encodePNG(): ByteArray = encodeToData(EncodedImageFormat.PNG).use { it!!.bytes }

private fun norm(s: String) = s.lowercase().replace(" ", "").replace("-", "").replace("_", "")

/**
 * 按字体名与字重匹配系统字体
 *
 * @param names 候选字体名
 * @param weight 目标字重
 * @return 匹配到的字体
 */
fun matchFamily(vararg names: String, weight: Int = 400): Typeface? {
    names.forEach { name ->
        FontMgr.default.matchFamilyStyle(
            name,
            FontStyle(weight, 5, FontSlant.UPRIGHT)
        ) ?.let {
            return it
        }
        val n = norm(name)
        val mgr = FontMgr.default
        (0 until mgr.familiesCount).forEach { i ->
            val family = mgr.getFamilyName(i)
            if (norm(family) != n)
                return@forEach
            val styleSet = mgr.makeStyleSet(i) ?: return@forEach
            (0 until styleSet.count()).forEach { j ->
                val tf = styleSet.getTypeface(j) ?: return@forEach
                if (tf.fontStyle.weight == weight)
                    return tf
            }
            return styleSet.getTypeface(0)
        }
    }
    return null
}

/**
 * 逐像素拷贝
 *
 * @param sourceOffset 源起始字节位置
 * @param targetOffset 目标起始字节位置
 */
fun copyPixel(
    source: ByteArray,
    sourceOffset: Int,
    target: ByteArray,
    targetOffset: Int
) {
    target[targetOffset] = source[sourceOffset]
    target[targetOffset + 1] = source[sourceOffset + 1]
    target[targetOffset + 2] = source[sourceOffset + 2]
    target[targetOffset + 3] = source[sourceOffset + 3]
}
package xyz.xszq.bot.meme

import korlibs.io.file.VfsFile
import org.jetbrains.skia.*

/**
 * RGBA 像素位图数据，内容比较基于像素而非引用
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
 * 从编码图片文件解码为像素位图数据
 *
 * @return 解码出的 RGBA 位图
 */
suspend fun VfsFile.readSkikoImage(): SkikoImageData {
    return Image.makeFromEncoded(readBytes()).use { image ->
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
 * 包装回可绘制的 Skia 图片
 */
fun SkikoImageData.toSkiaImage(): Image = Image.makeRaster(
    imageInfo = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL),
    bytes = pixels,
    rowBytes = width * 4
)

/**
 * 编码为 PNG 字节
 */
fun Image.encodePNG(): ByteArray = encodeToData(EncodedImageFormat.PNG).use { it!!.bytes }

private fun norm(s: String) = s.lowercase().replace(" ", "").replace("-", "").replace("_", "")

/**
 * 按字体名与字重匹配系统字体，名称比较忽略大小写与空格、连字符、下划线
 *
 * @param names 按序尝试的候选字体名
 * @param weight 目标字重
 * @return 匹配到的字体，未找到时返回 null
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
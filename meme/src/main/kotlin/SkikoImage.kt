package xyz.xszq.bot

import korlibs.io.file.VfsFile
import org.jetbrains.skia.*

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

suspend fun VfsFile.readSkikoImage(): SkikoImageData {
    val image = Image.makeFromEncoded(readBytes())
    val bitmap = Bitmap.makeFromImage(image)
    val info = ImageInfo(image.width, image.height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL)
    return SkikoImageData(image.width, image.height, bitmap.readPixels(info)!!)
}

fun SkikoImageData.toSkiaImage(): Image = Image.makeRaster(
    imageInfo = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL),
    bytes = pixels,
    rowBytes = width * 4
)

fun Image.encodePNG(): ByteArray = encodeToData(EncodedImageFormat.PNG)!!.bytes

private fun norm(s: String) = s.lowercase().replace(" ", "").replace("-", "").replace("_", "")

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

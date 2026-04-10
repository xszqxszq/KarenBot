package xyz.xszq.bot

import korlibs.io.file.VfsFile
import org.jetbrains.skia.*

data class SkikoImageData(
    val width: Int,
    val height: Int,
    val pixels: ByteArray
)

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

fun Image.encodePng(): ByteArray = encodeToData(EncodedImageFormat.PNG)!!.bytes

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

package xyz.xszq.shinobu

import dev.matrixlab.webp4j.WebPCodec
import korlibs.image.awt.toAwtNativeImage
import korlibs.image.bitmap.Bitmap
import korlibs.image.color.RGBA
import korlibs.image.font.Font
import korlibs.image.font.getTextBoundsWithGlyphs
import korlibs.image.format.JPEG
import korlibs.image.format.PNG
import korlibs.image.format.readNativeImage
import korlibs.image.text.DefaultStringTextRenderer
import korlibs.image.text.TextAlignment
import korlibs.image.text.TextRenderer
import korlibs.io.file.VfsFile
import korlibs.io.file.extensionLC
import korlibs.memory.extract8
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

typealias RenderedElement = Pair<Element, Bitmap>
typealias Elements = List<RenderedElement>
typealias PackedElements = List<Elements>

fun String.hexToRGBA(opacity: Double? = null): RGBA {
    val int = substring(1).toInt(16)
    return RGBA.Companion.invoke(
        int.extract8(16),
        int.extract8(8),
        int.extract8(0),
        opacity ?.let { (it * 255).toInt() } ?: 0xff
    )
}

fun ByteArray.readBitmap() = listOf(PNG, JPEG).firstNotNullOf { codec ->
    runCatching {
        codec.read(this)
    }.getOrNull()
}
suspend fun <T> countTime(block: suspend () -> T): Pair<Long, T> {
    val start = System.currentTimeMillis()
    val result = block()
    return Pair(System.currentTimeMillis() - start, result)
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> T.deepCopy(): T {
    val type = this::class
    return if (type.isData) {
        deepCopyDataClass()
    } else if (this is Collection<*>) {
        deepCopyCollection() as T
    } else {
        this
    }
}
private fun <T : Any> T.deepCopyDataClass(): T {
    val type = this::class
    val constructor = type.primaryConstructor
        ?: throw IllegalArgumentException("Data class must have a primary constructor")
    val args = constructor.parameters.associateWith { param ->
        @Suppress("UNCHECKED_CAST")
        val prop = type.memberProperties
            .find { it.name == param.name }
                as KProperty1<T, Any?>?
        prop?.get(this)?.deepCopy()
    }
    return constructor.callBy(args)
}
private fun <T> Collection<T>.deepCopyCollection(): Collection<T> {
    return this.map { it?.deepCopy() ?: it }
}

fun Font.measureTextGlyphs(
    size: Int,
    text: String,
    renderer: TextRenderer<String> = DefaultStringTextRenderer,
    align: TextAlignment = TextAlignment.BASELINE_LEFT
) = getTextBoundsWithGlyphs(size.toDouble(), text, renderer, align)

fun Bitmap.mask(
    mask: Bitmap
): Bitmap {
    forEach { n, x, y ->
        val now = getRgba(x, y)
        if (x < mask.width && y < mask.height)
            setRgba(x, y, RGBA(now.r, now.g, now.b, mask.getRgba(x, y).a))
        else
            setRgba(x, y, RGBA(now.r, now.g, now.b, 0))
    }
    return this
}

fun Bitmap.transparent(
    opacity: Double
): Bitmap {
    forEach { n, x, y ->
        val now = getRgba(x, y)
        setRgba(x, y, RGBA(now.r, now.g, now.b, (opacity * now.a).toInt()))
    }
    return this
}

suspend fun VfsFile.readAsImage() = when (extensionLC) {
    "png", "jpg" -> readNativeImage()
    "webp" -> WebPCodec.decodeImage(readBytes()).toAwtNativeImage()
    else -> null
}
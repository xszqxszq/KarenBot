package xyz.xszq.bot.image

import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.extract
import korlibs.image.bitmap.sliceWithSize
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.font.FolderBasedNativeSystemFontProvider
import korlibs.image.font.Font
import korlibs.image.font.TtfFont
import korlibs.image.font.getTextBoundsWithGlyphs
import korlibs.image.paint.Paint
import korlibs.image.text.DefaultStringTextRenderer
import korlibs.image.text.HorizontalAlign
import korlibs.image.text.TextAlignment
import korlibs.image.text.TextRenderer
import korlibs.image.vector.Context2d
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.math.geom.Point
import korlibs.math.geom.vector.LineJoin
import kotlinx.coroutines.*
import xyz.xszq.bot.rhythmgame.image.ItemProperties
import xyz.xszq.nereides.hexToRGBA


fun Font.ellipsize(rawText: String, fontSize: Float, maxWidth: Float, xScale: Float = 1.0F): String {
    var metrics = measureTextGlyphs(fontSize, rawText)
    if (metrics.metrics.width * xScale < maxWidth)
        return rawText
    var text = rawText
    while (text.isNotEmpty()) {
        metrics = measureTextGlyphs(fontSize, "$text...")
        if (metrics.metrics.width * xScale < maxWidth)
            return "$text..."
        text = text.substring(0 until text.length - 1)
    }
    return "$text..."
}
fun Context2d.drawText(
    rawText: String,
    pos: Point,
    color: RGBA = Colors.BLACK,
    font: TtfFont,
    fontSize: Float,
    align: TextAlignment = TextAlignment.LEFT,
    xScale: Float = 1.0F,
    stroke: Float = 0.0F,
    strokeColor: RGBA = Colors.BLACK,
    ellipsizeWidth: Float? = null
) {
    this.font = font
    this.fontSize = fontSize
    this.alignment = align
    if (stroke != 0.0F) {
        this.lineWidth = stroke
        this.strokeStyle = strokeColor
    } else {
        this.lineWidth = 0.0F
    }
    // TODO: Submit issue to korim to fix alignment, this is only a TEMPORARY solution
    val text = ellipsizeWidth ?.let {
        font.ellipsize(rawText, fontSize, ellipsizeWidth, xScale)
    } ?: rawText
    if (xScale != 1.0F) {
        drawTextWithXScale(text, pos, xScale, createColor(color), createColor(strokeColor))
    } else {
        val offsetX = when (this.alignment.horizontal) {
            HorizontalAlign.RIGHT -> -1 * (font.measureTextGlyphs(fontSize, text).metrics.width)
            HorizontalAlign.CENTER -> -1 * (font.measureTextGlyphs(fontSize, text).metrics.width / 2)
            else -> 0.0F
        }
        if (stroke != 0.0F)
            strokeTextSafe(text, Point(pos.x + offsetX, pos.y))
        this.fillStyle = createColor(color)
        fillText(text, Point(pos.x + offsetX, pos.y))
    }
    this.lineWidth = 0.0F
}
fun Context2d.drawTextWithXScale(
    text: String,
    pos: Point,
    xScale: Float = 1.0F,
    fillStyle: Paint,
    strokeStyle: Paint
) {
    val offsetX = when (this.alignment.horizontal) {
        HorizontalAlign.RIGHT -> -1 * (font!!.measureTextGlyphs(fontSize, text).metrics.width * xScale)
        HorizontalAlign.CENTER -> -1 * (font!!.measureTextGlyphs(fontSize, text).metrics.width * xScale / 2)
        else -> 0.0F
    }
    var x = pos.x + offsetX
    text.forEach { c ->
        val metrics = font!!.measureTextGlyphs(fontSize, c.toString())
        if (lineWidth != 0.0F) {
            this.strokeStyle = strokeStyle
            strokeTextSafe(c.toString(), Point(x, pos.y))
        }
        this.fillStyle = fillStyle
        fillText(c.toString(), Point(x, pos.y))
        x += metrics.glyphs.first().metrics.xadvance * xScale
    }
}

fun Context2d.drawText(
    text: String,
    attr: ItemProperties,
    align: TextAlignment = TextAlignment.LEFT,
    ellipsizeWidth: Float? = null,
    font: TtfFont
) = drawText(
    text,
    Point(attr.x, attr.y),
    attr.color.hexToRGBA(),
    font,
    attr.size.toFloat(),
    align,
    attr.xScale,
    attr.stroke,
    attr.strokeColor.hexToRGBA(),
    ellipsizeWidth
)
fun Context2d.drawTextRelative(
    text: String,
    pos: Point,
    attr: ItemProperties,
    align: TextAlignment = TextAlignment.LEFT,
    ellipsizeWidth: Float? = null,
    font: TtfFont
)  = drawText(
    text,
    Point(pos.x + attr.x, pos.y + attr.y),
    attr.color.hexToRGBA(),
    font,
    attr.size.toFloat(),
    align,
    attr.xScale,
    attr.stroke,
    attr.strokeColor.hexToRGBA(),
    ellipsizeWidth
)
fun Bitmap.randomSlice(size: Int = 66) =
    sliceWithSize((0..width - size).random(), (0..height - size).random(), size, size).extract()
@OptIn(DelicateCoroutinesApi::class, InternalCoroutinesApi::class)
val globalFontRegistry = FolderBasedNativeSystemFontProvider(
    Dispatchers.IO.newCoroutineContext(GlobalScope.coroutineContext), listOf(localCurrentDirVfs["font"].absolutePath)
)

@Suppress("UNCHECKED_CAST")
fun <T> Font.measureTextGlyphs(
    size: Float,
    text: T,
    renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>,
    align: TextAlignment = TextAlignment.BASELINE_LEFT
) = getTextBoundsWithGlyphs(size, text, renderer, align)
fun Context2d.strokeTextSafe(text: String, pos: Point) {
    lineJoin = LineJoin.ROUND
    strokeText(text, pos)
}
package xyz.xszq.bot.image

import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.NativeImage
import korlibs.image.bitmap.context2d
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.text.HorizontalAlign
import korlibs.math.geom.Point
import korlibs.math.geom.vector.LineJoin
import xyz.xszq.bot.image.BuildImage.Companion.defaultFallbackFonts
import xyz.xszq.bot.image.BuildImage.Companion.getProperFont
import xyz.xszq.nereides.sumOf

class Text2Image(
    var lines: List<Line>,
    private val spacing: Int = 4,
    private val fill: RGBA = Colors.BLACK,
    private val strokeWidth: Float = 0.0F,
    private val strokeFill: RGBA? = null
) {
    val width
        get() = lines.maxOf { it.width }
    val height
        get() = lines.sumOf { it.ascent } + lines.last().descent + spacing * (lines.size - 1) + strokeWidth * 2

    fun drawOnImage(image: Bitmap, pos: Point) {
        var top = pos.y
        image.context2d {
            lineJoin = LineJoin.ROUND
            kotlin.runCatching {
                lines.forEach { line ->
                    font = line.font
                    fontSize = line.fontSize
                    var left = pos.x
                    if (line.align == HorizontalAlign.CENTER)
                        left += (width - line.width) / 2
                    else if (line.align == HorizontalAlign.RIGHT)
                        left += width - line.width

                    val point = Point(left, top + line.ascent)
                    if (strokeWidth != 0.0F) {
                        strokeStyle = strokeFill ?: Colors.WHITE
                        lineWidth = strokeWidth
                        strokeText(line.chars, point)
                    }
                    fillStyle = fill
                    fillText(line.chars, point)
                    top += line.ascent + spacing
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }
    suspend fun wrap(width: Double): Text2Image {
        val newLines = mutableListOf<Line>()
        lines.forEach { line ->
            newLines.addAll(line.wrap(width))
        }
        lines = newLines
        return this
    }
    fun toImage(bgColor: RGBA? = null, padding: List<Int> = listOf(0, 0)): Bitmap {
        var paddingLeft = padding[0]
        var paddingRight = padding[0]
        var paddingTop = padding[1]
        var paddingBottom = padding[1]
        if (padding.size == 4) {
            paddingLeft = padding[0]
            paddingTop = padding[1]
            paddingRight = padding[2]
            paddingBottom = padding[3]
        }
        return NativeImage(
            width.toInt() + paddingLeft + paddingRight,
            height.toInt() + paddingTop + paddingBottom
        ).modify {
            lineJoin = LineJoin.ROUND
            bgColor?.let {
                fillStyle = it
                fillRect(0, 0, this.width, this.height)
            }
            var top = paddingTop.toDouble()
            kotlin.runCatching {
                lines.forEach { line ->
                    font = line.font
                    fontSize = line.fontSize
                    val now = Point(paddingLeft.toDouble() + when (line.align) {
                        HorizontalAlign.CENTER -> (width - line.width) / 2
                        HorizontalAlign.RIGHT -> width - line.width
                        else -> 0.0F
                    }, top + line.ascent)
                    strokeFill ?.let {
                        if (strokeWidth != 0.0F) {
                            lineWidth = strokeWidth
                            strokeStyle = it
                            strokeText(line.chars, now)
                        }
                    }
                    fillStyle = fill
                    fillText(line.chars,now)
                    top += line.ascent + spacing
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }
    companion object {

        fun fromText(
            text: String,
            fontSize: Int = 16,
            fill: RGBA = Colors.BLACK,
            spacing: Int = 4,
            align: HorizontalAlign = HorizontalAlign.LEFT,
            strokeWidth: Int = 0,
            strokeFill: RGBA? = null,
            fontFallback: Boolean = true,
            fontName: String? = null,
            fallbackFonts: List<String> = defaultFallbackFonts
        ): Text2Image {
            if (!fontFallback) {
                if (fontName.isNullOrBlank())
                    throw IllegalArgumentException("`fontFallback` 为 `false` 时必须指定 `fontName`")
            }

            return Text2Image(text.split('\n', '\r').map {
                Line(it, align, fontSize.toFloat(), getProperFont(
                it, fontName=fontName, fallbackFonts=fallbackFonts)
            ) }, spacing, fill, strokeWidth.toFloat(), strokeFill)
        }
    }
}
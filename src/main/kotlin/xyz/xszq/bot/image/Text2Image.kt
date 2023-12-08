package xyz.xszq.bot.image

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.Font
import com.soywiz.korim.font.measureTextGlyphs
import com.soywiz.korim.text.HorizontalAlign
import kotlin.properties.Delegates

class Text2Image(
    var lines: List<Line>,
    private val spacing: Int = 4,
    private val fill: RGBA = Colors.BLACK,
    private val strokeWidth: Double = 0.0,
    private val strokeFill: RGBA? = null
) {
    val width
        get() = lines.maxOf { it.width }
    val height
        get() = lines.sumOf { it.ascent } + lines.last().descent + spacing * (lines.size - 1) + strokeWidth * 2

    fun drawOnImage(image: Bitmap, pos: Pair<Double, Double>) {
        var top = pos.second
        image.context2d {
            kotlin.runCatching {
                lines.forEach { line ->
                    font = line.font
                    fontSize = line.fontSize.toDouble()
                    var left = pos.first
                    if (line.align == HorizontalAlign.CENTER)
                        left += (width - line.width) / 2
                    else if (line.align == HorizontalAlign.RIGHT)
                        left += width - line.width

                    val x = left
                    val y = top + line.ascent
                    if (strokeWidth != 0.0) {
                        strokeStyle = strokeFill ?: Colors.WHITE
                        lineWidth = strokeWidth
                        strokeText(line.chars, x, y)
                    }
                    fillStyle = fill
                    fillText(line.chars, x, y)
                    top += line.ascent + spacing
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }
    fun wrap(width: Double): Text2Image {
        val newLines = mutableListOf<Line>()
        lines.forEach { line ->
            newLines.addAll(line.wrap(width))
        }
        lines = newLines
        return this
    }
    companion object {

        fun fromText(
            text: String,
            fontSize: Int = 16,
            fill: RGBA = Colors.BLACK,
            spacing: Int = 4,
            align: HorizontalAlign = HorizontalAlign.CENTER,
            strokeWidth: Int = 0,
            strokeFill: RGBA? = null,
            fontFallback: Boolean = true,
            fontName: String? = null,
            fallbackFonts: List<String> = listOf()
        ): Text2Image {
            if (!fontFallback) {
                if (fontName.isNullOrBlank())
                    throw IllegalArgumentException("`fontFallback` 为 `false` 时必须指定 `fontName`")
            }

            return Text2Image(text.split('\n', '\r').map { Line(it, align, fontSize, getProperFont(
                it, fontName=fontName, fallbackFonts=fallbackFonts)
            ) }, spacing, fill, strokeWidth.toDouble(), strokeFill)
        }
    }
}
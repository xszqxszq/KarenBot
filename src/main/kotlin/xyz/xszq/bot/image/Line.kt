package xyz.xszq.bot.image

import com.soywiz.korim.font.Font
import com.soywiz.korim.font.measureTextGlyphs
import com.soywiz.korim.text.HorizontalAlign
import kotlin.math.max

class Line(
    val chars: String,
    val align: HorizontalAlign = HorizontalAlign.LEFT,
    val fontSize: Int = 16,
    val font: Font = globalFontRegistry.defaultFont()
) {
    val width: Double
        get() = font.measureTextGlyphs(fontSize.toDouble(), chars).glyphs.sumOf { it.metrics.xadvance }
    val height
        get() = font.measureTextGlyphs(fontSize.toDouble(), chars).metrics.height
    val ascent
        get() = font.measureTextGlyphs(fontSize.toDouble(), chars.ifBlank { "A" }).metrics.ascent
    val descent
        get() = -font.measureTextGlyphs(fontSize.toDouble(), chars.ifBlank { "A" }).metrics.descent
    fun wrap(width: Double): List<Line> {
        val result = mutableListOf<Line>()
        var current = ""
        chars.forEach { char ->
            if (font.measureTextGlyphs(fontSize.toDouble(), current + char).metrics.width > width) {
                result.add(Line(current, align, fontSize, font))
                current = ""
            }
            current += char
        }
        if (current.isNotEmpty())
            result.add(Line(current, align, fontSize, font))
        return result
    }
}
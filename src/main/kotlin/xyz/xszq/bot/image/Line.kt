package xyz.xszq.bot.image

import korlibs.image.font.TtfFont
import korlibs.image.text.HorizontalAlign
import kotlinx.coroutines.runBlocking
import xyz.xszq.bot.image.BuildImage.Companion.defaultFallbackFonts
import xyz.xszq.bot.image.BuildImage.Companion.fonts
import xyz.xszq.nereides.sumOf


class Line(
    val chars: String,
    val align: HorizontalAlign = HorizontalAlign.LEFT,
    val fontSize: Float = 16F,
    val font: TtfFont = fonts[defaultFallbackFonts.first()]!!
) {
    val width: Float
        get() = runBlocking { font.measureTextGlyphs(fontSize, chars).glyphs.sumOf { it.metrics.xadvance } }
    val height
        get() = runBlocking { font.measureTextGlyphs(fontSize, chars).metrics.height }
    val ascent
        get() = runBlocking { font.measureTextGlyphs(fontSize, chars.ifBlank { "A" }).metrics.ascent }
    val descent
        get() = runBlocking { -font.measureTextGlyphs(fontSize, chars.ifBlank { "A" }).metrics.descent }
    suspend fun wrap(width: Double): List<Line> {
        val result = mutableListOf<Line>()
        var current = ""
        chars.forEach { char ->
            if (kotlin.runCatching {
                font.measureTextGlyphs(fontSize, current + char).metrics.width
            }.getOrDefault((width + 1).toFloat()) > width) { // TODO: Fix this
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
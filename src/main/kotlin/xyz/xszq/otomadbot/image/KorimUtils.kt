package xyz.xszq.otomadbot.image

import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.Font
import com.soywiz.korim.font.measureTextGlyphs
import com.soywiz.korim.text.HorizontalAlign
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korim.text.VerticalAlign
import com.soywiz.korim.vector.Context2d

fun Context2d.text(text: String, x: Number, y: Number, size: Double, color: RGBA = Colors.BLACK,
                   align: TextAlignment = TextAlignment.LEFT, font: Font = this.font!!) {
    this.font = font
    this.fontSize = size
    this.alignment = align
    this.fillStyle = color
    val offsetX = when (this.alignment.horizontal) {
        HorizontalAlign.CENTER -> -1 * textWidth(text) / 2
        HorizontalAlign.RIGHT -> -1 * textWidth(text)
        else -> 0.0
    }
    val offsetY = when (this.alignment.vertical) { // TODO: Fix this
        VerticalAlign.MIDDLE -> -1 * textHeight(text) / 2
        VerticalAlign.BOTTOM -> -1 * textHeight(text)
        else -> 0.0
    }
    fillText(text, x.toDouble() + offsetX, y.toDouble() + offsetY)
}
fun Context2d.textWidth(text: String, size: Double = fontSize, font: Font = this.font!!) =
    font.measureTextGlyphs(size, text).glyphs.maxByOrNull { it.x } !!.let {
        it.x + it.metrics.bounds.width
    }
fun Context2d.textHeight(text: String, size: Double = fontSize, font: Font = this.font!!) =
    font.measureTextGlyphs(size, text).glyphs.maxOf { it.metrics.bounds.height }
fun Context2d.fillBg(color: RGBA) {
    fillStyle = color
    fillRect(0, 0, width, height)
}
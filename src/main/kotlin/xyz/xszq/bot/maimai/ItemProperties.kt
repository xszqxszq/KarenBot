package xyz.xszq.bot.maimai

import com.soywiz.korim.color.Colors
import com.soywiz.korim.paint.Paint
import kotlinx.serialization.Serializable

@Serializable
data class ItemProperties(
    val fontName: String = "",
    val size: Int = 0,
    val x: Int,
    val y: Int,
    val scale: Double = 1.0,
    val xScale: Double = 1.0,
    val color: String = Colors.BLACK.hexStringNoAlpha,
    val stroke: Double = 0.0,
    val strokeColor: String = Colors.BLACK.hexStringNoAlpha
)

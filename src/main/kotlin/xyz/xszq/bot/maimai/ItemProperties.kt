package xyz.xszq.bot.maimai

import korlibs.image.color.Colors
import korlibs.math.geom.Point
import kotlinx.serialization.Serializable

@Serializable
data class ItemProperties(
    val fontName: String = "",
    val size: Int = 0,
    val x: Float,
    val y: Float,
    val scale: Double = 1.0,
    val xScale: Float = 1.0F,
    val color: String = Colors.BLACK.hexStringNoAlpha,
    val stroke: Float = 0.0F,
    val strokeColor: String = Colors.BLACK.hexStringNoAlpha
) {
    val point
        get() = Point(x, y)
}

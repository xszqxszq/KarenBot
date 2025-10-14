package xyz.xszq.bot.theme

import kotlinx.serialization.Serializable

@Serializable(with = SpacingSerializer::class)
data class Spacing(
    val top: Int,
    val bottom: Int,
    val left: Int,
    val right: Int
) {
    constructor(spacing: Int) : this(spacing, spacing, spacing, spacing)
    fun lengthX() = left + right
    fun lengthY() = top + bottom
    fun isZero() = top == 0 && bottom == 0 && left == 0 && right == 0
}

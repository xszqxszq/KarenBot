package xyz.xszq.shinobu.style

/**
 * 文本阴影
 *
 * 阴影只按偏移量在原文本下层重绘一层，不产生模糊
 */
data class TextShadow(
    var color: Int,
    var dx: Float,
    var dy: Float
)
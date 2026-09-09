package xyz.xszq.shinobu.style

/**
 * 四边间距
 *
 * 按上右下左的顺序描述，用作 `margin` 与 `padding`
 */
data class Spacing(
    var top: Float = 0f,
    var right: Float = 0f,
    var bottom: Float = 0f,
    var left: Float = 0f
)
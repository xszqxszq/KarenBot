package xyz.xszq.shinobu.style

/**
 * 文本描边
 *
 * 沿文本笔画轮廓绘制一圈实色描边
 *
 * @property color 描边颜色
 * @property size 描边宽度（像素）
 */
data class TextStroke(
    var color: Int,
    var size: Float
)
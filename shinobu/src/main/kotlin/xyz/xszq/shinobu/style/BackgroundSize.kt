package xyz.xszq.shinobu.style

/**
 * 背景图的尺寸适配方式
 *
 * `AUTO` 按原始尺寸绘制，`STRETCH_FILL` 拉伸铺满容器，`COVER`
 * 等比放大并裁掉溢出部分，`CONTAIN` 等比缩放完整显示
 */
enum class BackgroundSize {
    AUTO,
    STRETCH_FILL,
    COVER,
    CONTAIN
}
package xyz.xszq.shinobu.style

/**
 * 图片在内容区内的适配方式
 *
 * `FILL` 拉伸铺满，`COVER` 等比放大并裁掉溢出部分，`CONTAIN`
 * 等比缩放完整显示，`NONE` 保持原始尺寸
 */
enum class ObjectFit {
    FILL,
    COVER,
    CONTAIN,
    NONE
}
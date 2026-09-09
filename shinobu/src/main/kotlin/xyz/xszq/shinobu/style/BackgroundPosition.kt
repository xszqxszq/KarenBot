package xyz.xszq.shinobu.style

/**
 * 背景图相对容器的对齐位置
 *
 * 仅背景采用 `COVER` 适配时参与定位计算
 */
enum class BackgroundPosition {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER_LEFT,
    CENTER,
    CENTER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT
}
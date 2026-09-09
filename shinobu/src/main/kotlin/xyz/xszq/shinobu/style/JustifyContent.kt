package xyz.xszq.shinobu.style

/**
 * 弹性布局主轴对齐方式
 *
 * 决定主轴剩余空间的分配，取值与 CSS `justify-content` 对应
 */
enum class JustifyContent {
    FLEX_START,
    FLEX_END,
    CENTER,
    SPACE_BETWEEN,
    SPACE_AROUND
}
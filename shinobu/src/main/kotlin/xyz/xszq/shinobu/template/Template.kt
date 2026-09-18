package xyz.xszq.shinobu.template

import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import xyz.xszq.shinobu.dom.Element
import xyz.xszq.shinobu.style.LayoutEngine

/**
 * 渲染模板
 *
 * 持有一批按 `id` 索引的顶层元素与资源管理器，元素经克隆后可
 * 自由修改并渲染为位图
 */
@Suppress("unused")
class Template(
    private val children: Map<String, Element>,
    val resourceManager: ResourceManager
) {
    /**
     * 按顶层元素 `id` 克隆出对应节点
     *
     * 每次调用都会深拷贝，可在副本上修改样式而不影响模板本身
     *
     * @param id 顶层元素的 `id`
     * @return 克隆出的元素
     */
    operator fun get(id: String) = children[id] ?.clone()

    /**
     * 把元素树渲染为位图
     *
     * 依次完成资源解析、布局与绘制后输出位图，位图边长受
     * Skia 上限约束。整个过程处于一次渲染会话中，会话期间取到的
     * 缓存位图保证有效
     *
     * @param element 要渲染的元素树根节点
     * @return 渲染结果位图
     */
    fun render(
        element: Element
    ): Image = ResourceManager.renderSession().use {
        element.resolveResources(resourceManager)
        LayoutEngine.performLayout(element)

        val w = element.measuredWidth.toInt().coerceAtMost(65500)
        val h = element.measuredHeight.toInt().coerceAtMost(65500)

        Surface.makeRasterN32Premul(w, h).use { surface ->
            element.draw(surface.canvas)
            surface.makeImageSnapshot()
        }
    }
}
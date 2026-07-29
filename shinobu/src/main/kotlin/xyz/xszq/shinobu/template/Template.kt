package xyz.xszq.shinobu.template

import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import xyz.xszq.shinobu.dom.Element
import xyz.xszq.shinobu.style.LayoutEngine

@Suppress("unused")
class Template(
    private val children: Map<String, Element>,
    val resourceManager: ResourceManager
) {
    operator fun get(id: String) = children[id] ?.clone()
    suspend fun render(
        element: Element
    ): Image {
        element.resolveResources(resourceManager)
        LayoutEngine.performLayout(element)
        element.prepareRenderTree()

        val w = element.measuredWidth.toInt().coerceAtMost(65500)
        val h = element.measuredHeight.toInt().coerceAtMost(65500)

        return Surface.makeRasterN32Premul(w, h).use { surface ->
            element.renderPicture?.let { surface.canvas.drawPicture(it) }
            surface.makeImageSnapshot()!!
        }
    }
}
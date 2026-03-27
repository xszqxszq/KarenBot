package xyz.xszq.bot.image.template

import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import xyz.xszq.bot.image.dom.Element
import xyz.xszq.bot.image.style.LayoutEngine

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

        val w = element.measuredWidth.toInt()
        val h = element.measuredHeight.toInt()

        val surface = Surface.makeRasterN32Premul(w, h)
        element.renderPicture?.let { surface.canvas.drawPicture(it) }

        return surface.makeImageSnapshot()
    }
}
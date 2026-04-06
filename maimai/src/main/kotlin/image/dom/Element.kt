package xyz.xszq.bot.image.dom

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Picture
import org.jetbrains.skia.PictureRecorder
import org.jetbrains.skia.Rect
import xyz.xszq.bot.image.style.Style
import xyz.xszq.bot.image.template.ResourceManager

sealed class Element(
    val id: String ?= null
) {
    var style = Style()

    val children: MutableList<Element> = mutableListOf()
    var parent: Element? = null

    var layoutX = 0f
    var layoutY = 0f
    var measuredWidth = 0f
    var measuredHeight = 0f

    var renderPicture: Picture ?= null

    val contentRect: Rect
        get() = Rect.makeLTRB(
            style.padding.left,
            style.padding.top,
            measuredWidth - style.padding.right,
            measuredHeight - style.padding.bottom
        )

    var background: String?
        get() = style.backgroundImage
        set(value) { style.backgroundImage = value }

    fun add(child: Element) {
        child.parent = this
        children.add(child)
    }

    operator fun get(path: String): Element? {
        val parts = path.split("/")
        var current: Element = this
        for (part in parts) {
            current = current.findById(part) ?: return null
        }
        return current
    }
    fun findById(
        targetId: String
    ): Element? {
        if (this.id == targetId)
            return this
        for (child in children) {
            val found = child.findById(targetId)
            if (found != null)
                return found
        }
        return null
    }

    inline fun modify(block: Element.() -> Unit) = this.apply(block)

    inline fun div(path: String, block: Div.() -> Unit) = (this[path] as Div?)?.apply(block)
    inline fun text(path: String, block: Span.() -> Unit) = (this[path] as Span?)?.apply(block)
    inline fun image(path: String, block: Img.() -> Unit) = (this[path] as Img?)?.apply(block)

    fun resolveResources(rm: ResourceManager) {
        if (this is Img) {
            if (!this.src.isNullOrEmpty())
                this.skiaImage = rm.getImage(this.src!!)
            if (!this.style.maskImage.isNullOrEmpty())
                this.maskSkiaImage = rm.getImage(this.style.maskImage!!)
        }

        if (this is Div) {
            if (!this.style.backgroundImage.isNullOrEmpty())
                this.bgSkiaImage = rm.getImage(this.style.backgroundImage!!)
            if (!this.style.maskImage.isNullOrEmpty())
                this.maskSkiaImage = rm.getImage(this.style.maskImage!!)
        }

        if (this is Span) {
            this.fontCollection = rm.fontCollection
        }

        this.children.forEach { it.resolveResources(rm) }
    }

    suspend fun prepareRenderTree(): Unit = coroutineScope {
        if (children.isNotEmpty()) {
            val deferredChildren = children.map { child ->
                async(Dispatchers.Default) {
                    child.prepareRenderTree()
                }
            }
            deferredChildren.awaitAll()
        }

        recordPicture()
    }

    fun recordPicture() {
        val recorder = PictureRecorder()
        val canvas = recorder.beginRecording(Rect.makeWH(measuredWidth, measuredHeight))
        draw(canvas)

        renderPicture = recorder.finishRecordingAsPicture()
    }

    protected abstract fun draw(canvas: Canvas)

    abstract fun clone(): Element

    protected fun copyBasePropertiesTo(target: Element) {
        target.style = this.style.deepCopy()

        this.children.forEach { child ->
            val clonedChild = child.clone()
            target.add(clonedChild)
        }
    }
}
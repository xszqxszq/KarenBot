package xyz.xszq.shinobu.dom

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Picture
import org.jetbrains.skia.PictureRecorder
import org.jetbrains.skia.Rect
import xyz.xszq.shinobu.style.Style
import xyz.xszq.shinobu.template.ResourceManager

/**
 * 渲染树的元素节点
 *
 * `Div`、`Span`、`Img` 等节点均继承自本类，共享子节点管理、
 * 布局测量与绘制录制等能力
 */
@Suppress("unused")
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

    /**
     * 添加子节点并回填其父节点引用
     */
    fun add(child: Element) {
        child.parent = this
        children.add(child)
    }

    /**
     * 按路径查找节点
     *
     * 路径由各级节点的 `id` 以斜杠连接组成，逐级向下查找
     *
     * @param path 以斜杠分隔的节点路径
     * @return 命中的节点
     */
    operator fun get(path: String): Element? {
        val parts = path.split("/")
        var current: Element = this
        for (part in parts) {
            current = current.findById(part) ?: return null
        }
        return current
    }

    /**
     * 在自身及后代节点中查找指定 `id` 的节点
     *
     * @param targetId 目标节点的 `id`
     * @return 命中的节点
     */
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

    /**
     * 以 DSL 方式修改路径指向的 `Div` 节点
     *
     * 路径未命中或节点类型不符时不生效
     *
     * @param path 以斜杠分隔的节点路径
     */
    inline fun div(path: String, block: Div.() -> Unit) = (this[path] as Div?)?.apply(block)

    /**
     * 以 DSL 方式修改路径指向的 `Span` 节点
     *
     * 路径未命中或节点类型不符时不生效
     *
     * @param path 以斜杠分隔的节点路径
     */
    inline fun text(path: String, block: Span.() -> Unit) = (this[path] as Span?)?.apply(block)

    /**
     * 以 DSL 方式修改路径指向的 `Img` 节点
     *
     * 路径未命中或节点类型不符时不生效
     *
     * @param path 以斜杠分隔的节点路径
     */
    inline fun image(path: String, block: Img.() -> Unit) = (this[path] as Img?)?.apply(block)

    /**
     * 解析节点引用的图片与字体资源
     *
     * 按样式或标签声明的路径从资源管理器取图，文本节点挂接字体集合，
     * 并递归处理全部子节点
     *
     * @param rm 资源管理器
     */
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

    /**
     * 自底向上录制整棵子树的绘制内容
     *
     * 先递归处理子节点，再录制自身，供渲染时直接回放
     */
    fun prepareRenderTree() {
        children.forEach { child ->
            child.prepareRenderTree()
        }
        recordPicture()
    }

    /**
     * 把当前节点的绘制内容录制为 `Picture` 并保存
     *
     * 重复录制前会先关闭上一次保存的 `Picture`
     */
    fun recordPicture() {
        renderPicture?.close()
        PictureRecorder().use { recorder ->
            val canvas = recorder.beginRecording(Rect.makeWH(measuredWidth, measuredHeight))
            draw(canvas)
            renderPicture = recorder.finishRecordingAsPicture()
        }
    }

    /**
     * 把节点内容绘制到画布
     *
     * @param canvas 目标画布
     */
    abstract fun draw(canvas: Canvas)

    /**
     * 深拷贝自身及子树
     *
     * 样式与全部子节点一并复制，副本与原树互不影响
     *
     * @return 拷贝出的节点
     */
    abstract fun clone(): Element

    protected fun copyBasePropertiesTo(target: Element) {
        target.style = this.style.deepCopy()

        this.children.forEach { child ->
            val clonedChild = child.clone()
            target.add(clonedChild)
        }
    }
}
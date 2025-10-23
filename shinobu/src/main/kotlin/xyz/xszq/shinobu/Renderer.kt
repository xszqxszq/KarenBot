package xyz.xszq.shinobu

import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.NativeImage
import korlibs.image.bitmap.context2d
import korlibs.image.bitmap.resized
import korlibs.image.color.Colors
import korlibs.image.font.Font
import korlibs.image.font.FontRegistry
import korlibs.image.font.SystemFontRegistry
import korlibs.math.geom.Anchor
import korlibs.math.geom.Point
import korlibs.math.geom.ScaleMode
import korlibs.math.geom.vector.LineJoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.math.max
import kotlin.math.min

/**
 * Renderer for Image.
 */
class Renderer(
    val defaultFont: String = "Simsun"
) {
    var fontRegistry: FontRegistry
    private fun prepare() {
        NativeImage(
            width = 100,
            height = 100
        ).context2d {
            fillStyle = Colors.BLACK
            drawText(
                text = "Example Text",
                pos = Point(0, 0),
                font = this@Renderer.fontRegistry[this@Renderer.defaultFont],
                size = 14.0
            )
        }
    }
    init {
        runBlocking {
            fontRegistry = SystemFontRegistry()
            prepare()
        }
    }
    private fun renderText(element: Text, fonts: Map<String, Font>, x: Double, y: Double): Bitmap {
        val text = element.text
        val font = element.bitmapFont ?: fonts[element.font]!!
        val glyphs = font.measureTextGlyphs(element.size, text)
        val fmetrics = glyphs.fmetrics

        var width = glyphs.glyphs.lastOrNull()?.let {
            it.pos[0] + it.metrics.xadvance
        }?.toInt() ?: 0
        val height = (fmetrics.ascent - fmetrics.descent).toInt()
        var xOffset = - (glyphs.glyphs.firstOrNull()?.metrics?.bounds?.x ?: 0.0)
        if (element.border?.size?.let { it > 10 } ?: false) {
            width += (element.border.size * 2).toInt()
            xOffset += element.border.size
        }
        val yOffset = fmetrics.ascent
        val pos = Point(x + xOffset, y + yOffset)
        element.descent = fmetrics.descent
        return NativeImage(
            width = width,
            height = height + (element.shadow ?.size ?.toInt() ?: 0),
        ).context2d {
            this.font = font
            this.fontSize = element.size.toDouble()
            this.lineJoin = LineJoin.ROUND
            element.shadow ?.let { shadow ->
                val shadowColor = shadow.color.hexToRGBA(shadow.opacity)
                element.border ?.let { border ->
                    this.lineWidth = border.size
                    this.strokeStyle = shadowColor
                    strokeText(text, Point(pos.x, pos.y + shadow.size))
                }
                this.fillStyle = shadowColor
                fillText(text, Point(pos.x, pos.y + shadow.size))
            }
            element.border ?.let { border ->
                this.lineWidth = border.size
                this.strokeStyle = border.color.hexToRGBA()
                strokeText(text, pos)
            }
            this.fillStyle = element.color.hexToRGBA()
            fillText(text, pos)
        }
    }
    private fun renderImage(element: Image, x: Double, y: Double): Bitmap? {
        var image = element.image ?: return null
        if (element.stretch && element.width != null && element.height != null) {
            image = image.resized(element.width, element.height, ScaleMode.COVER, Anchor.CENTER)
        }
        element.maskImage ?.let { mask ->
            image = image.mask(mask)
        }
        if (element.opacity != 1.0) {
            image = image.transparent(element.opacity)
        }
        if (element.padding.isZero())
            return image
        val width = (element.width ?: image.width) + element.padding.lengthX()
        val height = (element.height ?: image.height) + element.padding.lengthY()
        return NativeImage(
            width = width,
            height = height
        ).context2d {
            drawImage(
                image = image,
                pos = Point(x, y)
            )
        }
    }
    private fun fixedSize(container: Container): Pair<Int?, Int?> {
        val fixedWidth = container.width ?: container.parent ?.let { parent ->
            parent.width?.minus(parent.padding.lengthX())
        }
        val fixedHeight = container.height ?: container.parent ?.let { parent ->
            parent.height?.minus(parent.padding.lengthY())
        }
        return when (container.direction) {
            Direction.Row -> Pair(fixedWidth, container.height)
            Direction.Column -> Pair(container.width, fixedHeight)
        }
    }
    private fun placeElements(
        container: Container,
        elements: Elements,
        fixedWidth: Int?,
        fixedHeight: Int?
    ): Triple<PackedElements, Int, Int> {
        val actualXAvailable = fixedWidth ?.let {
            fixedWidth - container.padding.lengthX()
        }
        val actualYAvailable = fixedHeight ?.let {
            fixedHeight - container.padding.lengthY()
        }
        val placedElements = mutableListOf<MutableList<RenderedElement>>(mutableListOf())

        var line = 0
        var place = 0
        var length = 0
        var placedWidth = 0
        var placedHeight = 0

        elements.forEach { (child, rendered) ->
            when (container.direction) {
                Direction.Row -> {
                    actualXAvailable ?.let {
                        if (place + rendered.width + child.margin.lengthX() > actualXAvailable) {
                            line ++
                            placedElements.add(mutableListOf())
                            placedWidth = maxOf(placedWidth, place)
                            placedHeight += length

                            place = 0
                            length = 0
                        }
                    }
                    placedElements[line].add(Pair(child, rendered))
                    place += rendered.width + child.margin.lengthX()
                    length = max(length, child.margin.lengthY() + rendered.height)
                }
                Direction.Column -> {
                    actualYAvailable ?.let {
                        if (place + rendered.height + child.margin.lengthY() > actualYAvailable) {
                            line ++
                            placedElements.add(mutableListOf())
                            placedHeight = maxOf(placedHeight, place)
                            placedWidth += length

                            place = 0
                            length = 0
                        }
                    }
                    placedElements[line].add(Pair(child, rendered))
                    place += rendered.height + child.margin.lengthY()
                    length = max(length, child.margin.lengthX() + rendered.width)
                }
            }
        }
        when (container.direction) {
            Direction.Row -> {
                placedWidth = maxOf(placedWidth, place)
                placedHeight += length
            }
            Direction.Column -> {
                placedHeight = maxOf(placedHeight, place)
                placedWidth += length
            }
        }
        placedWidth += container.padding.lengthX()
        placedHeight += container.padding.lengthY()

        return Triple(placedElements, fixedWidth ?: placedWidth, fixedHeight ?: placedHeight)
    }
    private fun placedSize(
        container: Container,
        placedElements: PackedElements,
        fixedWidth: Int?,
        fixedHeight: Int?
    ): Pair<Int, Int> {
        if (placedElements.size == 1 && placedElements[0].isEmpty())
            return Pair(0, 0)
        val placedWidth = fixedWidth ?: (container.padding.lengthX() + run {
            if (container.direction == Direction.Row) {
                placedElements.maxOf { line -> line.sumOf { (child, rendered) ->
                    child.margin.lengthX() + rendered.width
                }}
            } else {
                placedElements.sumOf { line -> line.maxOf { (child, rendered) ->
                    child.margin.lengthX() + rendered.width
                }}
            }
        })
        val placedHeight = fixedHeight ?: (container.padding.lengthY() + run {
            if (container.direction == Direction.Column) {
                placedElements.maxOf { line -> line.sumOf { (child, rendered) ->
                    child.margin.lengthY() + rendered.height
                }}
            } else {
                placedElements.sumOf { line -> line.maxOf { (child, rendered) ->
                    child.margin.lengthY() + rendered.height
                }}
            }
        })
        return Pair(placedWidth, placedHeight)
    }
    private fun actualSize(container: Container, line: Elements): Pair<Int, Int> {
        if (line.isEmpty())
            return Pair(0, 0)
        val actualXLength =
            if (container.direction == Direction.Row)
                line.sumOf { (child, rendered) ->
                    rendered.width + child.margin.lengthX()
                }
            else
                line.maxOf { (child, rendered) ->
                    rendered.width + child.margin.lengthX()
                }
        val actualYLength =
            if (container.direction == Direction.Column)
                line.sumOf { (child, rendered) ->
                    rendered.height + child.margin.lengthY()
                }
            else
                line.maxOf { (child, rendered) ->
                    rendered.height + child.margin.lengthY()
                }
        return Pair(actualXLength, actualYLength)
    }
    private fun computeElements(
        container: Container,
        placedElements: PackedElements,
        initX: Double,
        initY: Double,
        placedWidth: Int,
        placedHeight: Int
    ): List<Pair<Bitmap, Point>> {
        var x = initX
        var y = initY

        val startX = x
        val startY = y

        var lastRight = x
        var lastBottom = y
        val computedElements = mutableListOf<Pair<Bitmap, Point>>()
        placedElements.forEach { line ->
            val (actualXLength, actualYLength) = actualSize(container, line)
            var spacing = 0.0
            when (container.justifyContent) {
                Alignment.Center -> {
                    when (container.direction) {
                        Direction.Row -> x = (placedWidth - actualXLength) / 2.0
                        Direction.Column -> y = (placedHeight - actualYLength) / 2.0
                    }
                }
                Alignment.End -> {
                    when (container.direction) {
                        Direction.Row -> x = (placedWidth - actualXLength - container.padding.right).toDouble()
                        Direction.Column -> y = (placedHeight - actualYLength - container.padding.bottom).toDouble()
                    }
                }
                Alignment.SpaceBetween -> {
                    spacing = when {
                        line.size == 1 -> 0.0
                        container.direction == Direction.Row -> {
                            (placedWidth - actualXLength
                                    - container.padding.lengthX()).toDouble() / (line.size - 1)
                        }
                        container.direction == Direction.Column -> {
                            (placedHeight - actualYLength
                                    - container.padding.lengthY()).toDouble() / (line.size - 1)
                        }
                        else -> 0.0
                    }
                }
                Alignment.SpaceAround -> {
                    when (container.direction) {
                        Direction.Row -> {
                            spacing =
                                (placedWidth - actualXLength - container.padding.lengthX()).toDouble() / line.size
                            x += spacing / 2
                        }
                        Direction.Column -> {
                            spacing =
                                (placedHeight - actualYLength - container.padding.lengthY()).toDouble() / line.size
                            y += spacing / 2
                        }
                    }
                }
                Alignment.Start -> {}
            }
            line.forEach { (child, rendered) ->
                var nowX = x + child.margin.left
                var nowY = y + child.margin.top
                
                when (container.alignContent) {
                    Alignment.Start -> {
                        when (container.direction) {
                            Direction.Row -> nowY = y + child.margin.top
                            Direction.Column -> nowX = x + child.margin.left
                        }
                    }
                    Alignment.End -> {
                        when (container.direction) {
                            Direction.Row -> nowY = y + actualYLength - rendered.height - child.margin.bottom - if (child is Text) child.descent else 0.0
                            Direction.Column -> nowX = x + actualXLength - rendered.width - child.margin.right
                        }
                    }
                    Alignment.Center -> {
                        when (container.direction) {
                            Direction.Row -> nowY = y + (placedHeight - rendered.height) / 2 + child.margin.top - child.margin.bottom
                            Direction.Column -> nowX = x + (placedWidth - rendered.width) / 2 + child.margin.left - child.margin.right
                        }
                    }
                    else -> {
                    }
                }
                computedElements.add(Pair(rendered, Point(nowX, nowY)))

                lastBottom = max(lastBottom, y + rendered.height + child.margin.lengthY())
                lastRight = max(lastRight, x + rendered.width + child.margin.lengthX())
                when (container.direction) {
                    Direction.Row -> {
                        x += rendered.width + child.margin.lengthX() + spacing
                    }
                    Direction.Column -> {
                        y += rendered.height + child.margin.lengthY() + spacing
                    }
                }
            }

            when (container.direction) {
                Direction.Row -> {
                    x = startX
                    y = lastBottom
                }
                Direction.Column -> {
                    y = startY
                    x = lastRight
                }
            }
            lastRight = x
            lastBottom = y
        }
        return computedElements
    }
    suspend fun <T, R> List<T>.mapParallel(
        parallel: Boolean = false,
        block: suspend (T) -> R
    ): List<R> {
        if (!parallel)
            return map { runBlocking { block(it) } }
        return coroutineScope {
            map {
                async(Dispatchers.IO) {
                    block(it)
                }
            }.awaitAll()
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun renderElement(
        element: Element,
        fonts: Map<String, Font>,
        parallel: Boolean = false
    ): Bitmap? {
        val x = element.padding.left.toDouble()
        val y = element.padding.top.toDouble()
        return when (element) {
            is Text -> renderText(element, fonts, x, y)
            is Image -> renderImage(element, x, y)
            is Container -> {
                val isParallel = parallel || element.parallel
                val elements = element.children.mapParallel(isParallel) { child ->
                    if (child is Container)
                        child.parent = element
                    renderElement(child, fonts) ?.let { rendered ->
                        Pair(child, rendered)
                    }
                }.filterNotNull()

                val (fixedWidth, fixedHeight) = fixedSize(element)
                val (placedElements, placedWidth, placedHeight) = if (element.wrap == Wrap.Wrap) {
                    placeElements(element, elements, fixedWidth, fixedHeight)
                } else {
                    val nowElements = mutableListOf(elements)
                    val (nowWidth, nowHeight) = placedSize(element, nowElements, fixedWidth, fixedHeight)
                    Triple(nowElements, nowWidth, nowHeight)
                }

                var realWidth = element.minWidth ?.let {
                    max(it, placedWidth)
                } ?: placedWidth
                var realHeight = element.minHeight ?.let {
                    max(it, placedHeight)
                } ?: placedHeight
                element.maxWidth ?.let {
                    realWidth = min(it, realWidth)
                }
                element.maxHeight ?.let {
                    realHeight = min(it, realHeight)
                }
                val computedElements = computeElements(
                    element, placedElements, x, y, realWidth, realHeight
                )

                NativeImage(
                    width = realWidth,
                    height = realHeight
                ).context2d {
                    element.backgroundImage ?.let { background ->
                        val target = if (element.stretch)
                            background.resized(realWidth, realHeight, ScaleMode.COVER, Anchor.CENTER)
                        else
                            background
                        drawImage(target, Point(0, 0))
                    }
                    element.color ?.let { colorHex ->
                        fillStyle = colorHex.hexToRGBA(element.colorOpacity)
                        fillRect(0, 0, realWidth, realHeight)
                    }
                    computedElements.forEach { (rendered, point) ->
                        drawImage(rendered, point)
                    }
                }
            }
        }
    }
    fun getFont(name: String): Font {
        return fontRegistry[name]
    }
    fun loadFonts(container: Container): Map<String, Font> {
        return container.collectFonts().associateWith { name ->
            getFont(name)
        }.toMutableMap().also {
            it.put("", getFont(defaultFont))
        }
    }
}
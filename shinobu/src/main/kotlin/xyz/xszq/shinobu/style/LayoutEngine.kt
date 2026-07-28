package xyz.xszq.shinobu.style

import org.jetbrains.skia.FontSlant
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.FontWidth
import org.jetbrains.skia.paragraph.*
import xyz.xszq.shinobu.dom.Div
import xyz.xszq.shinobu.dom.Element
import xyz.xszq.shinobu.dom.Img
import xyz.xszq.shinobu.dom.Span

object LayoutEngine {
    private class FlexLine(
        val items: MutableList<Element> = mutableListOf(),
        var mainSize: Float = 0f,
        var crossSize: Float = 0f
    )

    private val Element.outerWidth: Float get() = measuredWidth + style.margin.left + style.margin.right
    private val Element.outerHeight: Float get() = measuredHeight + style.margin.top + style.margin.bottom

    private fun Element.mainSize(isRow: Boolean) = if (isRow) outerWidth else outerHeight
    private fun Element.crossSize(isRow: Boolean) = if (isRow) outerHeight else outerWidth
    private fun Element.mainMargin(isRow: Boolean) = if (isRow) style.margin.left else style.margin.top
    private fun Element.crossMargin(isRow: Boolean) = if (isRow) style.margin.top else style.margin.left

    fun performLayout(root: Element) {
        measure(root)
        layout(root, 0f, 0f)
    }

    private fun measure(
        element: Element,
        parentAvailableW: Float = Float.POSITIVE_INFINITY,
        parentAvailableH: Float = Float.POSITIVE_INFINITY
    ) {
        var availableContentW = parentAvailableW
        var availableContentH = parentAvailableH

        element.style.width ?.let {
            availableContentW = maxOf(0f, element.style.width!! - element.style.padding.left - element.style.padding.right)
        } ?: run {
            if (availableContentW != Float.POSITIVE_INFINITY)
                availableContentW = maxOf(0f, availableContentW - element.style.padding.left - element.style.padding.right)
        }
        element.style.height ?.let {
            availableContentH = it - element.style.padding.top - element.style.padding.bottom
        } ?: run {
            if (availableContentH != Float.POSITIVE_INFINITY) {
                availableContentH = maxOf(0f, availableContentH - element.style.padding.top - element.style.padding.bottom)
            }
        }
        element.style.maxWidth?.let {
            availableContentW = minOf(
                availableContentW, it - element.style.padding.left - element.style.padding.right
            )
        }
        element.style.maxHeight?.let {
            availableContentH = minOf(
                availableContentH, it - element.style.padding.top - element.style.padding.bottom
            )
        }

        element.children.forEach { child ->
            measure(child, availableContentW)
        }

        var intrinsicContentW = 0f
        var intrinsicContentH = 0f

        when (element) {
            is Span -> {
                if (element.text.isNotEmpty() && element.fontCollection != null) {
                    @Suppress("UNUSED_VALUE")
                    fun buildPara(fontSize: Float): Paragraph {
                        val textStyle = TextStyle().apply {
                            this.fontSize = fontSize
                            element.style.fontFamilies ?.let {
                                fontFamilies = it.toTypedArray()
                            }

                            fontStyle = FontStyle(
                                element.style.fontWeight,
                                FontWidth.NORMAL,
                                FontSlant.UPRIGHT
                            )
                        }

                        val paragraphStyle = ParagraphStyle().apply {
                            if (element.style.whiteSpace == WhiteSpace.NOWRAP)
                                maxLinesCount = 1
                            alignment = when (element.style.textAlign) {
                                TextAlign.CENTER -> Alignment.CENTER
                                TextAlign.RIGHT -> Alignment.RIGHT
                                else -> Alignment.LEFT
                            }
                        }

                        return ParagraphBuilder(paragraphStyle, element.fontCollection!!).use { builder ->
                            builder.pushStyle(textStyle)
                            builder.addText(element.text)
                            builder.build()
                        }.also {
                            textStyle.close()
                            paragraphStyle.close()
                        }
                    }

                    var currentSize = element.style.textSize
                    var paragraph = buildPara(currentSize)

                    val layoutWidth = if (element.style.whiteSpace == WhiteSpace.NORMAL && availableContentW < Float.POSITIVE_INFINITY) {
                        maxOf(0f, availableContentW)
                    } else {
                        Float.POSITIVE_INFINITY
                    }

                    paragraph.layout(layoutWidth)

                    if (element.style.minTextSize != null && paragraph.maxIntrinsicWidth > availableContentW) {
                        val minSize = element.style.minTextSize!!

                        while (paragraph.maxIntrinsicWidth > availableContentW && currentSize > minSize) {
                            currentSize -= 1f
                            paragraph.close()
                            paragraph = buildPara(currentSize)
                            paragraph.layout(Float.POSITIVE_INFINITY)
                        }

                        element.computedFontSize = currentSize
                        paragraph.layout(layoutWidth)
                    }

                    val linesCount = paragraph.lineMetrics.size.coerceAtLeast(1)
                    intrinsicContentW = minOf(paragraph.maxIntrinsicWidth, layoutWidth)
                    intrinsicContentH = minOf(paragraph.height, currentSize * 1.4f * linesCount)
                    paragraph.close()
                }
            }
            is Img -> {
                val imgW = element.skiaImage?.width?.toFloat() ?: 0f
                val imgH = element.skiaImage?.height?.toFloat() ?: 0f
                val ratio = if (imgW > 0) imgH / imgW else 1f

                val hasW = element.style.width != null
                val hasH = element.style.height != null

                when {
                    hasW && !hasH -> {
                        intrinsicContentW = element.style.width!!
                        intrinsicContentH = intrinsicContentW * ratio
                    }
                    !hasW && hasH -> {
                        intrinsicContentH = element.style.height!!
                        intrinsicContentW = if (ratio > 0) intrinsicContentH / ratio else 0f
                    }
                    else -> {
                        intrinsicContentW = imgW
                        intrinsicContentH = imgH
                    }
                }
            }
            is Div -> {
                val isRow = element.style.flexDirection == FlexDirection.ROW
                val availableMain = if (isRow) availableContentW else availableContentH

                var currentMain = 0f
                var currentCross = 0f
                var maxMain = 0f
                var totalCross = 0f

                for (child in element.children) {
                    val childMain = child.mainSize(isRow)
                    val childCross = child.crossSize(isRow)

                    if (element.style.flexWrap == FlexWrap.WRAP && currentMain + childMain > availableMain && currentMain > 0) {
                        maxMain = maxOf(maxMain, currentMain)
                        totalCross += currentCross

                        currentMain = childMain
                        currentCross = childCross
                    } else {
                        currentMain += childMain
                        currentCross = maxOf(currentCross, childCross)
                    }
                }
                maxMain = maxOf(maxMain, currentMain)
                totalCross += currentCross

                if (isRow) {
                    intrinsicContentW = maxMain
                    intrinsicContentH = totalCross
                } else {
                    intrinsicContentW = totalCross
                    intrinsicContentH = maxMain
                }
            }
        }

        var finalW = element.style.width ?: (intrinsicContentW + element.style.padding.left + element.style.padding.right)
        var finalH = element.style.height ?: (intrinsicContentH + element.style.padding.top + element.style.padding.bottom)

        element.style.minWidth?.let { finalW = maxOf(finalW, it) }
        element.style.maxWidth?.let { finalW = minOf(finalW, it) }
        element.style.minHeight?.let { finalH = maxOf(finalH, it) }
        element.style.maxHeight?.let { finalH = minOf(finalH, it) }

        element.measuredWidth = finalW
        element.measuredHeight = finalH
    }

    private fun measureChildSize(
        isRow: Boolean,
        child: Element,
        line: FlexLine
    ) {
        if (isRow && child.style.height == null) {
            child.measuredHeight = maxOf(0f, line.crossSize - child.style.margin.top - child.style.margin.bottom)
        } else if (!isRow && child.style.width == null) {
            child.measuredWidth = maxOf(0f, line.crossSize - child.style.margin.left - child.style.margin.right)
        }
    }

    private fun layout(element: Element, startX: Float, startY: Float) {
        element.layoutX = startX
        element.layoutY = startY

        if (element.children.isEmpty())
            return

        val isRow = element.style.flexDirection == FlexDirection.ROW
        val contentW = element.measuredWidth - element.style.padding.left - element.style.padding.right
        val contentH = element.measuredHeight - element.style.padding.top - element.style.padding.bottom
        val mainSpace = if (isRow) contentW else contentH

        val crossSpace = if (isRow) contentH else contentW

        val lines = mutableListOf<FlexLine>()
        var currentLine = FlexLine()

        for (child in element.children) {
            val childMain = child.mainSize(isRow)
            val childCross = child.crossSize(isRow)

            if (element.style.flexWrap == FlexWrap.WRAP && currentLine.mainSize + childMain > mainSpace && currentLine.items.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = FlexLine(mutableListOf(child), childMain, childCross)
            } else {
                currentLine.items.add(child)
                currentLine.mainSize += childMain
                currentLine.crossSize = maxOf(currentLine.crossSize, childCross)
            }
        }
        if (currentLine.items.isNotEmpty()) {
            lines.add(currentLine)
        }

        if (element.style.flexWrap == FlexWrap.NOWRAP && lines.size == 1) {
            lines[0].crossSize = maxOf(lines[0].crossSize, crossSpace)
        }

        var currentCrossPos = 0f

        for (line in lines) {
            if (element.style.alignItems == AlignItems.STRETCH) {
                for (child in line.items) {
                    if (child is Img && child.skiaImage != null) {
                        if (isRow && child.style.height == null) {
                            val newCross = maxOf(0f, line.crossSize - child.style.margin.top - child.style.margin.bottom)
                            child.measuredHeight = newCross
                            val ratio = child.skiaImage!!.width.toFloat() / child.skiaImage!!.height.toFloat()
                            child.measuredWidth = newCross * ratio
                        } else if (!isRow && child.style.width == null) {
                            val newCross = maxOf(0f, line.crossSize - child.style.margin.left - child.style.margin.right)
                            child.measuredWidth = newCross
                            val ratio = child.skiaImage!!.width.toFloat() / child.skiaImage!!.height.toFloat()
                            child.measuredHeight = newCross / ratio
                        }
                    } else if (child is Div) {
                        measureChildSize(isRow, child, line)
                    }
                }
                line.mainSize = line.items.sumOf { it.mainSize(isRow).toDouble() }.toFloat()
            }

            val freeMainSpace = mainSpace - line.mainSize
            var currentMainPos = 0f
            var itemSpaceBetween = 0f

            when (element.style.justifyContent) {
                JustifyContent.FLEX_START -> currentMainPos = 0f
                JustifyContent.FLEX_END -> currentMainPos = freeMainSpace
                JustifyContent.CENTER -> currentMainPos = freeMainSpace / 2f
                JustifyContent.SPACE_BETWEEN -> {
                    if (freeMainSpace >= 0 && line.items.size > 1)
                        itemSpaceBetween = freeMainSpace / (line.items.size - 1)
                }
                JustifyContent.SPACE_AROUND -> {
                    if (freeMainSpace >= 0 && line.items.isNotEmpty()) {
                        itemSpaceBetween = freeMainSpace / line.items.size
                        currentMainPos = itemSpaceBetween / 2f
                    } else if (freeMainSpace < 0) {
                        currentMainPos = freeMainSpace / 2f
                    }
                }
            }

            for (child in line.items) {
                val childMainAdvance = child.mainSize(isRow)
                val mainOffset = currentMainPos + child.mainMargin(isRow)

                val childCrossSize = child.crossSize(isRow)
                val freeLineCrossSpace = line.crossSize - childCrossSize

                var crossOffset: Float
                when (element.style.alignItems) {
                    AlignItems.FLEX_START -> crossOffset = 0f
                    AlignItems.FLEX_END -> crossOffset = freeLineCrossSpace
                    AlignItems.CENTER -> crossOffset = freeLineCrossSpace / 2f
                    AlignItems.STRETCH -> {
                        crossOffset = 0f
                        if (child !is Span) {
                            measureChildSize(isRow, child, line)
                        }
                    }
                }
                crossOffset += child.crossMargin(isRow)

                val childX = element.style.padding.left + if (isRow) mainOffset else (currentCrossPos + crossOffset)
                val childY = element.style.padding.top + if (isRow) (currentCrossPos + crossOffset) else mainOffset

                layout(child, childX, childY)

                currentMainPos += childMainAdvance + itemSpaceBetween
            }

            currentCrossPos += line.crossSize
        }
    }
}
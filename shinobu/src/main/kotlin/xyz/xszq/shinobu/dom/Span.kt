package xyz.xszq.shinobu.dom

import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.*
import xyz.xszq.shinobu.style.TextAlign
import xyz.xszq.shinobu.style.WhiteSpace

/**
 * 文本节点
 *
 * 借助 Skia 段落排版绘制文本，支持对齐、描边、阴影与不换行等样式，
 * 布局时若设置了最小字号会缩小字号以适配可用宽度
 */
@Suppress("unused")
class Span(
    id: String ?= null,
    var text: String = ""
) : Element(id) {
    var fontCollection: FontCollection ?= null
    var computedFontSize: Float ?= null
    internal var measuredParagraph: Paragraph? = null
    override fun draw(canvas: Canvas) {
        if (text.isEmpty() || fontCollection == null)
            return

        val measured = measuredParagraph
        if (measured != null && style.textStroke == null && style.textShadow == null) {
            canvas.save()
            canvas.clipRect(contentRect)
            measured.paint(canvas, contentRect.left, contentRect.top + layerYOffset(measured))
            canvas.restore()
            return
        }

        val paragraphStyle = ParagraphStyle().apply {
            if (style.whiteSpace == WhiteSpace.NOWRAP)
                maxLinesCount = 1

            alignment = when (style.textAlign) {
                TextAlign.CENTER -> Alignment.CENTER
                TextAlign.RIGHT -> Alignment.RIGHT
                else -> Alignment.LEFT
            }
        }

        fun paintTextLayer(styleConfig: TextStyle.() -> Unit) {
            val textStyle = TextStyle().apply {
                fontSize = computedFontSize ?: style.textSize
                style.fontFamilies ?.let {
                    fontFamilies = it.toTypedArray()
                }
                fontStyle = FontStyle(style.fontWeight, FontWidth.NORMAL, FontSlant.UPRIGHT)

                styleConfig()
            }

            ParagraphBuilder(paragraphStyle, fontCollection!!).use { builder ->
                builder.pushStyle(textStyle)
                builder.addText(text)
                builder.build().use { paragraph ->
                    paragraph.layout(layoutWidth())
                    paragraph.paint(
                        canvas,
                        contentRect.left,
                        contentRect.top + layerYOffset(paragraph)
                    )
                }
            }
        }

        canvas.save()

        var clipPadding = 4f
        style.textStroke ?.let { stroke ->
            clipPadding += stroke.size
        }
        style.textShadow ?.let { shadow ->
            clipPadding += maxOf(kotlin.math.abs(shadow.dx), kotlin.math.abs(shadow.dy))
        }
        val safeRect = Rect.makeLTRB(
            contentRect.left - clipPadding,
            contentRect.top - clipPadding,
            contentRect.right + clipPadding,
            contentRect.bottom + clipPadding
        )
        canvas.clipRect(safeRect)

        val stroke = style.textStroke
        // 多行复用会有Bug
        val singleLine = (measured ?.lineNumber ?: 0) <= 1
        val reuse = if (stroke != null && singleLine) measured else null
        val layerWidth = layoutWidth()
        reuse ?.layout(layerWidth)
        val layerX = contentRect.left
        val layerY = contentRect.top + (reuse ?.let { layerYOffset(it) } ?: 0f)

        fun strokeLayer(configure: Paint.() -> Unit) {
            val paint = Paint().apply(configure)
            if (reuse != null)
                repaintLayer(canvas, reuse, paint, layerWidth, layerX, layerY)
            else
                paintTextLayer { foreground = paint }
            paint.close()
        }

        if (stroke != null) {
            style.textShadow ?.let { shadow ->
                canvas.save()
                canvas.translate(shadow.dx, shadow.dy)
                strokeLayer {
                    mode = PaintMode.STROKE_AND_FILL
                    strokeWidth = stroke.size
                    color = shadow.color
                    strokeJoin = PaintStrokeJoin.ROUND
                    strokeCap = PaintStrokeCap.ROUND
                }
                canvas.restore()
            }

            strokeLayer {
                mode = PaintMode.STROKE
                strokeWidth = stroke.size
                color = stroke.color
                strokeJoin = PaintStrokeJoin.ROUND
                strokeCap = PaintStrokeCap.ROUND
            }

            strokeLayer {
                mode = PaintMode.FILL
                color = style.textColor
            }
        } else {
            paintTextLayer {
                color = style.textColor
                style.textShadow?.let { shadow ->
                    addShadow(Shadow(shadow.color, shadow.dx, shadow.dy, 0.0))
                }
            }
        }

        canvas.restore()
    }

    private fun layoutWidth(): Float =
        if (style.whiteSpace == WhiteSpace.NOWRAP)
            Float.POSITIVE_INFINITY
        else contentRect.width

    private fun layerYOffset(paragraph: Paragraph): Float {
        val lines = paragraph.lineMetrics
        val metrics = lines.firstOrNull() ?: return 0f
        if (lines.size > 1)
            return (contentRect.height - paragraph.height) / 2f
        return (contentRect.height / 2f) -
            (metrics.baseline.toFloat() +
                (metrics.descent.toFloat() - metrics.ascent.toFloat()) / 2f)
    }

    private fun repaintLayer(
        canvas: Canvas,
        paragraph: Paragraph,
        paint: Paint,
        width: Float,
        x: Float,
        y: Float
    ) {
        paragraph.updateForegroundPaint(0, text.length, paint)
        paragraph.layout(width)
        paragraph.paint(canvas, x, y)
    }

    override fun clone(): Element {
        return Span(this.id, this.text).also { copyBasePropertiesTo(it) }
    }
}
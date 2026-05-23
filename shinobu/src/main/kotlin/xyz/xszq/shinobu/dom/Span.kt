package xyz.xszq.shinobu.dom

import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.*
import xyz.xszq.shinobu.style.TextAlign
import xyz.xszq.shinobu.style.WhiteSpace

@Suppress("unused")
class Span(
    id: String ?= null,
    var text: String = ""
) : Element(id) {
    var fontCollection: FontCollection ?= null
    var computedFontSize: Float ?= null
    override fun draw(canvas: Canvas) {
        if (text.isEmpty() || fontCollection == null)
            return

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
            val runs = FontResolver.resolve(
                text = text,
                fontFamilies = style.fontFamilies ?: emptyList(),
                fontWeight = style.fontWeight,
                fontCollection = fontCollection!!
            )

            val builder = ParagraphBuilder(paragraphStyle, fontCollection!!)
            for (run in runs) {
                val runStyle = TextStyle().apply {
                    fontSize = computedFontSize ?: style.textSize
                    if (run.typeface != null) {
                        setTypeface(run.typeface)
                    } else if (run.fontFamily != null) {
                        fontFamilies = arrayOf(run.fontFamily)
                    } else {
                        style.fontFamilies?.let { fontFamilies = it.toTypedArray() }
                    }
                    styleConfig()
                }
                builder.pushStyle(runStyle)
                builder.addText(run.text)
                builder.popStyle()
            }

            val paragraph = builder.build()

            val layoutW = if (style.whiteSpace == WhiteSpace.NOWRAP) {
                Float.POSITIVE_INFINITY
            } else {
                contentRect.width
            }
            paragraph.layout(layoutW)

            val metrics = paragraph.lineMetrics.firstOrNull()

            val yOffset = if (metrics != null) {
                val linesCount = paragraph.lineMetrics.size

                if (linesCount > 1) {
                    (contentRect.height - paragraph.height) / 2f
                } else {
                    val ascent = metrics.ascent.toFloat()
                    val descent = metrics.descent.toFloat()
                    val baseline = metrics.baseline.toFloat()

                    val inkCenterY = baseline + (descent - ascent) / 2f
                    val boxCenterY = contentRect.height / 2f

                    boxCenterY - inkCenterY
                }
            } else {
                0f
            }

            paragraph.paint(canvas, contentRect.left, contentRect.top + yOffset)
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

        if (style.textStroke != null) {
            val stroke = style.textStroke!!

            style.textShadow?.let { shadow ->
                canvas.save()
                canvas.translate(shadow.dx, shadow.dy)
                paintTextLayer {
                    foreground = Paint().apply {
                        mode = PaintMode.STROKE_AND_FILL
                        strokeWidth = stroke.size
                        color = shadow.color
                        strokeJoin = PaintStrokeJoin.ROUND
                        strokeCap = PaintStrokeCap.ROUND
                    }
                }
                canvas.restore()
            }

            paintTextLayer {
                foreground = Paint().apply {
                    mode = PaintMode.STROKE
                    strokeWidth = stroke.size
                    color = stroke.color
                    strokeJoin = PaintStrokeJoin.ROUND
                    strokeCap = PaintStrokeCap.ROUND
                }
            }

            paintTextLayer {
                foreground = Paint().apply {
                    mode = PaintMode.FILL
                    color = style.textColor
                }
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

    override fun clone(): Element {
        return Span(this.id, this.text).also { copyBasePropertiesTo(it) }
    }
}
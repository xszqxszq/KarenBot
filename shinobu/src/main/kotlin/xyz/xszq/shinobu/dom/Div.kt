package xyz.xszq.shinobu.dom

import org.jetbrains.skia.*
import xyz.xszq.shinobu.style.BackgroundPosition
import xyz.xszq.shinobu.style.BackgroundSize

@Suppress("unused")
class Div(
    id: String ?= null
) : Element(id) {
    var bgSkiaImage: Image? = null
    var maskSkiaImage: Image? = null
    override fun draw(canvas: Canvas) {
        style.backgroundColor ?.let { colorInt ->
            Paint().apply { color = colorInt }.use { paint ->
                canvas.drawRect(Rect.makeWH(measuredWidth, measuredHeight), paint)
            }
        }

        bgSkiaImage ?.let { image ->
            val imgW = image.width.toFloat()
            val imgH = image.height.toFloat()
            val destW = measuredWidth
            val destH = measuredHeight

            val bgPaint = Paint().apply {
                if (style.backgroundOpacity < 1.0f) {
                    alpha = (style.backgroundOpacity * 255).toInt().coerceIn(0, 255)
                }
            }

            val hasMask = maskSkiaImage != null
            if (hasMask) {
                canvas.saveLayer(Rect.makeWH(destW, destH), null)
            }

            fun getSamplingMode(drawW: Float, drawH: Float): SamplingMode {
                val noScale = (drawW == imgW && drawH == imgH)
                return if (noScale) {
                    SamplingMode.DEFAULT
                } else if (drawW > imgW || drawH > imgH) {
                    SamplingMode.LINEAR
                } else {
                    SamplingMode.MITCHELL
                }
            }

            when (style.backgroundSize) {
                BackgroundSize.STRETCH_FILL -> {
                    val destRect = Rect.makeWH(destW, destH)
                    canvas.drawImageRect(image, Rect.makeWH(imgW, imgH), destRect, getSamplingMode(destW, destH), bgPaint, true)
                }
                BackgroundSize.AUTO -> {
                    canvas.save()
                    canvas.clipRect(Rect.makeWH(destW, destH))
                    canvas.drawImageRect(image, Rect.makeWH(imgW, imgH), Rect.makeWH(imgW, imgH), SamplingMode.DEFAULT, bgPaint, true)
                    canvas.restore()
                }
                BackgroundSize.COVER -> {
                    val scale = maxOf(destW / imgW, destH / imgH)
                    val drawW = imgW * scale
                    val drawH = imgH * scale
                    val dx = when (style.backgroundPosition) {
                        BackgroundPosition.TOP_LEFT, BackgroundPosition.CENTER_LEFT, BackgroundPosition.BOTTOM_LEFT -> 0f
                        BackgroundPosition.TOP_CENTER, BackgroundPosition.CENTER, BackgroundPosition.BOTTOM_CENTER -> (destW - drawW) / 2f
                        BackgroundPosition.TOP_RIGHT, BackgroundPosition.CENTER_RIGHT, BackgroundPosition.BOTTOM_RIGHT -> destW - drawW
                    }

                    val dy = when (style.backgroundPosition) {
                        BackgroundPosition.TOP_LEFT, BackgroundPosition.TOP_CENTER, BackgroundPosition.TOP_RIGHT -> 0f
                        BackgroundPosition.CENTER_LEFT, BackgroundPosition.CENTER, BackgroundPosition.CENTER_RIGHT -> (destH - drawH) / 2f
                        BackgroundPosition.BOTTOM_LEFT, BackgroundPosition.BOTTOM_CENTER, BackgroundPosition.BOTTOM_RIGHT -> destH - drawH
                    }

                    canvas.save()
                    canvas.clipRect(Rect.makeWH(destW, destH))
                    canvas.drawImageRect(image, Rect.makeWH(imgW, imgH), Rect.makeXYWH(dx, dy, drawW, drawH), getSamplingMode(drawW, drawH), bgPaint, true)
                    canvas.restore()
                }
                BackgroundSize.CONTAIN -> {
                    val scale = minOf(destW / imgW, destH / imgH)
                    val drawW = imgW * scale
                    val drawH = imgH * scale
                    val dx = (destW - drawW) / 2f
                    val dy = (destH - drawH) / 2f

                    canvas.drawImageRect(image, Rect.makeWH(imgW, imgH), Rect.makeXYWH(dx, dy, drawW, drawH), getSamplingMode(drawW, drawH), bgPaint, true)
                }
            }

            if (hasMask) {
                Paint().apply { blendMode = BlendMode.DST_IN }.use { maskPaint ->
                    canvas.drawImageRect(
                        maskSkiaImage!!,
                        Rect.makeWH(maskSkiaImage!!.width.toFloat(), maskSkiaImage!!.height.toFloat()),
                        Rect.makeWH(destW, destH),
                        SamplingMode.DEFAULT,
                        maskPaint,
                        true
                    )
                }

                canvas.restore()
            }
            bgPaint.close()
        }

        children.forEach { child ->
            canvas.save()
            canvas.translate(child.layoutX, child.layoutY)

            child.renderPicture ?.let { childPic ->
                canvas.drawPicture(childPic)
            } ?: run {
                child.draw(canvas)
            }

            canvas.restore()
        }
    }

    override fun clone(): Element {
        return Div(this.id).also { copyBasePropertiesTo(it) }
    }
}
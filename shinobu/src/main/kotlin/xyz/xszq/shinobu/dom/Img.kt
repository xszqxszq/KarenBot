package xyz.xszq.shinobu.dom

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect.Companion.makeWH
import org.jetbrains.skia.SamplingMode
import xyz.xszq.shinobu.style.ObjectFit

@Suppress("unused")
class Img(
    id: String ?= null,
    var src: String?
) : Element(id) {
    var skiaImage: Image? = null
    var maskSkiaImage: Image? = null

    override fun draw(canvas: Canvas) {
        skiaImage?.let { img ->
            val imgW = img.width.toFloat()
            val imgH = img.height.toFloat()
            val boxW = contentRect.width
            val boxH = contentRect.height

            if (imgW <= 0f || imgH <= 0f || boxW <= 0f || boxH <= 0f)
                return

            var dstW = boxW
            var dstH = boxH

            when (style.objectFit) {
                ObjectFit.NONE -> {
                    dstW = imgW
                    dstH = imgH
                }
                ObjectFit.CONTAIN -> {
                    val scale = minOf(boxW / imgW, boxH / imgH)
                    dstW = imgW * scale
                    dstH = imgH * scale
                }
                ObjectFit.COVER -> {
                    val scale = maxOf(boxW / imgW, boxH / imgH)
                    dstW = imgW * scale
                    dstH = imgH * scale
                }
                ObjectFit.FILL -> {
                }
            }

            val dx = contentRect.left + (boxW - dstW) / 2f
            val dy = contentRect.top + (boxH - dstH) / 2f

            val dstRect = org.jetbrains.skia.Rect.makeXYWH(dx, dy, dstW, dstH)

            val noScale = (imgW == dstW && imgH == dstH)
            val samplingMode = if (noScale) {
                SamplingMode.DEFAULT
            } else if (dstW > imgW || dstH > imgH) {
                SamplingMode.LINEAR
            } else {
                SamplingMode.MITCHELL
            }

            val hasMask = maskSkiaImage != null

            if (hasMask) {
                canvas.saveLayer(contentRect, null)
            } else {
                canvas.save()
            }

            canvas.clipRect(contentRect)
            canvas.drawImageRect(img, makeWH(imgW, imgH), dstRect, samplingMode, null, true)

            if (hasMask) {
                val maskPaint = Paint().apply {
                    blendMode = org.jetbrains.skia.BlendMode.DST_IN
                }

                canvas.drawImageRect(
                    maskSkiaImage!!,
                    makeWH(maskSkiaImage!!.width.toFloat(), maskSkiaImage!!.height.toFloat()),
                    contentRect,
                    SamplingMode.DEFAULT,
                    maskPaint,
                    true
                )
            }

            canvas.restore()
        }
    }

    override fun clone(): Element {
        return Img(this.id, this.src).also { copyBasePropertiesTo(it) }
    }

    companion object {
        fun Img.noScale() = apply { style.objectFit = ObjectFit.NONE }
    }
}
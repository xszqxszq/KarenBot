package xyz.xszq.bot.image

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImageOrBitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.Font
import com.soywiz.korim.font.measureTextGlyphs
import com.soywiz.korim.format.readNativeImage
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korio.file.std.localCurrentDirVfs
import xyz.xszq.bot.maimai.MultiPlatformNativeSystemFontProvider

object BlueArchiveLogo {
    private const val transparentBg: Boolean = false
    private const val canvasHeight: Int = 250
    private const val size: Int = 84
    private const val textBaseLine: Double = 0.68
    private const val horizontalTilt: Double = -0.4
    private const val paddingX: Int = 10
    private const val graphOffsetX: Int = -15
    private const val graphOffsetY: Int = 0
    private val defaultFont = globalFontRegistry.loadFontByName("RoGSanSrfStd-Bd")
        ?: globalFontRegistry.defaultFont()
    private val fallbackFont = globalFontRegistry.loadFontByName("Glow Sans SC Normal Heavy")
        ?: globalFontRegistry.defaultFont()
    private val hollowPath = listOf(
        Pair(284, 136),
        Pair(321, 153),
        Pair(159, 410),
        Pair(148, 403)
    )
    private fun getFont(text: String): Font {
        if (text.any { defaultFont[it] == null })
            return fallbackFont
        text.forEach {
            val glyphs = defaultFont.measureTextGlyphs(size.toDouble(), it.toString())
            if (it.code > 255 && glyphs.metrics.width < 16.0)
                return fallbackFont
        }
        return defaultFont
    }
    suspend fun draw(textL: String, textR: String): Bitmap {
        val fontL = getFont(textL)
        val fontR = getFont(textR)
        val textMetricsL = fontL.measureTextGlyphs(size.toDouble(), textL)
        val textMetricsR = fontR.measureTextGlyphs(size.toDouble(), textR)
        val textWidthL = textMetricsL.metrics.width -
                (textBaseLine * canvasHeight + textMetricsL.fmetrics.bottom - textMetricsL.fmetrics.baseline) *
                horizontalTilt
        val textWidthR = textMetricsR.metrics.width +
                (textBaseLine * canvasHeight - textMetricsL.fmetrics.bottom - textMetricsL.fmetrics.baseline) *
                horizontalTilt
        val canvasWidthL = textWidthL + paddingX
        val canvasWidthR = textWidthR + paddingX
        val realWidth = canvasWidthL + canvasWidthR + 80
        val result = NativeImageOrBitmap32(realWidth.toInt(), canvasHeight, true)
        return result.context2d {
            fontSize = size.toDouble()
            if (!transparentBg) {
                fillStyle = Colors.WHITE
                fillRect(0, 0, this.width, this.height)
            }
            fillStyle = RGBA(0x12, 0x8a, 0xfa, 0xff)
            // alignment = TextAlignment.RIGHT
            setTransform(1.0, 0.0, horizontalTilt, 1.0, 0.0, 0.0)
            font = fontL
            fillText(textL, canvasWidthL - textWidthL + 30 +
                    if (fontL.name == "RoGSanSrfStd-Bd") 45 else 40, height * textBaseLine)
            setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
            drawImage(
                localCurrentDirVfs["image/BlueArchive/halo.png"].readNativeImage(),
                canvasWidthL - this.height / 2 + graphOffsetX,
                graphOffsetY,
                canvasHeight,
                canvasHeight
            )
            fillStyle = RGBA(0x2b, 0x2b, 0x2b, 0xff)
            // alignment = TextAlignment.LEFT
            strokeStyle = Colors.WHITE
            lineWidth = 12.0
            setTransform(1.0, 0.0, horizontalTilt, 1.0, 0.0, 0.0)
            font = fontR
            strokeText(textR, canvasWidthL + 30, height * textBaseLine)
            fillStyle = Colors.BLACK
            fillText(textR, canvasWidthL + 30, height * textBaseLine)
            setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
            val graphX = canvasWidthL - height / 2 + graphOffsetX
            val graphY = graphOffsetY
            beginPath()
            moveTo(
                graphX + (hollowPath[0].first / 500.0) * canvasHeight,
                graphY + (hollowPath[0].second / 500.0) * canvasHeight
            )
            for (i in 1 until 4) {
                lineTo(
                    graphX + (hollowPath[i].first / 500.0) * canvasHeight,
                    graphY + (hollowPath[i].second / 500.0) * canvasHeight
                )
            }
            close()
            fillStyle = Colors.WHITE
            fill()
            drawImage(
                localCurrentDirVfs["image/BlueArchive/cross.png"].readNativeImage(),
                canvasWidthL - this.height / 2 + graphOffsetX,
                graphOffsetY,
                canvasHeight,
                canvasHeight
            )
            dispose()
        }
    }
}
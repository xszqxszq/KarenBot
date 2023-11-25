package xyz.xszq.bot

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.NativeImageOrBitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGB
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.SystemFontRegistry
import com.soywiz.korim.font.TextMetrics
import com.soywiz.korim.font.TextMetricsResult
import com.soywiz.korim.font.measureTextGlyphs
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korim.format.readNativeImage
import com.soywiz.korim.paint.ColorPaint
import com.soywiz.korim.paint.Paint
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.file.std.localCurrentDirVfs
import kotlinx.coroutines.delay
import xyz.xszq.bot.maimai.ItemPosition
import xyz.xszq.bot.maimai.MultiPlatformNativeSystemFontProvider
import xyz.xszq.bot.maimai.drawText
import kotlin.properties.Delegates

class BlueArchiveLogo(
    val transparentBg: Boolean,
    val canvasHeight: Int = 250,
    val canvasWidth: Int = 900,
    val size: Int = 84,
    val textBaseLine: Double = 0.68,
    val horizontalTilt: Double = -0.4,
    val paddingX: Int = 10,
    val graphOffsetX: Int = -15,
    val graphOffsetY: Int = 0
) {
    lateinit var textMetricsL: TextMetricsResult
    lateinit var textMetricsR: TextMetricsResult
    var textWidthL by Delegates.notNull<Double>()
    var textWidthR by Delegates.notNull<Double>()
    var canvasWidthL by Delegates.notNull<Double>()
    var canvasWidthR by Delegates.notNull<Double>()
    var realWidth by Delegates.notNull<Double>()
    val fonts = MultiPlatformNativeSystemFontProvider(localCurrentDirVfs["font"].absolutePath)
    private val hollowPath = listOf(
        Pair(284, 136),
        Pair(321, 153),
        Pair(159, 410),
        Pair(148, 403)
    )
    private fun Context2d.setWidth() {
        textWidthL =
            textMetricsL.metrics.width -
        (textBaseLine * canvasHeight + textMetricsL.fmetrics.bottom - textMetricsL.fmetrics.baseline) * horizontalTilt
        textWidthR =
            textMetricsR.metrics.width +
        (textBaseLine * canvasHeight - textMetricsL.fmetrics.bottom - textMetricsL.fmetrics.baseline) * horizontalTilt
        canvasWidthL = textWidthL + paddingX
        canvasWidthR = textWidthR + paddingX
        realWidth = canvasWidthL + canvasWidthR + 80
    }
    fun getFont(text: String): String {
        val now = fonts.loadFontByName("RoGSanSrfStd-Bd")!!
        text.forEach {
            val glyphs = now.measureTextGlyphs(size.toDouble(), it.toString())
            if (it.code > 255 && glyphs.metrics.width < 16.0)
                return "Glow Sans SC Normal Heavy"
        }
        return "RoGSanSrfStd-Bd"
    }
    suspend fun draw(textL: String, textR: String): Bitmap {
        var result = NativeImageOrBitmap32(canvasWidth, canvasHeight, true)
        val fontL = getFont(textL)
        val fontR = getFont(textR)
        result.context2d {
            fontSize = size.toDouble()
            textMetricsL = fonts.loadFontByName(fontL)!!.measureTextGlyphs(fontSize, textL)
            textMetricsR = fonts.loadFontByName(fontR)!!.measureTextGlyphs(fontSize, textR)
            setWidth()
        }
        result = NativeImageOrBitmap32(realWidth.toInt(), canvasHeight, true)
        return result.context2d {
            fontSize = size.toDouble()
            if (!transparentBg) {
                fillStyle = Colors.WHITE
                fillRect(0, 0, this.width, this.height)
            }
            fillStyle = RGBA(0x12, 0x8a, 0xfa, 0xff)
            alignment = TextAlignment.RIGHT
            setTransform(1.0, 0.0, horizontalTilt, 1.0, 0.0, 0.0)
            font = fonts.loadFontByName(fontL)
            fillText(textL, canvasWidthL - textWidthL + 30 + if (fontL == "RoGSanSrfStd-Bd") 45 else 40, height * textBaseLine)
            setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
            drawImage(
                localCurrentDirVfs["image/BlueArchive/halo.png"].readNativeImage(),
                canvasWidthL - this.height / 2 + graphOffsetX,
                graphOffsetY,
                canvasHeight,
                canvasHeight
            )
            fillStyle = RGBA(0x2b, 0x2b, 0x2b, 0xff)
            alignment = TextAlignment.LEFT
            strokeStyle = Colors.WHITE
            lineWidth = 12.0
            setTransform(1.0, 0.0, horizontalTilt, 1.0, 0.0, 0.0)
            font = fonts.loadFontByName(fontR)
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
            );
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
package xyz.xszq.bot.image

import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.NativeImageOrBitmap32
import korlibs.image.bitmap.context2d
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.font.TtfFont
import korlibs.image.format.readNativeImage
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.math.geom.Point
import korlibs.math.geom.Size
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

object BlueArchiveLogo {
    private const val transparentBg: Boolean = false
    private const val canvasHeight: Int = 250
    private const val size: Float = 84F
    private const val textBaseLine: Double = 0.68
    private const val horizontalTilt: Double = -0.4
    private const val paddingX: Int = 10
    private const val graphOffsetX: Int = -15
    private const val graphOffsetY: Int = 0
    private val defaultFont = globalFontRegistry.loadFontByName("RoGSanSrfStd-Bd")!!
    private val fallbackFont = globalFontRegistry.loadFontByName("Glow Sans SC Normal Heavy")!!
    private val hollowPath = listOf(
        Pair(284, 136),
        Pair(321, 153),
        Pair(159, 410),
        Pair(148, 403)
    )
    private suspend fun getFont(text: String): TtfFont {
        if (text.any {
            fontLock.withLock {
                defaultFont[it] == null
            }
        })
            return fallbackFont
        text.forEach {
            val glyphs = defaultFont.measureTextGlyphs(size, it.toString())
            if (it.code > 255 && glyphs.metrics.bounds.x == 34.272F)
                return fallbackFont
        }
        return defaultFont
    }
    suspend fun draw(textL: String, textR: String): Bitmap = MemeGenerator.semaphore.withPermit {
        kotlin.runCatching {
            val fontL = getFont(textL)
            val fontR = getFont(textR)
            val textMetricsL = fontL.measureTextGlyphs(size, textL)
            val textMetricsR = fontR.measureTextGlyphs(size, textR)
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
            result.context2d {
                fontSize = size
                if (!transparentBg) {
                    fillStyle = Colors.WHITE
                    fillRect(0, 0, this.width, this.height)
                }
                fillStyle = RGBA(0x12, 0x8a, 0xfa, 0xff)
                // alignment = TextAlignment.RIGHT
                setTransform(1.0, 0.0, horizontalTilt, 1.0, 0.0, 0.0)
                font = fontL
                fillTextSafe(textL, Point(
                    canvasWidthL - textWidthL + 30 +
                            if (fontL.ttfCompleteName == "RoGSanSrfStd-Bd") 45 else 40,
                    height * textBaseLine)
                )
                setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
                drawImage(
                    localCurrentDirVfs["image/BlueArchive/halo.png"].readNativeImage(),
                    Point(canvasWidthL - this.height / 2 + graphOffsetX, graphOffsetY),
                    Size(canvasHeight, canvasHeight)
                )
                fillStyle = RGBA(0x2b, 0x2b, 0x2b, 0xff)
                // alignment = TextAlignment.LEFT
                strokeStyle = Colors.WHITE
                lineWidth = 12.0F
                setTransform(1.0, 0.0, horizontalTilt, 1.0, 0.0, 0.0)
                font = fontR
                strokeTextSafe(textR, Point(canvasWidthL + 30, height * textBaseLine))
                fillStyle = Colors.BLACK
                fillTextSafe(textR, Point(canvasWidthL + 30, height * textBaseLine))
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
                    Point(canvasWidthL - this.height / 2 + graphOffsetX, graphOffsetY),
                    Size(canvasHeight, canvasHeight)
                )
                dispose()
            }
        }.onFailure {
            it.printStackTrace()
        }
    }.getOrThrow()
}
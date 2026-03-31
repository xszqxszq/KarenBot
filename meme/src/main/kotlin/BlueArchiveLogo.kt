package xyz.xszq.bot

import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.NativeImage
import korlibs.image.bitmap.NativeImageOrBitmap32
import korlibs.image.bitmap.context2d
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.font.FontRegistry
import korlibs.image.font.SystemFontRegistry
import korlibs.image.font.getTextBoundsWithGlyphs
import korlibs.image.format.readNativeImage
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.math.geom.Point
import korlibs.math.geom.Size
import korlibs.math.isAlmostEquals

class BlueArchiveLogo {
    val imgDir = localCurrentDirVfs[ASSETS_DIR]
    lateinit var halo: NativeImage
    lateinit var cross: NativeImage
    lateinit var registry: FontRegistry
    suspend fun init() {
        halo = imgDir["halo.png"].readNativeImage()
        cross = imgDir["cross.png"].readNativeImage()
        registry = SystemFontRegistry()
    }
    private fun getFont(text: String): String {
        val default = registry[DEFAULT_FONT]
        text.forEach {
            val glyphs = default.getTextBoundsWithGlyphs(SIZE, it.toString())
            if (it.code > 255 && (glyphs.metrics.bounds.x.isAlmostEquals(34.272) || glyphs.metrics.bounds.x == 0.0))
                return FALLBACK_FONT
        }
        return DEFAULT_FONT
    }
    fun draw(textL: String, textR: String): Bitmap {
        val fontL = registry[getFont(textL)]
        val fontR = registry[getFont(textR)]
        val textMetricsL = fontL.getTextBoundsWithGlyphs(SIZE, textL)
        val textMetricsR = fontR.getTextBoundsWithGlyphs(SIZE, textR)
        val textWidthL = textMetricsL.metrics.width -
                (TEXT_BASELINE * CANVAS_HEIGHT + textMetricsL.fmetrics.descent) * HORIZONTAL_TILT
        val textWidthR = textMetricsR.metrics.width +
                (TEXT_BASELINE * CANVAS_HEIGHT - textMetricsL.fmetrics.ascent) * HORIZONTAL_TILT
        val canvasWidthL = textWidthL + PADDING_X
        val canvasWidthR = textWidthR + PADDING_X
        val realWidth = canvasWidthL + canvasWidthR
        val result = NativeImageOrBitmap32(realWidth.toInt(), CANVAS_HEIGHT, true)
        return result.context2d {
            fontSize = SIZE
            fillStyle = RGBA(0x12, 0x8a, 0xfa, 0xff)
            setTransform(1.0, 0.0, HORIZONTAL_TILT, 1.0, 0.0, 0.0)
            font = fontL
            fillText(textL, Point(canvasWidthL - textMetricsL.metrics.width, height * TEXT_BASELINE))

            setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
            drawImage(halo,
                Point(canvasWidthL - this.height / 2 + GRAPH_OFFSET_X, GRAPH_OFFSET_Y),
                Size(CANVAS_HEIGHT, CANVAS_HEIGHT)
            )

            fillStyle = RGBA(0x2b, 0x2b, 0x2b, 0xff)
            strokeStyle = Colors.WHITE
            lineWidth = 12.0
            setTransform(1.0, 0.0, HORIZONTAL_TILT, 1.0, 0.0, 0.0)
            font = fontR
            strokeText(textR, Point(canvasWidthL, height * TEXT_BASELINE))

            fillStyle = Colors.BLACK
            fillText(textR, Point(canvasWidthL, height * TEXT_BASELINE))
            setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
            val graphX = canvasWidthL - height / 2 + GRAPH_OFFSET_X
            val graphY = GRAPH_OFFSET_Y
            beginPath()
            moveTo(
                graphX + (HOLLOW_PATH[0].first / 500.0) * CANVAS_HEIGHT,
                graphY + (HOLLOW_PATH[0].second / 500.0) * CANVAS_HEIGHT
            )
            for (i in 1 until 4) {
                lineTo(
                    graphX + (HOLLOW_PATH[i].first / 500.0) * CANVAS_HEIGHT,
                    graphY + (HOLLOW_PATH[i].second / 500.0) * CANVAS_HEIGHT
                )
            }
            close()
            fillStyle = Colors.WHITE
            fill()

            drawImage(
                cross,
                Point(canvasWidthL - this.height / 2 + GRAPH_OFFSET_X, GRAPH_OFFSET_Y),
                Size(CANVAS_HEIGHT, CANVAS_HEIGHT)
            )
            dispose()
        }
    }
    companion object {
        private const val ASSETS_DIR = "./data/meme/ba/"
        private const val CANVAS_HEIGHT: Int = 250
        private const val SIZE: Double = 84.0
        private const val TEXT_BASELINE: Double = 0.68
        private const val HORIZONTAL_TILT: Double = -0.4
        private const val PADDING_X: Int = 10
        private const val GRAPH_OFFSET_X: Int = -15
        private const val GRAPH_OFFSET_Y: Int = 0
        private const val DEFAULT_FONT = "RoGSanSrfStd-Bd"
        private const val FALLBACK_FONT = "Glow Sans SC Normal Heavy"
        private val HOLLOW_PATH = listOf(
            Pair(284, 136),
            Pair(321, 153),
            Pair(159, 410),
            Pair(148, 403)
        )
    }
}
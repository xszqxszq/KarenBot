package xyz.xszq.bot.meme

import org.jetbrains.skia.*
import java.io.File

class BlueArchiveLogo {
    val imgDir = File(ASSETS_DIR)
    lateinit var halo: Image
    lateinit var cross: Image
    private var defaultTypeface: Typeface? = null
    private var fallbackTypeface: Typeface? = null
    fun init() {
        halo = Image.makeFromEncoded(File(imgDir, "halo.png").readBytes())
        cross = Image.makeFromEncoded(File(imgDir, "cross.png").readBytes())
        defaultTypeface = matchFamily(DEFAULT_FONT, "Ro GSan Serif Std B", weight = 400)
        fallbackTypeface = matchFamily(FALLBACK_FONT, "Glow Sans SC", weight = 900)
    }
    private fun getTypeface(text: String): Typeface {
        val typeface = defaultTypeface!!
        text.forEach { char ->
            if (char.code > 255) {
                val glyphId = Font(typeface, SIZE).getUTF32Glyph(char.code)
                if (glyphId == 0.toShort())
                    return fallbackTypeface!!
            }
        }
        return typeface
    }
    fun draw(textL: String, textR: String): Image {
        val typefaceL = getTypeface(textL)
        val typefaceR = getTypeface(textR)
        val fontL = Font(typefaceL, SIZE)
        val fontR = Font(typefaceR, SIZE)
        val textWidthL = fontL.measureTextWidth(textL) -
                (TEXT_BASELINE * CANVAS_HEIGHT + fontL.metrics.descent) * HORIZONTAL_TILT
        val textWidthR = fontR.measureTextWidth(textR) +
                (TEXT_BASELINE * CANVAS_HEIGHT + fontR.metrics.ascent) * HORIZONTAL_TILT
        val canvasWidthL = textWidthL + PADDING_X
        val canvasWidthR = textWidthR + PADDING_X
        val realWidth = (canvasWidthL + canvasWidthR).toInt()
        val surface = Surface.makeRasterN32Premul(realWidth, CANVAS_HEIGHT)
        val canvas = surface.canvas

        val bluePaint = Paint().apply {
            color = Color.makeRGB(0x12, 0x8a, 0xfa)
            isAntiAlias = true
        }
        canvas.save()
        canvas.skew(HORIZONTAL_TILT, 0f)
        canvas.drawString(
            textL,
            canvasWidthL - fontL.measureTextWidth(textL),
            CANVAS_HEIGHT * TEXT_BASELINE,
            fontL,
            bluePaint
        )
        canvas.restore()
        bluePaint.close()

        canvas.drawImageRect(
            halo,
            Rect.makeWH(halo.width.toFloat(), halo.height.toFloat()),
            Rect.makeXYWH(
                canvasWidthL - CANVAS_HEIGHT / 2f + GRAPH_OFFSET_X,
                GRAPH_OFFSET_Y,
                CANVAS_HEIGHT.toFloat(),
                CANVAS_HEIGHT.toFloat()
            )
        )

        val p = Paint().apply {
            strokeJoin = PaintStrokeJoin.ROUND
            strokeCap = PaintStrokeCap.ROUND
            isAntiAlias = true
        }
        canvas.save()
        canvas.skew(HORIZONTAL_TILT, 0f)
        p.color = Color.WHITE
        p.mode = PaintMode.STROKE
        p.strokeWidth = 12f
        canvas.drawString(textR, canvasWidthL, CANVAS_HEIGHT * TEXT_BASELINE, fontR, p)
        p.color = Color.makeRGB(43, 43, 43)
        p.mode = PaintMode.FILL
        canvas.drawString(textR, canvasWidthL, CANVAS_HEIGHT * TEXT_BASELINE, fontR, p)
        canvas.restore()
        p.close()

        val graphX = canvasWidthL - CANVAS_HEIGHT / 2f + GRAPH_OFFSET_X
        val graphY = GRAPH_OFFSET_Y
        val vertexes = floatArrayOf(
            graphX + (HOLLOW_PATH[0].first / 500f) * CANVAS_HEIGHT, graphY + (HOLLOW_PATH[0].second / 500f) * CANVAS_HEIGHT,
            graphX + (HOLLOW_PATH[1].first / 500f) * CANVAS_HEIGHT, graphY + (HOLLOW_PATH[1].second / 500f) * CANVAS_HEIGHT,
            graphX + (HOLLOW_PATH[2].first / 500f) * CANVAS_HEIGHT, graphY + (HOLLOW_PATH[2].second / 500f) * CANVAS_HEIGHT,
            graphX + (HOLLOW_PATH[3].first / 500f) * CANVAS_HEIGHT, graphY + (HOLLOW_PATH[3].second / 500f) * CANVAS_HEIGHT
        )
        Paint().apply { color = Color.WHITE }.use { hollowFill ->
            canvas.drawVertices(VertexMode.TRIANGLE_STRIP, vertexes, null, null, null, BlendMode.SRC_OVER, hollowFill)
        }

        canvas.drawImageRect(
            cross,
            Rect.makeWH(cross.width.toFloat(), cross.height.toFloat()),
            Rect.makeXYWH(
                canvasWidthL - CANVAS_HEIGHT / 2f + GRAPH_OFFSET_X,
                GRAPH_OFFSET_Y,
                CANVAS_HEIGHT.toFloat(),
                CANVAS_HEIGHT.toFloat()
            )
        )
        val result = surface.makeImageSnapshot()!!
        fontL.close()
        fontR.close()
        surface.close()
        return result
    }
    companion object {
        private const val ASSETS_DIR = "./data/meme/ba/"
        private const val CANVAS_HEIGHT: Int = 250
        private const val SIZE: Float = 84f
        private const val TEXT_BASELINE: Float = 0.68f
        private const val HORIZONTAL_TILT: Float = -0.4f
        private const val PADDING_X: Int = 10
        private const val GRAPH_OFFSET_X: Float = -15f
        private const val GRAPH_OFFSET_Y: Float = 0f
        private const val DEFAULT_FONT = "Ro GSan Serif Std B"
        private const val FALLBACK_FONT = "Glow Sans SC Normal Heavy"
        private val HOLLOW_PATH = listOf(
            Pair(284, 136),
            Pair(321, 153),
            Pair(159, 410),
            Pair(148, 403)
        )
    }
}
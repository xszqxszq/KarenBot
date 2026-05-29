package xyz.xszq.bot

import org.jetbrains.skia.*
import kotlin.math.max

class FiveThousandChoyen {
    private var topTypeface: Typeface? = null
    private var botTypeface: Typeface? = null
    fun init() {
        topTypeface = matchFamily(TOP_FONT, "Source Han Sans", weight = 700)
        botTypeface = matchFamily(BOTTOM_FONT, "Source Han Serif CN", weight = 700)
    }
    fun draw(top: String, bottom: String ?= null): Image {
        var rightBorder = 0f
        val surface = Surface.makeRasterN32Premul(1500, 270)
        val canvas = surface.canvas
        canvas.drawRect(Rect.makeWH(1500f, 270f), Paint().also { it.color = Color.WHITE })
        with (canvas) {
            val topW = drawTop(top)
            rightBorder = TOP_X + topW
            bottom?.let {
                val botW = drawBottom(it)
                rightBorder = max(rightBorder, BOTTOM_X + botW)
            }
        }
        return surface.makeImageSnapshot(IRect.makeXYWH(0, 0, rightBorder.toInt(), 270))!!
    }
    fun Canvas.drawTop(
        top: String,
        x: Float = TOP_X,
        y: Float = TOP_Y
    ): Float {
        val font = Font(topTypeface!!, SIZE)
        save()
        skew(-0.45f, 0f)

        val p = Paint().apply {
            strokeJoin = PaintStrokeJoin.ROUND
            strokeCap = PaintStrokeCap.ROUND
        }

        p.color = Color.BLACK
        p.mode = PaintMode.STROKE
        p.strokeWidth = 22f
        p.shader = null
        drawString(top, x + 4, y + 4, font, p)

        p.color = Color.BLACK
        p.shader = Shader.makeLinearGradient(0f, 24f, 0f, 122f,
            intArrayOf(
                Color.makeRGB(0, 15, 36), Color.makeRGB(255, 255, 255),
                Color.makeRGB(55, 58, 59), Color.makeRGB(55, 58, 59),
                Color.makeRGB(200, 200, 200), Color.makeRGB(55, 58, 59),
                Color.makeRGB(25, 20, 31), Color.makeRGB(240, 240, 240),
                Color.makeRGB(166, 175, 194), Color.makeRGB(50, 50, 50)
            ), floatArrayOf(0f, .10f, .18f, .25f, .5f, .75f, .85f, .91f, .95f, 1f),
            GradientStyle(FilterTileMode.CLAMP, false, Matrix33.IDENTITY))
        p.strokeWidth = 20f
        p.mode = PaintMode.STROKE
        drawString(top, x + 4, y + 4, font, p)

        p.shader = null
        p.color = Color.BLACK
        p.strokeWidth = 16f
        p.mode = PaintMode.STROKE
        drawString(top, x, y, font, p)

        p.shader = Shader.makeLinearGradient(0f, 20f, 0f, 100f,
            intArrayOf(
                Color.makeRGB(253, 241, 0), Color.makeRGB(245, 253, 187),
                Color.makeRGB(255, 255, 255), Color.makeRGB(253, 219, 9),
                Color.makeRGB(127, 53, 0), Color.makeRGB(243, 196, 11)
            ), floatArrayOf(0f, .25f, .4f, .75f, .9f, 1f),
            GradientStyle(FilterTileMode.CLAMP, false, Matrix33.IDENTITY))
        p.strokeWidth = 10f
        p.mode = PaintMode.STROKE
        drawString(top, x, y, font, p)

        p.shader = null
        p.color = Color.BLACK
        p.strokeWidth = 6f
        p.mode = PaintMode.STROKE
        drawString(top, x + 2, y - 3, font, p)

        p.color = Color.WHITE
        p.strokeWidth = 6f
        p.mode = PaintMode.STROKE
        drawString(top, x, y - 3, font, p)

        p.shader = Shader.makeLinearGradient(0f, 20f, 0f, 100f,
            intArrayOf(
                Color.makeRGB(255, 100, 0), Color.makeRGB(123, 0, 0),
                Color.makeRGB(240, 0, 0), Color.makeRGB(5, 0, 0)
            ), floatArrayOf(0f, .5f, .51f, 1f),
            GradientStyle(FilterTileMode.CLAMP, false, Matrix33.IDENTITY))
        p.color = Color.makeRGB(200, 0, 0)
        p.strokeWidth = 4f
        p.mode = PaintMode.STROKE
        drawString(top, x, y - 3, font, p)

        p.shader = Shader.makeLinearGradient(0f, 20f, 0f, 100f,
            intArrayOf(
                Color.makeRGB(230, 0, 0), Color.makeRGB(123, 0, 0),
                Color.makeRGB(240, 0, 0), Color.makeRGB(5, 0, 0)
            ), floatArrayOf(0f, .5f, .51f, 1f),
            GradientStyle(FilterTileMode.CLAMP, false, Matrix33.IDENTITY))
        p.color = Color.makeRGB(200, 0, 0)
        p.mode = PaintMode.FILL
        drawString(top, x, y - 3, font, p)

        restore()
        return font.measureTextWidth(top)
    }

    fun Canvas.drawBottom(
        bottom: String,
        x: Float = BOTTOM_X,
        y: Float = BOTTOM_Y
    ): Float {
        val font = Font(botTypeface!!, SIZE)
        save()
        skew(-0.45f, 0f)

        val p = Paint().apply {
            strokeJoin = PaintStrokeJoin.ROUND
            strokeCap = PaintStrokeCap.ROUND
        }

        p.color = Color.BLACK
        p.mode = PaintMode.STROKE
        p.strokeWidth = 22f
        p.shader = null
        drawString(bottom, x + 5, y + 2, font, p)

        p.shader = Shader.makeLinearGradient(0f, y - 80, 0f, y + 18,
            intArrayOf(
                Color.makeRGB(0, 15, 36), Color.makeRGB(250, 250, 250),
                Color.makeRGB(150, 150, 150), Color.makeRGB(55, 58, 59),
                Color.makeRGB(25, 20, 31), Color.makeRGB(240, 240, 240),
                Color.makeRGB(166, 175, 194), Color.makeRGB(50, 50, 50)
            ), floatArrayOf(0f, .25f, .5f, .75f, .85f, .91f, .95f, 1f),
            GradientStyle(FilterTileMode.CLAMP, false, Matrix33.IDENTITY))
        p.color = Color.makeRGB(200, 210, 220)
        p.strokeWidth = 19f
        p.mode = PaintMode.STROKE
        drawString(bottom, x + 5, y + 2, font, p)

        p.shader = null
        p.color = Color.makeRGB(16, 25, 58)
        p.strokeWidth = 17f
        p.mode = PaintMode.STROKE
        drawString(bottom, x, y, font, p)

        p.color = Color.makeRGB(221, 221, 221)
        p.strokeWidth = 8f
        p.mode = PaintMode.STROKE
        drawString(bottom, x, y, font, p)

        p.shader = Shader.makeLinearGradient(0f, y - 80, 0f, y,
            intArrayOf(
                Color.makeRGB(16, 25, 58), Color.makeRGB(255, 255, 255),
                Color.makeRGB(16, 25, 58), Color.makeRGB(16, 25, 58),
                Color.makeRGB(16, 25, 58)
            ), floatArrayOf(0f, .03f, .08f, .2f, 1f),
            GradientStyle(FilterTileMode.CLAMP, false, Matrix33.IDENTITY))
        p.color = Color.makeRGB(16, 25, 58)
        p.strokeWidth = 7f
        p.mode = PaintMode.STROKE
        drawString(bottom, x, y, font, p)

        p.shader = Shader.makeLinearGradient(0f, y - 80, 0f, y,
            intArrayOf(
                Color.makeRGB(245, 246, 248), Color.makeRGB(255, 255, 255),
                Color.makeRGB(195, 213, 220), Color.makeRGB(160, 190, 201),
                Color.makeRGB(160, 190, 201), Color.makeRGB(196, 215, 222),
                Color.makeRGB(255, 255, 255)
            ), floatArrayOf(0f, .15f, .35f, .5f, .51f, .52f, 1f),
            GradientStyle(FilterTileMode.CLAMP, false, Matrix33.IDENTITY))
        p.color = Color.makeRGB(200, 210, 220)
        p.mode = PaintMode.FILL
        drawString(bottom, x, y - 3, font, p)

        restore()
        return font.measureTextWidth(bottom)
    }

    companion object {
        const val TOP_FONT = "Source Han Sans CN Bold"
        const val BOTTOM_FONT = "Source Han Serif SC"
        const val SIZE = 100f
        const val TOP_X = 70f
        const val TOP_Y = 100f
        const val BOTTOM_X = 250f
        const val BOTTOM_Y = 230f
    }
}
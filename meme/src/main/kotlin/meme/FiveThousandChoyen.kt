package xyz.xszq.bot.meme

import org.jetbrains.skia.*
import kotlin.math.max

/**
 * 将上下两行文字渲染成 5000 兆円风格图
 */
class FiveThousandChoyen {
    /**
     * 垂直方向的线性渐变
     *
     * @property y0 渐变起点
     * @property y1 渐变终点
     * @property colors 渐变颜色
     * @property stops 颜色位置
     */
    private class Gradient(
        val y0: Float,
        val y1: Float,
        val colors: IntArray,
        val stops: FloatArray
    ) {
        fun toShader(): Shader = Shader.makeLinearGradient(
            0f, y0, 0f, y1, colors, stops,
            GradientStyle(FilterTileMode.CLAMP, false, Matrix33.IDENTITY)
        )
    }

    /**
     * 文字层
     *
     * @property x 横坐标
     * @property y 纵坐标
     * @property strokeWidth 描边宽度
     * @property color 画笔颜色
     * @property mode 绘制模式
     * @property gradient 渐变
     */
    private class Layer(
        val x: Float,
        val y: Float,
        val strokeWidth: Float,
        val color: Int,
        val mode: PaintMode = PaintMode.STROKE,
        val gradient: Gradient ?= null
    )

    private var topTypeface: Typeface? = null
    private var botTypeface: Typeface? = null
    /**
     * 加载字体
     */
    fun init() {
        topTypeface = matchFamily(TOP_FONT, "Source Han Sans", weight = 700)
        botTypeface = matchFamily(BOTTOM_FONT, "Source Han Serif CN", weight = 700)
    }
    /**
     * 绘制 5000 兆円风格图
     *
     * @param top 上行文字
     * @param bottom 下行文字，为 null 时只绘制上行
     * @return 生成结果
     */
    fun draw(top: String, bottom: String ?= null): Image {
        var rightBorder = 0f
        return Surface.makeRasterN32Premul(1500, 270).use { surface ->
            val canvas = surface.canvas
            Paint().apply { color = Color.WHITE }.use { bgPaint ->
                canvas.drawRect(Rect.makeWH(1500f, 270f), bgPaint)
            }
            with (canvas) {
                val topW = drawTop(top)
                rightBorder = TOP_X + topW
                bottom?.let {
                    val botW = drawBottom(it)
                    rightBorder = max(rightBorder, BOTTOM_X + botW)
                }
            }
            surface.makeImageSnapshot(IRect.makeXYWH(0, 0, rightBorder.toInt(), 270))!!
        }
    }

    /**
     * 绘制上方文本
     *
     * @param top 上方文本
     * @return 文本宽度
     */
    private fun Canvas.drawTop(top: String): Float {
        val red = Color.makeRGB(200, 0, 0)
        val redStops = floatArrayOf(0f, .5f, .51f, 1f)
        val outline = Gradient(24f, 122f, intArrayOf(
            Color.makeRGB(0, 15, 36), Color.makeRGB(255, 255, 255),
            Color.makeRGB(55, 58, 59), Color.makeRGB(55, 58, 59),
            Color.makeRGB(200, 200, 200), Color.makeRGB(55, 58, 59),
            Color.makeRGB(25, 20, 31), Color.makeRGB(240, 240, 240),
            Color.makeRGB(166, 175, 194), Color.makeRGB(50, 50, 50)
        ), floatArrayOf(
            0f, .10f, .18f, .25f, .5f, .75f, .85f, .91f, .95f, 1f
        ))
        val gold = Gradient(20f, 100f, intArrayOf(
            Color.makeRGB(253, 241, 0), Color.makeRGB(245, 253, 187),
            Color.makeRGB(255, 255, 255), Color.makeRGB(253, 219, 9),
            Color.makeRGB(127, 53, 0), Color.makeRGB(243, 196, 11)
        ), floatArrayOf(
            0f, .25f, .4f, .75f, .9f, 1f
        ))
        val edge = Gradient(20f, 100f, intArrayOf(
            Color.makeRGB(255, 100, 0), Color.makeRGB(123, 0, 0),
            Color.makeRGB(240, 0, 0), Color.makeRGB(5, 0, 0)
        ), redStops)
        val body = Gradient(20f, 100f, intArrayOf(
            Color.makeRGB(230, 0, 0), Color.makeRGB(123, 0, 0),
            Color.makeRGB(240, 0, 0), Color.makeRGB(5, 0, 0)
        ), redStops)
        val layers = listOf(
            Layer(TOP_X + 4, TOP_Y + 4, 22f, Color.BLACK),
            Layer(TOP_X + 4, TOP_Y + 4, 20f, Color.BLACK, gradient = outline),
            Layer(TOP_X, TOP_Y, 16f, Color.BLACK),
            Layer(TOP_X, TOP_Y, 10f, Color.BLACK, gradient = gold),
            Layer(TOP_X + 2, TOP_Y - 3, 6f, Color.BLACK),
            Layer(TOP_X, TOP_Y - 3, 6f, Color.WHITE),
            Layer(TOP_X, TOP_Y - 3, 4f, red, gradient = edge),
            Layer(TOP_X, TOP_Y - 3, 4f, red, PaintMode.FILL, body)
        )
        return drawLayers(top, topTypeface!!, layers)
    }

    /**
     * 绘制下方文本
     *
     * @param bottom 下方文本
     * @return 文本宽度
     */
    private fun Canvas.drawBottom(bottom: String): Float {
        val silver = Color.makeRGB(200, 210, 220)
        val navy = Color.makeRGB(16, 25, 58)
        val outline = Gradient(BOTTOM_Y - 80f, BOTTOM_Y + 18f, intArrayOf(
            Color.makeRGB(0, 15, 36), Color.makeRGB(250, 250, 250),
            Color.makeRGB(150, 150, 150), Color.makeRGB(55, 58, 59),
            Color.makeRGB(25, 20, 31), Color.makeRGB(240, 240, 240),
            Color.makeRGB(166, 175, 194), Color.makeRGB(50, 50, 50)
        ), floatArrayOf(
            0f, .25f, .5f, .75f, .85f, .91f, .95f, 1f
        ))
        val body = Gradient(BOTTOM_Y - 80f, BOTTOM_Y, intArrayOf(
            Color.makeRGB(16, 25, 58), Color.makeRGB(255, 255, 255),
            Color.makeRGB(16, 25, 58), Color.makeRGB(16, 25, 58),
            Color.makeRGB(16, 25, 58)
        ), floatArrayOf(
            0f, .03f, .08f, .2f, 1f
        ))
        val gloss = Gradient(BOTTOM_Y - 80f, BOTTOM_Y, intArrayOf(
            Color.makeRGB(245, 246, 248), Color.makeRGB(255, 255, 255),
            Color.makeRGB(195, 213, 220), Color.makeRGB(160, 190, 201),
            Color.makeRGB(160, 190, 201), Color.makeRGB(196, 215, 222),
            Color.makeRGB(255, 255, 255)
        ), floatArrayOf(
            0f, .15f, .35f, .5f, .51f, .52f, 1f
        ))
        val layers = listOf(
            Layer(BOTTOM_X + 5, BOTTOM_Y + 2, 22f, Color.BLACK),
            Layer(BOTTOM_X + 5, BOTTOM_Y + 2, 19f, silver, gradient = outline),
            Layer(BOTTOM_X, BOTTOM_Y, 17f, navy),
            Layer(BOTTOM_X, BOTTOM_Y, 8f, Color.makeRGB(221, 221, 221)),
            Layer(BOTTOM_X, BOTTOM_Y, 7f, navy, gradient = body),
            Layer(BOTTOM_X, BOTTOM_Y - 3, 7f, silver, PaintMode.FILL, gloss)
        )
        return drawLayers(bottom, botTypeface!!, layers)
    }

    /**
     * 绘制文本
     *
     * @param text 文本
     * @param typeface 字体
     * @param layers 各层文本
     * @return 文本宽度
     */
    private fun Canvas.drawLayers(
        text: String,
        typeface: Typeface,
        layers: List<Layer>
    ): Float {
        val font = Font(typeface, SIZE)
        val paint = Paint().apply {
            strokeJoin = PaintStrokeJoin.ROUND
            strokeCap = PaintStrokeCap.ROUND
        }
        save()
        skew(-0.45f, 0f)
        layers.forEach { layer ->
            paint.shader?.close()
            paint.shader = layer.gradient?.toShader()
            paint.color = layer.color
            paint.strokeWidth = layer.strokeWidth
            paint.mode = layer.mode
            drawString(text, layer.x, layer.y, font, paint)
        }
        val width = font.measureTextWidth(text)
        restore()
        font.close()
        paint.close()
        return width
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
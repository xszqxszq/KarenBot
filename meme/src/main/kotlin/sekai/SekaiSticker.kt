package xyz.xszq.bot.sekai

import kotlinx.serialization.json.Json
import org.jetbrains.skia.Color
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.paragraph.Alignment
import org.jetbrains.skia.paragraph.FontCollection
import org.jetbrains.skia.paragraph.Paragraph
import org.jetbrains.skia.paragraph.ParagraphBuilder
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.TextStyle
import org.jetbrains.skia.paragraph.TypefaceFontProvider
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class SekaiSticker {
    val imgDir = File(ASSETS_DIR)
    lateinit var fontCollection: FontCollection

    val characters = Json.decodeFromString<List<SekaiCharacter>>(
        File(imgDir, "characters.json").readText()
    )

    fun init() {
        val provider = TypefaceFontProvider()
        fontCollection = FontCollection().apply {
            setDefaultFontManager(FontMgr.default)
            setAssetFontManager(provider)
        }
        registerSystemFonts(provider)
    }

    fun draw(
        character: SekaiCharacter,
        text: String,
    ): org.jetbrains.skia.Image {
        val sticker = Surface.makeRasterN32Premul(296, 256)
        val template = org.jetbrains.skia.Image.makeFromEncoded(File(imgDir, character.img).readBytes())
        val ratio = min(296f / template.width, 256f / template.height)
        val width = template.width * ratio
        val height = template.height * ratio
        val x = (296f - width) / 2f
        val y = (256f - height) / 2f

        sticker.canvas.drawImageRect(
            template,
            Rect.makeWH(template.width.toFloat(), template.height.toFloat()),
            Rect.makeXYWH(x, y, width, height)
        )

        val rendered = drawText(character, text)
        val angle = character.defaultText.r / 10f * 57.29578f

        sticker.canvas.save()
        sticker.canvas.translate(character.defaultText.x.toFloat(), character.defaultText.y.toFloat())
        sticker.canvas.rotate(angle)
        val (textX, textY) = calcTextPosition(
            rendered = rendered,
            anchorX = character.defaultText.x.toFloat(),
            anchorY = character.defaultText.y.toFloat(),
            angle = angle
        )
        rendered.stroke.paint(sticker.canvas, textX, textY)
        rendered.fill.paint(sticker.canvas, textX, textY)
        sticker.canvas.restore()

        return sticker.makeImageSnapshot()
    }

    private fun drawText(
        character: SekaiCharacter,
        text: String
    ): RenderedText {
        var size = character.defaultText.s.toFloat()
        val content = text.trim().ifBlank { " " }

        repeat(12) {
            val stroke = drawLine(content, size, true, parseColor(character.color))
            val fill = drawLine(content, size, false, parseColor(character.color))
            if (fill.lineMetrics.size <= 4 && fill.height <= 140f) {
                val firstLineHeight = fill.lineMetrics.firstOrNull()?.height?.toFloat() ?: fill.height
                val totalHeight = fill.lineMetrics.sumOf { it.height }.toFloat().takeIf { it > 0f } ?: fill.height
                return RenderedText(stroke, fill, firstLineHeight, totalHeight)
            }
            size = (size * 0.92f).coerceAtLeast(16f)
        }

        val stroke = drawLine(content, size, true, parseColor(character.color))
        val fill = drawLine(content, size, false, parseColor(character.color))
        val firstLineHeight = fill.lineMetrics.firstOrNull()?.height?.toFloat() ?: fill.height
        val totalHeight = fill.lineMetrics.sumOf { it.height }.toFloat().takeIf { it > 0f } ?: fill.height
        return RenderedText(stroke, fill, firstLineHeight, totalHeight)
    }

    private fun drawLine(
        text: String,
        size: Float,
        stroke: Boolean,
        color: Int
    ): Paragraph {
        val style = ParagraphStyle().apply {
            alignment = Alignment.CENTER
        }
        val textStyle = TextStyle().apply {
            fontSize = size
            fontFamilies = fonts
            height = 0.94f
            this.color = color
            foreground = Paint().apply {
                mode = if (stroke) PaintMode.STROKE else PaintMode.FILL
                this.color = if (stroke) Color.WHITE else color
                strokeWidth = if (stroke) 9f else 0f
                strokeJoin = PaintStrokeJoin.ROUND
                strokeCap = PaintStrokeCap.ROUND
                isAntiAlias = true
            }
        }

        val builder = ParagraphBuilder(style, fontCollection)
        builder.pushStyle(textStyle)
        builder.addText(text)
        return builder.build().also { it.layout(220f) }
    }

    private fun calcTextPosition(
        rendered: RenderedText,
        anchorX: Float,
        anchorY: Float,
        angle: Float
    ): Pair<Float, Float> {
        val width = 220f
        val firstLineHeight = rendered.firstLineHeight + 9f
        val totalHeight = rendered.totalHeight + 9f
        val expandUp = (totalHeight - firstLineHeight).coerceAtLeast(0f)
        val x = -width / 2f
        var y = -firstLineHeight / 2f - expandUp

        val rad = angle / 57.29578f
        val c = cos(rad)
        val s = sin(rad)
        val corners = listOf(
            rotatePoint(x, y, c, s),
            rotatePoint(x + width, y, c, s),
            rotatePoint(x, y + totalHeight, c, s),
            rotatePoint(x + width, y + totalHeight, c, s)
        )
        y += solveYOffset(corners, anchorX, anchorY, c, s)
        return x to y
    }

    private fun solveYOffset(
        corners: List<Pair<Float, Float>>,
        anchorX: Float,
        anchorY: Float,
        cos: Float,
        sin: Float
    ): Float {
        var lower = Float.NEGATIVE_INFINITY
        var upper = Float.POSITIVE_INFINITY

        corners.forEach { (x, y) ->
            updateBounds(anchorX + x, -sin, 8f, 288f) { lo, hi ->
                lower = max(lower, lo)
                upper = min(upper, hi)
            }
            updateBounds(anchorY + y, cos, 8f, 248f) { lo, hi ->
                lower = max(lower, lo)
                upper = min(upper, hi)
            }
        }

        if (lower <= 0f && 0f <= upper)
            return 0f
        if (lower.isFinite() && upper.isFinite() && lower > upper)
            return lower
        return when {
            lower.isFinite() && lower > 0f -> lower
            upper.isFinite() && upper < 0f -> upper
            lower.isFinite() -> lower
            upper.isFinite() -> upper
            else -> 0f
        }
    }

    private fun updateBounds(
        base: Float,
        coeff: Float,
        min: Float,
        max: Float,
        onBounds: (Float, Float) -> Unit
    ) {
        if (abs(coeff) < 0.0001f) {
            if (base < min || base > max)
                onBounds(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
            return
        }

        val a = (min - base) / coeff
        val b = (max - base) / coeff
        onBounds(min(a, b), max(a, b))
    }

    private fun rotatePoint(
        x: Float,
        y: Float,
        cos: Float,
        sin: Float
    ): Pair<Float, Float> = x * cos - y * sin to x * sin + y * cos

    private fun registerSystemFonts(
        provider: TypefaceFontProvider
    ) {
        val mgr = FontMgr.default
        fontAliases.forEach { (target, names) ->
            for (i in 0 until mgr.familiesCount) {
                val family = mgr.getFamilyName(i)
                if (names.none { normalize(it) == normalize(family) })
                    continue
                val styleSet = mgr.makeStyleSet(i) ?: continue
                val hints = fontStyles[target].orEmpty()
                val index = (0 until styleSet.count()).firstOrNull { index ->
                    val styleName = styleSet.getStyleName(index)
                    hints.any { styleName.contains(it, true) }
                } ?: 0
                styleSet.getTypeface(index)?.let {
                    provider.registerTypeface(it, target)
                    break
                }
            }
        }
    }

    private fun parseColor(hex: String): Int {
        val color = hex.removePrefix("#").toInt(16)
        return Color.makeARGB(255, color shr 16 and 0xFF, color shr 8 and 0xFF, color and 0xFF)
    }

    private fun normalize(name: String): String =
        name.lowercase().replace(" ", "").replace("-", "").replace("_", "")

    private data class RenderedText(
        val stroke: Paragraph,
        val fill: Paragraph,
        val firstLineHeight: Float,
        val totalHeight: Float
    )

    companion object {
        private const val ASSETS_DIR = "./data/meme/pjsk/"
        private val fonts = arrayOf(
            "FOT-Yuruka Std UB",
            "SSFangTangTi",
            "Alibaba-PuHuiTi-H",
            "Alibaba-PuHuiTi-B"
        )
        private val fontAliases = mapOf(
            "FOT-Yuruka Std UB" to listOf("FOT-Yuruka Std UB", "FOT-Yuruka Std", "YurukaStd", "Yuruka Std"),
            "SSFangTangTi" to listOf("SSFangTangTi", "ShangShouFangTangTi", "Shang Shou Fang Tang Ti"),
            "Alibaba-PuHuiTi-H" to listOf("Alibaba-PuHuiTi-H", "Alibaba PuHuiTi", "Alibaba PuHuiTi Heavy"),
            "Alibaba-PuHuiTi-B" to listOf("Alibaba-PuHuiTi-B", "Alibaba PuHuiTi", "Alibaba PuHuiTi Bold")
        )
        private val fontStyles = mapOf(
            "FOT-Yuruka Std UB" to listOf("UB", "Ultra", "Heavy", "Bold", "Black"),
            "SSFangTangTi" to listOf("Regular", "Normal"),
            "Alibaba-PuHuiTi-H" to listOf("H", "Heavy", "Bold"),
            "Alibaba-PuHuiTi-B" to listOf("B", "Bold", "Medium")
        )

        val aliases = buildMap {
            put("airi", listOf("airi", "桃井爱莉", "桃井", "爱莉", "桃井愛莉", "愛莉", "momoi"))
            put("akito", listOf("akito", "东云彰人", "東雲彰人", "彰人", "彰人", "akt"))
            put("an", listOf("an", "白石杏", "白石", "杏"))
            put("emu", listOf("emu", "凤绘梦", "鳳えむ", "鳳", "えむ", "凤", "绘梦", "凤笑梦", "笑梦"))
            put("ena", listOf("ena", "东云绘名", "東雲絵名", "絵名", "绘名"))
            put("Haruka", listOf("haruka", "桐谷遥", "桐谷", "遥", "hrk"))
            put("Honami", listOf("honami", "望月穂波", "望月", "穂波", "穗波", "望月穗波", "hnm"))
            put("Ichika", listOf("ichika", "星乃一歌", "星乃", "一歌", "ick"))
            put("KAITO", listOf("kaito", "KAITO", "かいと", "カイト"))
            put("Kanade", listOf("kanade", "宵崎奏", "宵崎", "奏", "knd"))
            put("Kohane", listOf("kohane", "小豆沢心羽", "小豆沢こはね", "小豆沢", "こはね", "小豆泽", "心羽", "khn"))
            put("Len", listOf("len", "镜音连", "鏡音レン", "レン", "连"))
            put("Luka", listOf("luka", "巡音流歌", "巡音ルカ", "巡音", "ルカ", "流歌"))
            put("Mafuyu", listOf("mafuyu", "朝比奈真冬", "朝比奈まふゆ", "朝比奈", "まふゆ", "真冬", "mfy"))
            put("Meiko", listOf("meiko", "MEIKO", "めいこ", "メイコ", "起音"))
            put("Miku", listOf("miku", "初音未来", "初音", "初音ミク", "ミク"))
            put("Minori", listOf("minori", "花里实乃里", "花里みのり", "花里", "みのり", "实乃里", "mnr"))
            put("Mizuki", listOf("mizuki", "晓山瑞希", "暁山瑞希", "暁山", "瑞希", "晓山", "mzk"))
            put("Nene", listOf("nene", "草薙宁宁", "草薙寧々", "草薙", "寧々", "宁宁"))
            put("Rin", listOf("rin", "镜音铃", "鏡音リン", "リン", "铃"))
            put("Rui", listOf("rui", "神代类", "神代類", "神代", "類", "类"))
            put("Saki", listOf("saki", "天马咲希", "天馬咲希", "咲希"))
            put("Shiho", listOf("shiho", "日野森志步", "日野森志歩", "志歩", "志步"))
            put("Shizuku", listOf("shizuku", "日野森雫", "雫", "szk"))
            put("Touya", listOf("touya", "青柳冬弥", "青柳", "冬弥"))
            put("Tsukasa", listOf("tsukasa", "天马司", "天馬司", "司", "tks"))
        }
    }
}
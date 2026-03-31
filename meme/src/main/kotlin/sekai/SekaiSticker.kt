package xyz.xszq.bot.sekai

import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.NativeImage
import korlibs.image.bitmap.context2d
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.font.Font
import korlibs.image.font.FontRegistry
import korlibs.image.font.SystemFontRegistry
import korlibs.image.font.getTextBoundsWithGlyphs
import korlibs.image.format.readNativeImage
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.math.geom.Angle
import korlibs.math.geom.Point
import korlibs.math.geom.Size
import korlibs.math.geom.radians
import korlibs.math.geom.vector.LineCap
import korlibs.math.geom.vector.LineJoin
import korlibs.math.squared
import korlibs.math.toIntCeil
import korlibs.memory.extract8
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class SekaiSticker {
    val imgDir = localCurrentDirVfs[ASSETS_DIR]
    lateinit var registry: FontRegistry
    lateinit var fonts: List<Font>
    val characters = Json.decodeFromString<List<SekaiCharacter>>(
        File(imgDir["characters.json"].absolutePath).readText()
    )

    suspend fun init() {
        registry = SystemFontRegistry()
        fonts = listOf("FOT-Yuruka Std UB", "SSFangTangTi", "Alibaba-PuHuiTi-H", "Alibaba-PuHuiTi-B").map {
            registry[it]
        }
    }

    suspend fun draw(
        character: SekaiCharacter,
        text: String,
    ): Bitmap {
        val sticker = NativeImage(296, 256)
        val template = imgDir[character.img].readNativeImage()

        var ratio = min(sticker.width.toDouble() / template.width, sticker.height.toDouble() / template.height)
        var templatePos = Point(
            (sticker.width - template.width * ratio) / 2,
            (sticker.height - template.height * ratio) / 2
        )
        val angle = (character.defaultText.r / 10.0).radians

        // 先渲染文本，否则 Stroke 会很丑
        val renderedTexts = text.split("\n").map { line ->
            drawLine(character, line)
        }

        return sticker.context2d {
            drawImage(
                template,
                templatePos,
                Size(template.width * ratio, template.height * ratio)
            )
            save()

            // 计算旋转后的中心偏移
            val reference = renderedTexts.first()
            val (_, offsetY) = calcOffset(reference, angle)

            translate(
                character.defaultText.x,
                character.defaultText.y - offsetY
            )
            rotate(angle)
            var nowY = 0
            renderedTexts.forEach { rendered ->
                val (offsetX, _) = calcOffset(rendered, angle)
                drawImage(
                    rendered,
                    Point(-offsetX, nowY)
                )
                nowY += rendered.height
            }
        }
    }
    fun calcOffset(
        rendered: NativeImage,
        angle: Angle
    ): Pair<Double, Double> {
        val diagonal = sqrt(rendered.width.squared().toDouble() + rendered.height.squared())
        val centerAngle = angle + atan(rendered.height.toDouble() / rendered.width).radians
        val offsetX = diagonal / 2 * centerAngle.cosine()
        val offsetY = diagonal / 2 * centerAngle.sine() + STROKE * 2
        return Pair(offsetX, offsetY)
    }
    private fun drawLine(
        character: SekaiCharacter,
        text: String
    ): NativeImage {
        var textHeight = 0.0
        var ascent = 0.0
        val chars = mutableListOf<Triple<Char, Font, Double>>()
        text.forEach { char ->
            val (font, metrics) = fonts.firstNotNullOfOrNull { font ->
                val metrics = font.getTextBoundsWithGlyphs(character.defaultText.s.toDouble(), char.toString())
                if (char.toString().isBlank() || metrics.glyphs.firstOrNull()?.metrics?.existing == true)
                    Pair(font, metrics)
                else
                    null
            } ?: return@forEach
            val width =
                if (metrics.metrics.width == 0.0) metrics.glyphs.first().metrics.xadvance
                else metrics.metrics.width
            chars.add(Triple(char, font, width))
            textHeight = max(textHeight, metrics.metrics.ascent - metrics.metrics.descent)
            ascent = max(ascent, metrics.metrics.ascent)
        }
        ascent += STROKE
        val textWidth = chars.sumOf { it.third }
        return NativeImage(
            (textWidth + STROKE * 2).toIntCeil(),
            (textHeight + STROKE * 2).toIntCeil()
        ).context2d {
            fontSize = character.defaultText.s.toDouble()
            lineWidth = STROKE
            lineCap = LineCap.ROUND
            lineJoin = LineJoin.ROUND

            var x = STROKE
            chars.forEach { (char, selected, width) ->
                font = selected
                strokeStyle = Colors.WHITE
                strokeText(char.toString(), Point(x, ascent))
                x += width
            }
            x = STROKE
            chars.forEach { (char, selected, width) ->
                font = selected
                fillStyle = character.color.hexToRGBA()
                fillText(char.toString(), Point(x, ascent))
                x += width
            }
        }
    }

    private fun String.hexToRGBA(): RGBA {
        val int = substring(1).toInt(16)
        return RGBA.Companion.invoke(
            int.extract8(16),
            int.extract8(8),
            int.extract8(0)
        )
    }
    companion object {
        private const val ASSETS_DIR = "./data/meme/pjsk/"
        private const val STROKE = 9.0
        val aliases = buildMap {
            put("airi", listOf("airi", "桃井爱莉", "桃井", "爱莉", "桃井愛莉", "愛莉", "momoi"))
            put("akito", listOf("akito", "東雲彰人", "彰人", "东云彰人", "彰人", "akt"))
            put("an", listOf("an", "白石杏", "白石", "杏"))
            put("emu", listOf("emu", "鳳えむ", "鳳", "えむ", "凤绘梦", "凤", "绘梦", "凤笑梦", "笑梦"))
            put("ena", listOf("ena", "東雲絵名", "絵名", "东云绘名", "绘名"))
            put("Haruka", listOf("haruka", "桐谷遥", "桐谷", "遥", "hrk"))
            put("Honami", listOf("honami", "望月穂波", "望月", "穂波", "穗波", "望月穗波", "hnm"))
            put("Ichika", listOf("ichika", "星乃一歌", "星乃", "一歌", "ick"))
            put("KAITO", listOf("kaito", "かいと", "カイト"))
            put("Kanade", listOf("kanade", "宵崎奏", "宵崎", "奏", "knd"))
            put("Kohane", listOf("kohane", "小豆沢こはね", "小豆沢", "こはね", "小豆泽", "小豆沢心羽", "心羽", "khn"))
            put("Len", listOf("len", "鏡音レン", "镜音连", "レン", "连"))
            put("Luka", listOf("luka", "巡音ルカ", "巡音流歌", "巡音", "ルカ", "流歌"))
            put("Mafuyu", listOf("mafuyu", "朝比奈まふゆ", "朝比奈", "まふゆ", "朝比奈真冬", "真冬", "mfy"))
            put("Meiko", listOf("meiko", "めいこ", "メイコ", "起音"))
            put("Miku", listOf("miku", "初音", "初音未来", "初音ミク", "ミク"))
            put("Minori", listOf("minori", "花里みのり", "花里", "みのり", "实乃里", "花里实乃里", "mnr"))
            put("Mizuki", listOf("mizuki", "暁山瑞希", "暁山", "瑞希", "晓山瑞希", "晓山", "mzk"))
            put("Nene", listOf("nene", "草薙寧々", "草薙", "寧々", "草薙宁宁", "宁宁"))
            put("Rin", listOf("rin", "鏡音リン", "镜音铃", "リン", "铃"))
            put("Rui", listOf("rui", "神代類", "神代", "類", "神代类", "类"))
            put("Saki", listOf("saki", "天馬咲希", "天马咲希", "咲希"))
            put("Shiho", listOf("shiho", "日野森志歩", "志歩", "志步", "日野森志步"))
            put("Shizuku", listOf("shizuku", "日野森雫", "雫", "szk"))
            put("Touya", listOf("touya", "青柳冬弥", "青柳", "冬弥"))
            put("Tsukasa", listOf("tsukasa", "天馬司", "天马司", "司", "tks"))
        }
    }
}
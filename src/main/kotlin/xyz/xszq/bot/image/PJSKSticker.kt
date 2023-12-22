package xyz.xszq.bot.image

import korlibs.image.bitmap.NativeImage
import korlibs.image.color.Colors
import korlibs.image.format.readNativeImage
import korlibs.image.text.HorizontalAlign
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.math.geom.Point
import korlibs.math.geom.Size
import korlibs.math.geom.SizeInt
import korlibs.math.geom.radians
import xyz.xszq.bot.config.PJSKCharacter
import xyz.xszq.bot.config.PJSKConfig
import xyz.xszq.nereides.hexToRGBA
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

object PJSKSticker {
    lateinit var config: PJSKConfig
    private val imgDir = localCurrentDirVfs["image/pjsk"]
    suspend fun draw(character: PJSKCharacter, text: String): BuildImage {
        val img = imgDir[character.img].readNativeImage()
        val bg = NativeImage(296, 256)
        val ratio = (1.0 * bg.width / img.width).coerceAtMost(1.0 * bg.height / img.height)

        val a = (character.defaultText.r.toDouble().absoluteValue / 10).radians
        val sinA = sin(a.radians)
        val cosA = cos(a.radians)
        val newW = (1.0 * bg.width / (cosA + 1.0 * bg.height / 2.0 / bg.width * sinA)).toInt()
        val newH = (1.0 * bg.height * newW / bg.width / 2.0).toInt()
        val textImage = BuildImage.new("RGBA", SizeInt(newW, newH)).drawText(
            xy = listOf(0, 0, newW, newH),
            text = text,
            maxFontSize = character.defaultText.s,
            allowWrap = text.length > 16,
            fill = character.color.hexToRGBA(),
            spacing = 11,
            hAlign = HorizontalAlign.LEFT,
            linesAlign = HorizontalAlign.CENTER,
            strokeWidth = 9,
            strokeFill = Colors.WHITE,
            fontName = "FOT-Yuruka Std UB",
            fallbackFonts = listOf("SSFangTangTi")
        ).rotate(
            -(character.defaultText.r.toDouble() / 10).radians.degrees.toDouble(),
            expand = true
        ).trim()
        return BuildImage(bg.modify {
            drawImage(
                img,
                Point((bg.width - img.width * ratio) / 2, (bg.height - img.height * ratio) / 2),
                Size(img.width * ratio, img.height * ratio)
            )
        }).paste(
            textImage,
            Point(
                character.defaultText.x - textImage.width / 2,
                character.defaultText.y - character.defaultText.s
            )
        )
    }
    val aliases = buildMap {
        put("airi", listOf("airi", "桃井爱莉", "桃井", "爱莉", "桃井愛莉", "愛莉", "momoi"))
        put("akito", listOf("akito", "東雲彰人", "東雲", "彰人", "东云彰人", "彰人", "akt"))
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
        put("Saki", listOf("saki", "天馬咲希", "咲希"))
        put("Shiho", listOf("shiho", "日野森志歩", "志歩", "志步", "日野森志步"))
        put("Shizuku", listOf("shizuku", "日野森雫", "雫", "szk"))
        put("Touya", listOf("touya", "青柳冬弥", "青柳", "冬弥"))
        put("Tsukasa", listOf("tsukasa", "天馬司", "司", "tks"))
    }
}
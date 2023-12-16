@file:Suppress("unused", "UNCHECKED_CAST")

package xyz.xszq.bot.image

import com.sksamuel.scrimage.filter.BrightnessFilter
import com.sksamuel.scrimage.filter.InvertFilter
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.NativeImage
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.format.showImageAndWait
import korlibs.image.text.HorizontalAlign
import korlibs.image.text.VerticalAlign
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.math.geom.Point
import korlibs.math.geom.SizeInt
import korlibs.math.geom.degrees
import xyz.xszq.bot.text.LibreTranslate
import xyz.xszq.nereides.hexToRGBA
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.*
import kotlin.random.Random
import kotlin.reflect.full.declaredMemberProperties

typealias Images = List<BuildImage>
typealias Args = List<String>
typealias Maker = suspend (Images, Args)-> ByteArray
object MemeGenerator {
    private val imgDir = localCurrentDirVfs["image/meme"]
    suspend fun handle(
        type: String,
        args: List<String> = listOf(),
        images: List<BuildImage>
    ): ByteArray {
        MemeGenerator::class.declaredMemberProperties.forEach { func ->
            func.annotations.filterIsInstance<Meme>().firstOrNull()?.let { settings ->
                val commands = mutableListOf(settings.command)
                if (settings.aliases.isNotBlank())
                    commands.addAll(settings.aliases.split(','))
                if (type !in commands)
                    return@let
                return (func.getter.call(this) as Maker).invoke(images, args)
            }
        }
        throw UnsupportedOperationException()
    }

    fun getList() = buildString {
        MemeGenerator::class.declaredMemberProperties.forEach { func ->
            func.annotations.filterIsInstance<Meme>().firstOrNull()?.let { settings ->
                append(" ")
                append(settings.command)
            }
        }
    }

    fun getHelpText(type: String): String {
        MemeGenerator::class.declaredMemberProperties.forEach { func ->
            func.annotations.filterIsInstance<Meme>().firstOrNull()?.let { settings ->
                val commands = mutableListOf(settings.command)
                if (settings.aliases.isNotBlank())
                    commands.addAll(settings.aliases.split(','))
                if (type !in commands)
                    return@let
                return buildString {
                    appendLine("用法：/生成 ${settings.command}")
                    if (settings.aliases.isNotEmpty())
                        appendLine("别名：${settings.aliases}")
                    if (settings.help.isNotEmpty())
                        appendLine(settings.help)
                }
            }
        }
        return ""
    }

    private suspend fun alwaysNormal(img: BuildImage): ByteArray {
        return makeJpgOrGif(img) {
            val imgBig = img.resizeWidth(500)
            val imgSmall = img.resizeWidth(100)
            val h1 = imgBig.height
            val h2 = max(imgSmall.height, 80)
            val frame = BuildImage.new("RGBA", SizeInt(500, h1 + h2 + 10), Colors.WHITE)
            frame.paste(imgBig, alpha = true).paste(
                imgSmall, Point(290, h1 + 5 + (h2 - imgSmall.height) / 2), alpha = true
            )
            frame.drawText(
                listOf(20, h1 + 5, 280, h1 + h2 + 5),
                "要我一直", hAlign = HorizontalAlign.RIGHT, maxFontSize = 60
            )
            frame.drawText(
                listOf(400, h1 + 5, 480, h1 + h2 + 5),
                "吗", hAlign = HorizontalAlign.LEFT, maxFontSize = 60
            )
            frame
        }
    }

    private suspend fun alwaysAlways(img: BuildImage, loop: Boolean = false): ByteArray {
        val tmpImg = img.resizeWidth(500)
        val imgH = tmpImg.height
        val textH = max(tmpImg.heightIfResized(100) + tmpImg.heightIfResized(20) + 10, 80)
        val frameH = imgH + textH
        val textFrame = BuildImage.new("RGBA", SizeInt(500, frameH), Colors.WHITE)
        textFrame.drawText(
            listOf(0, imgH, 280, frameH),
            "要我一直",
            hAlign = HorizontalAlign.LEFT,
            maxFontSize = 60
        ).drawText(
            listOf(400, imgH, 500, frameH),
            "吗",
            hAlign = HorizontalAlign.LEFT,
            maxFontSize = 60
        )
        val frameNum = 20
        val coeff = 5.0.pow(1.0 / frameNum)
        val maker: suspend BuildImage.(Int) -> BuildImage = { i ->
            val now = resizeWidth(500)
            val baseFrame = textFrame.copy().paste(now, alpha = true)
            val frame = BuildImage.new("RGBA", baseFrame.size, Colors.WHITE)
            var r = coeff.pow(i)
            repeat(4) {
                val x = (358 * (1 - r)).roundToInt()
                val y = (frameH * (1 - r)).roundToInt()
                val w = (500 * r).roundToInt()
                val h = (frameH * r).roundToInt()
                frame.paste(baseFrame.resize(SizeInt(w, h)), Point(x, y))
                r /= 5
            }
            frame
        }
        if (!loop)
            return makeJpgOrGif(img) { maker(0) }
        return makeGifOrCombinedGif(img, frameNum, 0.1, FrameAlignPolicy.ExtendLoop, maker = maker)
    }

    class AlwaysArgs(parser: ArgParser) {
        val mode by parser.storing("生成模式").default("normal")
    }

    @Meme("添乱", "给社会添乱")
    val addChaos: Maker = { images, _ ->
        val banner = BuildImage.open(imgDir["add_chaos/0.png"])
        makeJpgOrGif(images[0]) {
            resizeWidth(240).paste(banner)
        }
    }

    @Meme("上瘾", "毒瘾发作")
    val addiction: Maker = { images, texts ->
        var frame = BuildImage.open(imgDir["addiction/0.png"])
        if (texts.isNotEmpty()) {
            val text = texts[0]
            frame = frame.resizeCanvas(
                SizeInt(246, 286), direction = BuildImage.DirectionType.North, bgColor = Colors.WHITE
            )
            kotlin.runCatching {
                frame.drawText(
                    listOf(10, 246, 236, 286), text, maxFontSize = 45
                )
            }.onFailure {
                throw TextOverLengthException()
            }
        }
        makeJpgOrGif(images[0]) {
            frame.copy().paste(resize(SizeInt(91, 91), keepRatio = true), alpha = true)
        }
    }

    @Meme("一样")
    val alike: Maker = { images, _ ->
        val frame = BuildImage.new("RGBA", SizeInt(470, 180), Colors.WHITE)
        frame.drawText(
            listOf(10, 10, 185, 140), "你怎么跟", maxFontSize = 40, minFontSize = 30, hAlign = HorizontalAlign.RIGHT
        ).drawText(
            listOf(365, 10, 460, 140), "一样", maxFontSize = 40, minFontSize = 30, hAlign = HorizontalAlign.LEFT
        )
        makeJpgOrGif(images[0]) {
            frame.copy().paste(resize(SizeInt(150, 150), keepRatio = true), Point(200, 15), alpha = true)
        }
    }

    @Meme("要我一直", help = "可以使用“--mode 模式”指定以下模式：\n\tnormal，普通模式\n\tcircle 套娃\n\tloop 套娃循环GIF")
    val always: Maker = { images, texts ->
        val args = ArgParser(texts.toTypedArray()).parseInto(::AlwaysArgs)
        val img = images[0]
        when (args.mode) {
            "normal" -> alwaysNormal(img)
            "circle" -> alwaysAlways(img, false)
            else -> alwaysAlways(img, true)
        }
    }
    @Meme(
        "我永远喜欢",
        help = "需要附带一对或多对文字+图片，且文字数量需要与图片相等\n\t例：/生成 我永远喜欢 心爱 [图片]\n\t例：/生成 我永远喜欢 心爱 虹夏 [图片] [图片]"
    )
    val alwaysLike: Maker = { images, texts ->
        if (texts.isEmpty() || texts.first().isBlank()) {
            throw TextOrNameNotEnoughException()
        }
        val img = images.first()
        val name = texts.first()
        val text = "我永远喜欢$name"

        val frame = BuildImage.open(imgDir["always_like/0.png"])
        frame.paste(img.resize(SizeInt(350, 400), keepRatio = true, inside = true), Point(25, 35), alpha = true)
        kotlin.runCatching {
            frame.drawText(
                listOf(20, 470, frame.width - 20, 570),
                text,
                maxFontSize = 70,
                minFontSize = 30,
                fontName = "Glow Sans SC Normal Bold"
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        val randomColor: () -> RGBA = {
            listOf(
                Colors.RED, Colors.DARKORANGE, Colors.GOLD, Colors.DARKGREEN, Colors.BLUE, Colors.CYAN, Colors.PURPLE
            ).random()
        }
        if (images.size > 1) {
            var textW = Text2Image.fromText(text, 70).width
            val ratio = min((frame.width - 40) / textW, 1.0F)
            textW *= ratio
            val nameW = Text2Image.fromText(name, 70).width * ratio
            val startW = textW - nameW + (frame.width - textW) / 2.0
            frame.drawLine(
                listOf(startW, 525.0, startW + nameW, 525.0),
                fill = randomColor(),
                width = 10.0
            )
        }

        var currentH = 400
        images.mapIndexed { index, memeBuilder -> Pair(memeBuilder, texts[index]) }.subList(1, images.size)
            .forEachIndexed { index, (image, name) ->
                val i = index + 1
                frame.paste(
                    image.resize(SizeInt(350, 400), keepRatio = true, inside = true),
                    Point(10 + Random.nextInt(0, 50), 20 + Random.nextInt(0, 70)),
                    alpha = true
                )
                kotlin.runCatching {
                    frame.drawText(
                        listOf(400, currentH, frame.width - 20, currentH + 80),
                        name,
                        maxFontSize = 70,
                        minFontSize = 30,
                        fontName = "Glow Sans SC Normal Bold"
                    )
                }.onFailure {
                    throw TextOverLengthException()
                }
                if (images.size > i + 1) {
                    val nameW = min(Text2Image.fromText(name, 70).width, 380.0F)
                    val startW = 400 + (410 - nameW) / 2.0
                    val lineH = currentH + 40
                    frame.drawLine(
                        listOf(startW, lineH.toDouble(), startW + nameW, lineH.toDouble()),
                        fill = randomColor(),
                        width = 10.0,
                    )
                }
                currentH -= 70
            }
        frame.saveJpg()

    }

    @Meme("防诱拐")
    val antiKidnap: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(SizeInt(450, 450), keepRatio = true)
        val frame = BuildImage.open(imgDir["anti_kidnap/0.png"])
        frame.paste(img, Point(30, 78), below = true)
        frame.saveJpg()
    }

    @Meme("阿尼亚喜欢", help = "需要附带名字和图片")
    val anyaSuki: Maker = { images, texts ->
        val text = texts.ifBlank { "阿尼亚喜欢这个" }
        val frame = BuildImage.open(imgDir["anya_suki/0.png"])
        kotlin.runCatching {
            frame.drawText(
                listOf(5, frame.height - 60, frame.width - 5, frame.height - 10),
                text,
                maxFontSize = 40,
                fill = Colors.WHITE,
                strokeFill = Colors.BLACK,
                strokeRatio = 0.06
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resize(SizeInt(305, 235), keepRatio = true)
            frame.copy().paste(img, Point(106, 72), below = true)
        }
    }

    @Meme("鼓掌")
    val applaud: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(SizeInt(110, 110))
        val frames = mutableListOf<Bitmap>()
        val locs = listOf(
            listOf(109, 102, 27, 17),
            listOf(107, 105, 28, 15),
            listOf(110, 106, 27, 14),
            listOf(109, 106, 27, 14),
            listOf(107, 108, 29, 12),
        )
        (0 until 5).forEach {
            val frame = BuildImage.open(imgDir["applaud/$it.png"])
            val (w, h, x, y) = locs[it]
            frame.paste(img.resize(SizeInt(w, h)), Point(x, y), below = true)
            frames.add(frame.image)
        }
        saveGif(frames, 0.1)
    }

    @Meme("升天")
    val ascension: Maker = { _, rawTexts ->
        val arg = rawTexts.ifBlank { "学的是机械" }
        val frame = BuildImage.open(imgDir["ascension/0.png"])
        val text = "你原本应该要去地狱的，但因为你生前$arg，我们就当作你已经服完刑期了"
        kotlin.runCatching {
            frame.drawText(
                listOf(40, 30, 482, 135),
                text,
                allowWrap = true,
                maxFontSize = 50,
                minFontSize = 20,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.saveJpg()
    }
    @Meme("土豆")
    val potato: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["potato/0.png"])
        val img = images[0].convert("RGBA").square().resize(SizeInt(458, 458))
        frame.paste(img.rotate(-5.0), Point(531, 15), below = true)
        frame.saveJpg()
    }

    @Meme("抱紧")
    val holdTight: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(SizeInt(159, 171), keepRatio = true)
        val frame = BuildImage.open(imgDir["hold_tight/0.png"])
        frame.paste(img, Point(113, 205), below = true)
        frame.saveJpg()
    }

    @Meme("离婚协议", "离婚申请")
    val divorce: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["divorce/0.png"])
        val img = images[0].convert("RGBA").resize(frame.size, keepRatio = true)
        frame.paste(img, below = true)
        frame.saveJpg()
    }

    @Meme("完美")
    val perfect: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["perfect/0.png"])
        val img = images[0].convert("RGBA").resize(SizeInt(310, 460), keepRatio = true, inside = true)
        frame.paste(img, Point(313, 64), alpha = true)
        frame.saveJpg()
    }

    @Meme("无响应")
    val noResponse: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(SizeInt(1050, 783), keepRatio = true)
        val frame = BuildImage.open(imgDir["no_response/0.png"])
        frame.paste(img, Point(0, 581), below = true)
        frame.saveJpg()
    }

    @Meme("像样的亲亲")
    val decentKiss: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(SizeInt(589, 340), keepRatio = true)
        val frame = BuildImage.open(imgDir["decent_kiss/0.png"])
        frame.paste(img, Point(0, 91), below = true)
        frame.saveJpg()
    }

    @Meme("凯露指")
    val karylPoint: Maker = { images, _ ->
        val img = images[0].convert("RGBA").rotate(7.5, expand = true).resize(SizeInt(225, 225))
        val frame = BuildImage.open(imgDir["karyl_point/0.png"])
        frame.paste(img, Point(87, 790), alpha = true)
        frame.savePng()
    }

    @Meme("这像画吗")
    val paint: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(SizeInt(117, 135), keepRatio = true)
        val frame = BuildImage.open(imgDir["paint/0.png"])
        frame.paste(img.rotate(4.0, expand = true), Point(95, 107), below = true)
        frame.saveJpg()
    }

    @Meme("为什么@我")
    val whyAtMe: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(SizeInt(265, 265), keepRatio = true)
        val frame = BuildImage.open(imgDir["why_at_me/0.png"])
        frame.paste(img.rotate(19.0), Point(42, 13), below = true)
        frame.saveJpg()
    }

    @Meme("精神支柱")
    val support: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(SizeInt(815, 815)).rotate(23.0, expand = true)
        val frame = BuildImage.open(imgDir["support/0.png"])
        frame.paste(img, Point(-172, -17), below = true)
        frame.saveJpg()
    }

    @Meme("加班")
    val overtime: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["overtime/0.png"])
        val img = images[0].convert("RGBA").resize(SizeInt(250, 250), keepRatio = true)
        frame.paste(img.rotate(-25.0, expand = true), Point(165, 220), below = true)
        frame.saveJpg()
    }

    @Meme("小画家")
    val painter: Maker = { images, _ ->
        val img = images[0].convert("RGBA")
            .resize(SizeInt(240, 345), keepRatio = true, direction = BuildImage.DirectionType.North)
        val frame = BuildImage.open(imgDir["painter/0.png"])
        frame.paste(img, Point(125, 91), below = true)
        frame.saveJpg()
    }

    @Meme("捂脸")
    val coverFace: Maker = { images, _ ->
        val points = listOf(Point(15, 15), Point(448, 0), Point(445, 456), Point(0, 465))
        val img = images[0].convert("RGBA").square().resize(SizeInt(450, 450)).perspective(points)
        val frame = BuildImage.open(imgDir["cover_face/0.png"])
        frame.paste(img, Point(120, 150), below = true)
        frame.saveJpg()
    }

    @Meme("木鱼")
    val woodenFish: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(SizeInt(85, 85))
        val frames = (0 until 66).map { i ->
            BuildImage.open(imgDir["wooden_fish/$i.png"]).paste(img, Point(116, 153), below = true).image
        }
        saveGif(frames, 0.1)
    }

    @Meme("丢", "扔")
    val throwBall: Maker = { images, _ ->
        val img = images[0]
            .convert("RGBA")
            .circle()
            .rotate(Random.nextInt(1, 360).toDouble())
            .resize(SizeInt(143, 143))
        val frame = BuildImage.open(imgDir["throw/0.png"])
        frame.paste(img, Point(15, 178), alpha = true)
        frame.saveJpg()
    }

    @Meme("怒撕")
    val ripAngrily: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(SizeInt(105, 105))
        val frame = BuildImage.open(imgDir["rip_angrily/0.png"])
        frame.paste(img.rotate(-24.0, expand = true), Point(18, 170), below = true)
        frame.paste(img.rotate(24.0, expand = true), Point(163, 65), below = true)
        frame.saveJpg()
    }

    @Meme("继续干活", "打工人")
    val backToWork: Maker = { images, _ ->
        val img = images[0].convert("RGBA")
            .resize(SizeInt(220, 310), keepRatio = true, direction = BuildImage.DirectionType.North)
        val frame = BuildImage.open(imgDir["back_to_work/0.png"])
        frame.paste(img.rotate(25.0, expand = true), Point(56, 32), below = true)
        frame.saveJpg()
    }

    @Meme("嘲讽")
    val taunt: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["taunt/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").square().resize(SizeInt(230, 230))
            frame.copy().paste(img, Point(245, 245))
        }
    }

    @Meme("吃")
    val eat: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(SizeInt(34, 34))
        val frames = (0 until 3).map { i ->
            BuildImage.open(imgDir["eat/$i.png"]).paste(img, Point(2, 38), below = true).image
        }
        saveGif(frames, 0.05)
    }

    @Meme("白天黑夜", "白天晚上", help = "需要两张图片")
    val dayNight: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(SizeInt(333, 360), keepRatio = true)
        val img1 = images[1].convert("RGBA").resize(SizeInt(333, 360), keepRatio = true)
        val frame = BuildImage.open(imgDir["daynight/0.png"])
        frame.paste(img, Point(349, 0))
        frame.paste(img1, Point(349, 361))
        frame.saveJpg()
    }

    @Meme("需要", "你可能需要")
    val need: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["need/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").square().resize(SizeInt(115, 115))
            frame.copy().paste(img, Point(327, 232), below = true)
        }
    }

    @Meme("想什么")
    val thinkWhat: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["think_what/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resize(SizeInt(534, 493), keepRatio = true)
            frame.copy().paste(img, Point(530, 0), below = true)
        }
    }

    @Meme("胡桃平板")
    val walnutPad: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["walnut_pad/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resize(SizeInt(540, 360), keepRatio = true)
            frame.copy().paste(img, Point(368, 65), below = true)
        }
    }

    @Meme("恐龙", "小恐龙")
    val dinosaur: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["dinosaur/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resize(SizeInt(680, 578), keepRatio = true)
            frame.copy().paste(img, Point(294, 369), below = true)
        }
    }

    @Meme("震惊")
    val shock: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(SizeInt(300, 300))
        val frames = (0 until 30).map { _ ->
            NativeImage(300, 300).modify {
                fillStyle = Colors.WHITE
                fillRect(0.0, 0.0, 300.0, 300.0)
                drawImage(
                    img.motionBlur(Random.nextInt(-90, 90).toDouble(), Random.nextInt(0, 50))
                        .rotate(Random.nextInt(-20, 20).toDouble()).image, Point(0, 0)
                )
            }
        }
        saveGif(frames, 0.01)
    }

    @Meme("不要靠近")
    val dontGoNear: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["dont_go_near/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resize(SizeInt(170, 170), keepRatio = true)
            frame.copy().paste(img, Point(23, 231), alpha = true)
        }
    }

    @Meme("高血压")
    val bloodPressure: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["blood_pressure/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resize(SizeInt(414, 450), keepRatio = true)
            frame.copy().paste(img, Point(16, 17), below = true)
        }
    }

    @Meme("旅行伙伴加入")
    val maimaiJoin: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["maimai_join/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").square().resize(SizeInt(400, 400))
            frame.copy().paste(img, Point(50, 50), alpha = true, below = true)
        }
    }

    @Meme("捏", "捏脸")
    val pinch: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["pinch/0.png"])
        makeJpgOrGif(images[0]) {
            frame.paste(
                convert("RGBA").resize(SizeInt(1800, 1440), keepRatio = true),
                Point(1080, 0), below = true
            )
        }
    }

    @Meme("啾啾")
    val jiujiu: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(SizeInt(75, 51), keepRatio = true)
        val frames = (0 until 8).map { i ->
            BuildImage.open(imgDir["jiujiu/$i.png"]).paste(img, below = true).image
        }
        saveGif(frames, 0.06)
    }

    @Meme("转")
    val turn: Maker = { images, _ ->
        val img = images[0].convert("RGBA").circle()
        var frames = (0 until 360 step 10).map { i ->
            val frame = BuildImage.new("RGBA", SizeInt(250, 250), Colors.WHITE)
            frame.paste(img.rotate(i.toDouble()).resize(SizeInt(250, 250)), alpha = true)
            frame.image
        }
        if (Random.nextInt(0, 2) == 1)
            frames = frames.reversed()
        saveGif(frames, 0.05)
    }

    @Meme("我想上的")
    val whatIWantToDo: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["what_I_want_to_do/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").circle().resize(SizeInt(270, 270))
            frame.copy().paste(img, Point(350, 590), alpha = true)
        }
    }

    @Meme("遇到困难请拨打", help = "需要两张图片代表 1 和 0")
    val call110: Maker = { images, _ ->
        val img1 = images[0].convert("RGBA").square().resize(SizeInt(250, 250))
        val img0 = images[1].convert("RGBA").square().resize(SizeInt(250, 250))

        val frame = BuildImage.new("RGB", SizeInt(900, 500), Colors.WHITE)
        frame.drawText(listOf(0, 0, 900, 200), "遇到困难请拨打", maxFontSize = 100)
        frame.paste(img1, Point(50, 200), alpha = true)
        frame.paste(img1, Point(325, 200), alpha = true)
        frame.paste(img0, Point(600, 200), alpha = true)

        frame.saveJpg()
    }

    @Meme("一巴掌", help = "需要文本")
    val slap: Maker = { _, texts ->
        val text = texts.first()
        val frame = BuildImage.open(imgDir["slap/0.jpg"])

        kotlin.runCatching {
            frame.drawText(
                listOf(20, 450, 620, 630),
                text,
                allowWrap = true,
                maxFontSize = 110,
                minFontSize = 50
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.saveJpg()
    }

    @Meme("旅行伙伴觉醒")
    val maimaiAwaken: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["maimai_awaken/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").square().resize(SizeInt(250, 250)).rotate(-25.0, expand = true)
            frame.copy().paste(img, Point(134, 134), alpha = true, below = true)
        }
    }

    @Meme("注意力涣散")
    val distracted: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["distracted/1.png"])
        val label = BuildImage.open(imgDir["distracted/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").square().resize(SizeInt(500, 500))
            frame.copy().paste(img, below = true).paste(label, Point(140, 320), alpha = true)
        }
    }

    @Meme("上坟", "坟前比耶")
    val tombYeah: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["tomb_yeah/0.jpg"])
        frame.paste(
            images[0].convert("RGBA").circle().resize(SizeInt(145, 145)), Point(138, 265), alpha = true
        )
        if (images.size > 1) {
            frame.paste(
                images[1].convert("RGBA").circle().rotate(30.0).resize(SizeInt(145, 145)),
                Point(371, 312), alpha = true
            )
        }
        frame.saveJpg()
    }

    @Meme("砸")
    val smash: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["smash/0.png"])
        makeJpgOrGif(images[0]) {
            val points = listOf(Point(1, 237), Point(826, 1), Point(832, 508), Point(160, 732))
            val screen = convert("RGBA").resize(SizeInt(800, 500), keepRatio = true).perspective(points)
            frame.copy().paste(screen, Point(-136, -81), below = true)
        }
    }

    @Meme("低语", help = "需要文本")
    val murmur: Maker = { _, texts ->
        val text = texts[0]
        val frame = BuildImage.open(imgDir["murmur/0.jpg"])
        kotlin.runCatching {
            frame.drawText(
                listOf(10, 255, 430, 300),
                text,
                maxFontSize = 40,
                minFontSize = 15
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.saveJpg()
    }

    @Meme("听音乐")
    val listenMusic: Maker = { images, _ ->
        val img = images[0].convert("RGBA")
        val frame = BuildImage.open(imgDir["listen_music/0.png"])
        val frames = (0 until 360 step 10).map { i ->
            frame.copy().paste(
                img.rotate(-i.toDouble())
                    .resize(SizeInt(215, 215)), Point(100, 100), below = true
            ).image
        }
        saveGif(frames, 0.05)
    }

    @Meme("恍惚")
    val trance: Maker = { images, _ ->
        val img = images[0]
        val width = img.width
        val height = img.height
        val height1 = (1.1 * height).toInt()
        val frame = BuildImage.new("RGB", SizeInt(width, height1), Colors.WHITE)
        frame.paste(img, Point(0, (height * 0.1).toInt()))
        ((height * 0.1).toInt() downTo 1).forEach { i ->
            frame.paste(img.image.alpha(16), Point(0, i), alpha = true)
        }
        ((height * 0.1).toInt() downTo (height * 0.1 * 2).toInt() + 1).forEach { i ->
            frame.paste(img.image.alpha(16), Point(0, i), alpha = true)
        }
        frame.saveJpg()
    }

    @Meme("管人痴")
    val dogOfVtb: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["dog_of_vtb/0.png"])
        makeJpgOrGif(images[0]) {
            val points = listOf(Point(0, 0), Point(579, 0), Point(584, 430), Point(5, 440))
            val img = convert("RGBA").resize(SizeInt(600, 450), keepRatio = true)
            frame.copy().paste(img.perspective(points), Point(97, 32), below = true)
        }
    }

    @Meme("舔", "舔屏,prpr")
    val prpr: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["prpr/0.png"])

        makeJpgOrGif(images[0]) {
            val points = listOf(Point(0, 19), Point(236, 0), Point(287, 264), Point(66, 351))
            val screen = convert("RGBA").resize(SizeInt(330, 330), keepRatio = true).perspective(points)
            frame.copy().paste(screen, Point(56, 284), below = true)
        }
    }

    @Meme("流星")
    val meteor: Maker = { _, texts ->
        val text = texts.ifBlank { "我要对象" }
        val frame = BuildImage.open(imgDir["meteor/0.png"])
        kotlin.runCatching {
            frame.drawText(
                listOf(220, 230, 920, 315),
                text,
                allowWrap = true,
                maxFontSize = 80,
                minFontSize = 20,
                fill = Colors.WHITE,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.saveJpg()
    }

    @Meme("别说了")
    val shutUp: Maker = { _, texts ->
        val text = texts.ifBlank { "你不要再说了" }
        val frame = BuildImage.open(imgDir["shutup/0.jpg"])
        kotlin.runCatching {
            frame.drawText(
                listOf(10, 180, 230, 230), text, allowWrap = true, maxFontSize = 40, minFontSize = 15,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.saveJpg()
    }

    @Meme("膜", "膜拜")
    val worship: Maker = { images, _ ->
        val img = images[0].convert("RGBA")
        val points = listOf(Point(0, -30), Point(135, 17), Point(135, 145), Point(0, 140))
        val paint = img.square().resize(SizeInt(150, 150)).perspective(points)
        val frames = (0 until 10).map {
            val frame = BuildImage.open(imgDir["worship/$it.png"])
            frame.paste(paint, below = true).image
        }
        saveGif(frames, 0.04)
    }

    @Meme("群青")
    val cyan: Maker = { images, _ ->
        val color = RGBA.invoke(78, 114, 184)
        val frame = images[0].convert("RGB").square().resize(SizeInt(500, 500))
            .colorMask(color)
        frame.drawText(
            listOf(400, 40, 480, 280), "群\n青", maxFontSize = 80, fontName = "Glow Sans SC Normal Bold",
            fill = Colors.WHITE, strokeRatio = 0.04, strokeFill = color,
        ).drawText(
            listOf(200, 270, 480, 350), "YOASOBI", hAlign = HorizontalAlign.RIGHT, maxFontSize = 40,
            fill = Colors.WHITE, strokeRatio = 0.06, strokeFill = color,
        )
        frame.saveJpg()
    }

    @Meme("许愿失败")
    val wishFail: Maker = { _, texts ->
        val text = texts.ifBlank { "我要对象" }
        val frame = BuildImage.open(imgDir["wish_fail/0.png"])
        kotlin.runCatching {
            frame.drawText(
                listOf(70, 305, 320, 380),
                text,
                allowWrap = true,
                maxFontSize = 80,
                minFontSize = 20,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.saveJpg()
    }

    @Meme("坐牢")
    val imprison: Maker = { _, texts ->
        val text = texts.ifBlank { "我发涩图被抓起来了" }
        val frame = BuildImage.open(imgDir["imprison/0.jpg"])
        kotlin.runCatching {
            frame.drawText(
                listOf(10, 157, 230, 197),
                text,
                allowWrap = true,
                maxFontSize = 35,
                minFontSize = 15,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.saveJpg()
    }

    @Meme("胡桃啃")
    val hutaoBite: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(SizeInt(100, 100))
        val frames = listOf(listOf(98, 101, 108, 234), listOf(96, 100, 108, 237)).mapIndexed { i, locs ->
            val frame = BuildImage.open(imgDir["hutao_bite/$i.png"])
            val (w, h, x, y) = locs
            frame.paste(img.resize(SizeInt(w, h)), Point(x, y), below = true).image
        }
        saveGif(frames, 0.1)
    }

    @Meme("墙纸")
    val wallpaper: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(SizeInt(515, 383), keepRatio = true)
        val frames = (0 until 8).map { BuildImage.open(imgDir["wallpaper/$it.png"]).image }.toMutableList()
        (8 until 20).forEach {
            val frame = BuildImage.open(imgDir["wallpaper/$it.png"])
            frame.paste(img, Point(176, -9), below = true)
            frames.add(frame.image)
        }
        saveGif(frames, 0.07)
    }

    @Meme("不喊我")
    val notCallMe: Maker = { _, texts ->
        val text = texts.ifBlank { "开银趴不喊我是吧" }
        val frame = BuildImage.open(imgDir["not_call_me/0.png"])
        kotlin.runCatching {
            frame.drawText(
                listOf(228, 11, 340, 164),
                text,
                allowWrap = true,
                maxFontSize = 80,
                minFontSize = 20,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.savePng()
    }

    @Meme("狂爱", "狂粉")
    val fanatic: Maker = { _, texts ->
        val text = texts.ifBlank { "洛天依" }
        val frame = BuildImage.open(imgDir["fanatic/0.jpg"])
        kotlin.runCatching {
            frame.drawText(
                listOf(145, 40, 343, 160),
                text,
                allowWrap = true,
                linesAlign = HorizontalAlign.LEFT,
                maxFontSize = 70,
                minFontSize = 30,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.saveJpg()
    }

    @Meme("捶")
    val thump: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square()
        val frames = listOf(
            listOf(65, 128, 77, 72), listOf(67, 128, 73, 72),
            listOf(54, 139, 94, 61), listOf(57, 135, 86, 65)
        ).mapIndexed { i, (x, y, w, h) ->
            val frame = BuildImage.open(imgDir["thump/$i.png"])
            frame.paste(img.resize(SizeInt(w, h)), Point(x, y), below = true)
            frame.image
        }
        saveGif(frames, 0.04)
    }

    @Meme("捶爆", "爆捶")
    val thumpWildly: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(SizeInt(122, 122), keepRatio = true)
        val rawFrames = (0 until 31).map { BuildImage.open(imgDir["thump_wildly/$it.png"]) }
        (0 until 14).forEach {
            rawFrames[it].paste(img, Point(203, 196), below = true)
        }
        rawFrames[14].paste(img, Point(207, 239), below = true)
        val frames = rawFrames.map { it.image }.toMutableList()
        (0 until 6).forEach { _ ->
            frames.add(frames[0])
        }
        saveGif(frames, 0.04)
    }

    @Meme("起来了")
    val wakeup: Maker = { _, texts ->
        val text = texts.ifBlank { "好" }
        val frame = BuildImage.open(imgDir["wakeup/0.jpg"])
        kotlin.runCatching {
            frame.drawText(listOf(310, 270, 460, 380), text, maxFontSize = 90, minFontSize = 50)
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.drawText(
            listOf(50, 610, 670, 720),
            "${text}起来了", maxFontSize = 110, minFontSize = 70
        )
        frame.saveJpg()
    }

    @Meme("米哈游")
    val mihoyo: Maker = { images, _ ->
        val mask = BuildImage.new("RGBA", SizeInt(500, 60), RGBA(53, 49, 65, 230))
        val logo = BuildImage.open(imgDir["mihoyo/logo.png"]).resizeHeight(50)
        makePngOrGif(images[0]) {
            val img = convert("RGBA").resize(SizeInt(500, 500), keepRatio = true)
            img.paste(mask, Point(0, 440), alpha = true)
            img.paste(logo, Point((img.width - logo.width) / 2, 445), alpha = true)
            img.circleCorner(100.0)
        }
    }

    @Meme("结婚申请", "结婚登记")
    val marriage: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resizeHeight(1080)
        var imgW = img.width
        var imgH = img.height
        if (imgW > 1500)
            imgW = 1500
        else if (imgW < 800)
            imgH = imgH * imgW / 800
        val frame = img.resizeCanvas(SizeInt(imgW, imgH)).resizeHeight(1080)
        val left = BuildImage.open(imgDir["marriage/0.png"])
        val right = BuildImage.open(imgDir["marriage/1.png"])
        frame.paste(left, alpha = true).paste(
            right, Point(frame.width - right.width, 0), alpha = true
        )
        frame.saveJpg()
    }

    @Meme("打印")
    val printing: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(
            SizeInt(304, 174), keepRatio = true,
            inside = true, bgColor = Colors.WHITE, direction = BuildImage.DirectionType.South
        )
        val frames = (0 until 115).map {
            BuildImage.open(imgDir["printing/$it.png"])
        }
        (50 until 115).forEach {
            frames[it].paste(img, Point(146, 164), below = true)
        }
        saveGif(frames.map { it.image }, 0.05)
    }

    @Meme("永远爱你")
    val loveYou: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square()
        val locs = listOf(listOf(68, 65, 70, 70), listOf(63, 59, 80, 80))
        val frames = mutableListOf<Bitmap>()
        (0 until 2).forEach {
            val heart = BuildImage.open(imgDir["love_you/$it.png"])
            val frame = BuildImage.new("RGBA", heart.size, Colors.WHITE)
            val (x, y, w, h) = locs[it]
            frame.paste(img.resize(SizeInt(w, h)), Point(x, y), alpha = true).paste(heart, alpha = true)
            frames.add(frame.image)
        }
        saveGif(frames, 0.2)
    }

    @Meme("搓")
    val twist: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(SizeInt(78, 78))
        val locs = listOf(
            listOf(25, 66, 0), listOf(25, 66, 60), listOf(23, 68, 120),
            listOf(20, 69, 180), listOf(22, 68, 240), listOf(25, 66, 300)
        )
        val frames = (0 until 5).map {
            val frame = BuildImage.open(imgDir["twist/$it.png"])
            val (x, y, a) = locs[it]
            frame.paste(img.rotate(a.toDouble()), Point(x, y), below = true).image
        }
        saveGif(frames, 0.1)

    }

    @Meme("快跑")
    val run: Maker = { _, texts ->
        val text = texts[0]
        val frame = BuildImage.open(imgDir["run/0.png"])
        val textImg = BuildImage.new("RGBA", SizeInt(122, 53))
        kotlin.runCatching {
            textImg.drawText(
                listOf(0, 0, 122, 53),
                text,
                allowWrap = true,
                maxFontSize = 50,
                minFontSize = 10,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.paste(textImg.rotate(7.0, expand = true), Point(200, 195), alpha = true)
        frame.saveJpg()
    }

    @Meme("osu")
    val osu: Maker = { _, texts ->
        val text = texts.ifBlank { "hso" }
        val frame = BuildImage.open(imgDir["osu/osu.png"])
        kotlin.runCatching {
            frame.drawText(
                listOf(80, 80, 432, 432),
                text,
                maxFontSize = 192,
                minFontSize = 80,
                fill = Colors.WHITE,
                fontName = "Aller Bold",
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.savePng()
    }

    @Meme("咖波蹭", "咖波贴")
    val capooRub: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(SizeInt(180, 180))
        val locs = listOf(
            listOf(178, 184, 78, 260),
            listOf(178, 174, 84, 269),
            listOf(178, 174, 84, 269),
            listOf(178, 178, 84, 264)
        )
        val frames = (0 until 4).map {
            val frame = BuildImage.open(imgDir["capoo_rub/$it.png"])
            val (w, h, x, y) = locs[it]
            frame.paste(img.resize(SizeInt(w, h)), Point(x, y), below = true).image
        }
        saveGif(frames, 0.1)
    }
    @Meme("布洛妮娅举牌", "大鸭鸭举牌")
    val bronyaHoldSign: Maker = { _, texts ->
        val text = texts.ifBlank { "V我50" }
        val frame = BuildImage.open(imgDir["bronya_holdsign/0.jpg"])
        kotlin.runCatching {
            frame.drawText(
                listOf(190, 675, 640, 930),
                text,
                fill = RGBA(111, 95, 95),
                allowWrap = true,
                maxFontSize = 60,
                minFontSize = 25,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.saveJpg()
    }

    @Meme("抱大腿")
    val hugLeg: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square()
        val locs = listOf(
            listOf(50, 73, 68, 92),
            listOf(58, 60, 62, 95),
            listOf(65, 10, 67, 118),
            listOf(61, 20, 77, 97),
            listOf(55, 44, 65, 106),
            listOf(66, 85, 60, 98)
        )
        val frames = (0 until 6).map {
            val frame = BuildImage.open(imgDir["hug_leg/$it.png"])
            val (x, y, w, h) = locs[it]
            frame.paste(img.resize(SizeInt(w, h)), Point(x, y), below = true).image
        }
        saveGif(frames, 0.06)
    }

    @Meme("滚")
    val roll: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(SizeInt(210, 210))
        val locs = listOf(
            listOf(87, 77, 0), listOf(96, 85, -45), listOf(92, 79, -90), listOf(92, 78, -135),
            listOf(92, 75, -180), listOf(92, 75, -225), listOf(93, 76, -270), listOf(90, 80, -315)
        )
        val frames = (0 until 8).map {
            val frame = BuildImage.open(imgDir["roll/$it.png"])
            val (x, y, a) = locs[it]
            frame.paste(img.rotate(a.toDouble()), Point(x, y), below = true).image
        }
        saveGif(frames, 0.1)
    }

    @Meme("舰长")
    val captain: Maker = { imgs, _ ->
        val images = imgs.toMutableList()
        if (images.size == 2) {
            images.add(images.last())
        }

        val bg0 = BuildImage.open(imgDir["captain/0.png"])
        val bg1 = BuildImage.open(imgDir["captain/1.png"])
        val bg2 = BuildImage.open(imgDir["captain/2.png"])

        val frame = BuildImage.new("RGBA", SizeInt(640, 440 * images.size), Colors.WHITE)
        (0 until images.size).map { i ->
            var bg = if (i < images.size - 2) bg0 else if (i == images.size - 2) bg1 else bg2
            images[i] = images[i].convert("RGBA").square().resize(SizeInt(250, 250))
            bg = bg.copy().paste(images[i], Point(350, 85))
            frame.paste(bg, Point(0, 440 * i))
        }
        frame.saveJpg()
    }

    @Meme("挠头")
    val scratchHead: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(SizeInt(68, 68))
        val locs = listOf(
            listOf(53, 46, 4, 5),
            listOf(50, 45, 7, 6),
            listOf(50, 42, 6, 8),
            listOf(50, 44, 7, 7),
            listOf(53, 42, 4, 8),
            listOf(52, 45, 7, 7)
        )
        val frames = (0 until 6).map {
            val frame = BuildImage.open(imgDir["scratch_head/$it.png"])
            val (w, h, x, y) = locs[it]
            frame.paste(img.resize(SizeInt(w, h)), Point(x, y), below = true).image
        }
        saveGif(frames, 0.1)
    }

    @Meme("锤")
    val hammer: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square()
        val locs = listOf(
            listOf(62, 143, 158, 113), listOf(52, 177, 173, 105), listOf(42, 192, 192, 92),
            listOf(46, 182, 184, 100), listOf(54, 169, 174, 110), listOf(69, 128, 144, 135),
            listOf(65, 130, 152, 124)
        )
        val frames = (0 until 6).map {
            val frame = BuildImage.open(imgDir["hammer/$it.png"])
            val (x, y, w, h) = locs[it]
            frame.paste(img.resize(SizeInt(w, h)), Point(x, y), below = true).image
        }
        saveGif(frames, 0.07)
    }

    @Meme("敲")
    val knock: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square()
        val locs = listOf(
            listOf(60, 308, 210, 195), listOf(60, 308, 210, 198), listOf(45, 330, 250, 172), listOf(58, 320, 218, 180),
            listOf(60, 310, 215, 193), listOf(40, 320, 250, 285), listOf(48, 308, 226, 192),
            listOf(51, 301, 223, 200)
        )
        val frames = (0 until 6).map {
            val frame = BuildImage.open(imgDir["knock/$it.png"])
            val (x, y, w, h) = locs[it]
            frame.paste(img.resize(SizeInt(w, h)), Point(x, y), below = true).image
        }
        saveGif(frames, 0.04)
    }

    @Meme("猫羽雫举牌", "猫猫举牌")
    val nekohaHoldSign: Maker = { _, texts ->
        val text = texts.ifBlank { "V我50" }
        val frame = BuildImage.open(imgDir["nekoha_holdsign/0.jpg"])
        kotlin.runCatching {
            frame.drawText(
                listOf(210, 520, 570, 765),
                text,
                fill = RGBA(72, 110, 173),
                allowWrap = true,
                fontName = "FZShaoEr-M11S",
                maxFontSize = 65,
                minFontSize = 25
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.saveJpg()
    }

    @Meme("悲报")
    val badNews: Maker = { _, texts ->
        val text = texts[0]
        val frame = BuildImage.open(imgDir["bad_news/0.png"])
        kotlin.runCatching {
            frame.drawText(
                listOf(50, 100, frame.width - 50, frame.height - 100),
                text,
                allowWrap = true,
                maxFontSize = 60,
                minFontSize = 30,
                fill = RGBA(0, 0, 0),
                strokeRatio = 1.0 / 15,
                strokeFill = Colors.WHITE,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.savePng()
    }

    @Meme("喜报")
    val goodNews: Maker = { _, texts ->
        val text = texts[0]
        val frame = BuildImage.open(imgDir["good_news/0.jpg"])
        kotlin.runCatching {
            frame.drawText(
                listOf(50, 100, frame.width - 50, frame.height - 100),
                text,
                allowWrap = true,
                maxFontSize = 60,
                minFontSize = 30,
                fill = RGBA(238, 0, 0),
                strokeRatio = 1.0 / 15,
                strokeFill = RGBA(255, 255, 153),
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.savePng()
    }

    @Meme("刮刮乐")
    val scratchcard: Maker = { _, texts ->
        val text = texts[0]
        val frame = BuildImage.open(imgDir["scratchcard/0.png"])
        kotlin.runCatching {
            frame.drawText(
                listOf(80, 160, 360, 290),
                text,
                allowWrap = true,
                maxFontSize = 80,
                minFontSize = 30,
                fill = Colors.WHITE,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        val mask = BuildImage.open(imgDir["scratchcard/1.png"])
        frame.paste(mask, alpha = true)
        frame.saveJpg()
    }

    @Meme("垃圾", "垃圾桶")
    val garbage: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(SizeInt(79, 79))
        val locs = mutableListOf<Point>()
        repeat(3) {
            locs.add(Point(39, 40))
        }
        repeat(2) {
            locs.add(Point(39, 30))
        }
        repeat(10) {
            locs.add(Point(39, 32))
        }
        locs.addAll(
            listOf(
                Point(39, 30), Point(39, 27), Point(39, 32), Point(37, 49), Point(37, 64),
                Point(37, 67), Point(37, 67), Point(39, 69), Point(37, 70), Point(37, 70)
            )
        )
        val frames = (0 until 25).map {
            val frame = BuildImage.open(imgDir["garbage/$it.png"])
            frame.paste(img, locs[it], below = true).image
        }
        saveGif(frames, 0.04)
    }

    @Meme("捣")
    val pound: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square()
        val locs = listOf(
            listOf(135, 240, 138, 47), listOf(135, 240, 138, 47), listOf(150, 190, 105, 95),
            listOf(150, 190, 105, 95), listOf(148, 188, 106, 98), listOf(146, 196, 110, 88),
            listOf(145, 223, 112, 61), listOf(145, 223, 112, 61)
        )
        val frames = (0 until 8).map {
            val frame = BuildImage.open(imgDir["pound/$it.png"])
            val (x, y, w, h) = locs[it]
            frame.paste(img.resize(SizeInt(w, h)), Point(x, y), below = true).image
        }
        saveGif(frames, 0.05)
    }

    @Meme("踢球")
    val kickBall: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(SizeInt(78, 78))
        val locs = listOf(
            Point(57, 136), Point(56, 117), Point(55, 99), Point(52, 113), Point(50, 126),
            Point(48, 139), Point(47, 112), Point(47, 85), Point(47, 57), Point(48, 97),
            Point(50, 136), Point(51, 176), Point(52, 169), Point(55, 181), Point(58, 153)
        )
        val frames = (0 until 15).map {
            val frame = BuildImage.open(imgDir["kick_ball/$it.png"])
            frame.paste(img.rotate(-24.0 * it), locs[it], below = true).image
        }
        saveGif(frames, 0.1)
    }

    @Meme("拍")
    val pat: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square()
        val locs = listOf(listOf(11, 73, 106, 100), listOf(8, 79, 112, 96))
        val imgFrames = (0 until 10).map {
            val frame = BuildImage.open(imgDir["pat/$it.png"])
            val (x, y, w, h) = if (it == 2) locs[1] else locs[0]
            frame.paste(img.resize(SizeInt(w, h)), Point(x, y), below = true)
        }
        val frames =
            listOf(0, 1, 2, 3, 1, 2, 3, 0, 1, 2, 3, 0, 0, 1, 2, 3, 0, 0, 0, 0, 4, 5, 5, 5, 6, 7, 8, 9).map { n ->
                imgFrames[n].image
            }
        saveGif(frames, 0.085)
    }

    @Meme("伊地知虹夏举牌", "虹夏举牌")
    val nijikaHoldSign: Maker = { _, texts ->
        val text = texts.ifBlank { "你可少看点二次元吧" }
        val frame = BuildImage.open(imgDir["nijika_holdsign/0.jpg"])
        kotlin.runCatching {
            frame.drawText(
                listOf(57, 279, 249, 405),
                text,
                fill = RGBA(111, 95, 95),
                allowWrap = true,
                fontName = "FZSJ-QINGCRJ",
                maxFontSize = 60,
                minFontSize = 25
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.saveJpg()
    }

    @Meme("踩")
    val stepOn: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(SizeInt(100, 100), keepRatio = true)
        val locs = listOf(
            listOf(104, 72, 32, 185, -25),
            listOf(104, 72, 32, 185, -25),
            listOf(90, 73, 51, 207, 0),
            listOf(88, 78, 51, 202, 0),
            listOf(88, 86, 49, 197, 0)
        )
        val frames = (0 until 5).map {
            val frame = BuildImage.open(imgDir["step_on/$it.png"])
            val (w, h, x, y, angle) = locs[it]
            frame.paste(img.resize(SizeInt(w, h)).rotate(angle.toDouble(), expand = true), Point(x, y), below = true).image
        }
        saveGif(frames, 0.07)
    }

    @Meme("鲁迅说", "鲁迅说过")
    val luxunSay: Maker = { _, texts ->
        val text = texts.ifBlank { "我没有说过这句话" }
        val frame = BuildImage.open(imgDir["luxun_say/0.jpg"])
        kotlin.runCatching {
            frame.drawText(
                listOf(40, frame.height - 200, frame.width - 40, frame.height - 100),
                text,
                allowWrap = true,
                maxFontSize = 40,
                minFontSize = 30,
                fill = Colors.WHITE,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.drawText(listOf(320, 400), "--鲁迅", fontSize = 30, fill = Colors.WHITE)
        frame.saveJpg()
    }

    @Meme("撕", "需要1~2张图片")
    val rip: Maker = { images, _ ->
        var (frame, selfImg, userImg) =
            if (images.size >= 2) {
                listOf(BuildImage.open(imgDir["rip/1.png"]), images[0], images[1])
            } else {
                listOf(BuildImage.open(imgDir["rip/0.png"]), null, images[0])
            }
        userImg = userImg!!.convert("RGBA").square().resize(SizeInt(385, 385))
        selfImg?.let {
            selfImg = it.convert("RGBA").square().resize(SizeInt(230, 230))
            frame!!.paste(it, Point(408, 418), below = true)
        }
        frame!!.paste(userImg.rotate(24.0, expand = true), Point(-5, 355), below = true)
        frame.paste(userImg.rotate(-11.0, expand = true), Point(649, 310), below = true)
        frame.saveJpg()
    }

    @Meme("举")
    val raiseImage: Maker = { images, _ ->
        val innerSize = SizeInt(599, 386)
        val pastePos = Point(134, 91)

        val bg = BuildImage.open(imgDir["raise_image/raise_image.png"])

        makeJpgOrGif(images[0]) {
            val innerFrame = BuildImage.new("RGBA", innerSize, Colors.WHITE).paste(
                convert("RGBA").resize(innerSize, keepRatio = true), alpha = true
            )
            bg.copy().paste(innerFrame, pastePos, alpha = true, below = true)
        }

    }

    @Meme("打拳")
    val punch: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(SizeInt(260, 260))
        val locs = listOf(
            Point(-50, 20), Point(-40, 10), Point(-30, 0), Point(-20, -10), Point(-10, -10), Point(0, 0),
            Point(10, 10), Point(20, 20), Point(10, 10), Point(0, 0), Point(-10, -10), Point(10, 0), Point(-30, 10)
        )
        val frames = (0 until 13).map {
            val fist = BuildImage.open(imgDir["punch/$it.png"])
            val frame = BuildImage.new("RGBA", fist.size, Colors.WHITE)
            val (x, y) = locs[it]
            frame.paste(img, Point(x, y - 15), alpha = true).paste(fist, alpha = true).image
        }
        saveGif(frames, 0.03)
    }

    @Meme("抛", "掷")
    val throwGif: Maker = { images, _ ->
        val img = images[0].convert("RGBA").circle()
        val locs = listOf(
            listOf(listOf(32, 32, 108, 36)),
            listOf(listOf(32, 32, 122, 36)),
            listOf(),
            listOf(listOf(123, 123, 19, 129)),
            listOf(listOf(185, 185, -50, 200), listOf(33, 33, 289, 70)),
            listOf(listOf(32, 32, 280, 73)),
            listOf(listOf(35, 35, 259, 31)),
            listOf(listOf(175, 175, -50, 220))
        )
        val frames = (0 until 8).map {
            val frame = BuildImage.open(imgDir["throw_gif/$it.png"])
            locs[it].forEach { (w, h, x, y) ->
                frame.paste(img.resize(SizeInt(w, h)), Point(x, y), alpha = true)
            }
            frame.image
        }
        saveGif(frames, 0.1)
    }

    @Meme("啃")
    val bite: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square()
        val locs = listOf(
            listOf(90, 90, 105, 150), listOf(90, 83, 96, 172), listOf(90, 90, 106, 148),
            listOf(88, 88, 97, 167), listOf(90, 85, 89, 179), listOf(90, 90, 106, 151)
        )
        val frames = (0 until 6).map {
            val frame = BuildImage.open(imgDir["bite/$it.png"])
            val (w, h, x, y) = locs[it]
            frame.paste(img.resize(SizeInt(w, h)), Point(x, y), below = true).image
        }.toMutableList()
        (6 until 16).forEach {
            val frame = BuildImage.open(imgDir["bite/$it.png"])
            frames.add(frame.image)
        }
        saveGif(frames, 0.07)
    }

    @Meme("风车转")
    val windmill_turn: Maker = { images, _ ->
        makeGifOrCombinedGif(
            images[0], 5, 0.05, FrameAlignPolicy.ExtendLoop
        ) { i ->
            val img = convert("RGBA").resize(SizeInt(300, 300), keepRatio = true)
            val frame = BuildImage.new("RGBA", SizeInt(600, 600), Colors.WHITE)
            frame.paste(img, alpha = true)
            frame.paste(img.rotate(90.0), Point(0, 300), alpha = true)
            frame.paste(img.rotate(180.0), Point(300, 300), alpha = true)
            frame.paste(img.rotate(270.0), Point(300, 0), alpha = true)
            frame.rotate(i * 18.0).crop(listOf(50, 50, 550, 550))
        }
    }

    @Meme("迷惑")
    val confuse: Maker = { images, _ ->
        val imgW = min(images[0].width, 500)
        makeGifOrCombinedGif(
            images[0], 100, 0.02, FrameAlignPolicy.ExtendLoop, inputBased = true
        ) {
            val img = convert("RGBA").resizeWidth(imgW)
            val frame = BuildImage.open(imgDir["confuse/$it.png"]).resize(img.size, keepRatio = true)
            val bg = BuildImage.new("RGB", img.size, Colors.WHITE)
            bg.paste(img, alpha = true).paste(frame, alpha = true)
        }
    }

    @Meme("爬", help = "有92种爬，可以指定编号，不指定则为随机")
    val crawl: Maker = { images, texts ->
        val totalNum = 92
        val arg = texts.firstOrNull()?.toIntOrNull()
        val num = if (arg != null && 1 <= arg && arg <= totalNum)
            arg
        else
            Random.nextInt(1, totalNum)

        val img = images[0].convert("RGBA").circle().resize(SizeInt(100, 100))
        val frame = BuildImage.open(imgDir["crawl/%02d.jpg".format(num)])
        frame.paste(img, Point(0, 400), alpha = true)
        frame.saveJpg()
    }

    @Meme("震动")
    val vibrate: Maker = { images, _ ->
        makeGifOrCombinedGif(
            images[0], 5, 0.05, FrameAlignPolicy.ExtendLoop
        ) { i ->
            val img = convert("RGBA").square()
            val w = width
            val locs = listOf(
                Point(0, 0),
                Point(w / 25, w / 25),
                Point(w / 50, w / 50),
                Point(0, w / 25),
                Point(w / 25, 0)
            )
            val frame = BuildImage.new("RGBA", SizeInt(w + w / 25, w + w / 25), Colors.WHITE)
            frame.paste(img, locs[i], alpha = true)
        }
    }

    @Meme("记仇", help = "需要文本")
    val holdGrudge: Maker = { _, texts ->
        val arg = texts.ifBlank { "群友不发好康的" }
        val pattern = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
        val date = pattern.format(LocalDateTime.now())
        val text = "$date 晴\n$arg\n这个仇我先记下了"
        val text2image = Text2Image.fromText(
            text, 45, fill = Colors.BLACK, spacing = 10, align = HorizontalAlign.LEFT,
            fontName = "Glow Sans SC Normal Regular"
        ).wrap(440.0)
        if (text2image.lines.size > 10) {
            throw TextOverLengthException()
        }
        val textImg = text2image.toImage()
        val frame = BuildImage.open(imgDir["hold_grudge/0.png"])
        val bg = BuildImage.new(
            "RGB", SizeInt(frame.width, frame.height + textImg.height + 20),
            Colors.WHITE
        )
        bg.paste(frame).paste(textImg, Point(30, frame.height + 5), alpha = true)
        bg.saveJpg()
    }

    @Meme("吸", "嗦")
    val suck: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square()
        val locs = listOf(
            listOf(82, 100, 130, 119), listOf(82, 94, 126, 125), listOf(82, 120, 128, 99), listOf(81, 164, 132, 55),
            listOf(79, 163, 132, 55), listOf(82, 140, 127, 79), listOf(83, 152, 125, 67), listOf(75, 157, 140, 62),
            listOf(72, 165, 144, 54), listOf(80, 132, 128, 87), listOf(81, 127, 127, 92), listOf(79, 111, 132, 108)
        )
        val frames = (0 until 12).map {
            val bg = BuildImage.open(imgDir["suck/$it.png"])
            val frame = BuildImage.new("RGBA", bg.size, Colors.WHITE)
            val (x, y, w, h) = locs[it]
            frame.paste(img.resize(SizeInt(w, h)), Point(x, y), alpha = true).paste(bg, alpha = true).image
        }
        saveGif(frames, 0.08)
    }


    @Meme("举牌", help = "需要文本")
    val raiseSign: Maker = { _, texts ->
        val text = texts.ifBlank { "大佬带带我" }
        val frame = BuildImage.open(imgDir["raise_sign/0.jpg"])
        var textImg = BuildImage.new("RGBA", SizeInt(360, 260))
        kotlin.runCatching {
            textImg.drawText(
                listOf(10, 10, 350, 250),
                text,
                maxFontSize = 80,
                minFontSize = 30,
                allowWrap = true,
                spacing = 10,
                fontName = "FZShaoEr-M11S",
                fill = "#51201b".hexToRGBA(),
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        textImg = textImg.perspective(listOf(Point(33, 0), Point(375, 120), Point(333, 387), Point(0, 258)))
        frame.paste(textImg, Point(285, 24), alpha = true)
        frame.saveJpg()
    }

    @Meme("坐得住", "坐的住", help = "需要图片和文本")
    val sitStill: Maker = { images, texts ->
        val name = texts.ifBlank { "" }
        val frame = BuildImage.open(imgDir["sit_still/0.png"])
        if (name.isNotBlank()) {
            kotlin.runCatching {
                frame.drawText(
                    listOf(100, 170, 600, 330),
                    name,
                    vAlign = VerticalAlign.BOTTOM,
                    maxFontSize = 75,
                    minFontSize = 30,
                )
            }.onFailure {
                throw TextOverLengthException()
            }
        }
        val img = images[0].convert("RGBA").circle().resize(SizeInt(150, 150)).rotate(-10.0, expand = true)
        frame.paste(img, Point(268, 344), alpha = true)
        frame.saveJpg()
    }

    @Meme("诺基亚", "有内鬼", help = "需要文本")
    val nokia: Maker = { _, texts ->
        val text = texts.joinToString(" ").ifBlank { "无内鬼，继续交易" }.take(900)
        val textImg = BuildImage(
            Text2Image.fromText(text, 70, fontName = "FZXS14", fill = Colors.BLACK, spacing = 30)
                .wrap(700.0)
                .toImage()
        ).resizeCanvas(SizeInt(700, 450), direction = BuildImage.DirectionType.Northwest)
            .rotate(-9.3, expand = true)

        val headImg = BuildImage(
            Text2Image.fromText(
                "${text.length}/900", 70, fontName = "FZXS14", fill = RGBA(129, 212, 250)
            ).toImage()
        ).rotate(-9.3, expand = true)

        val frame = BuildImage.open(imgDir["nokia/0.jpg"])
        frame.paste(textImg, Point(205, 330), alpha = true)
        frame.paste(headImg, Point(790, 320), alpha = true)
        frame.saveJpg()
    }

    @Meme("罗永浩说", help = "需要文本")
    val luoyonghaoSay: Maker = { _, texts ->
        val text = texts.ifBlank { "又不是不能用" }
        val frame = BuildImage.open(imgDir["luoyonghao_say/0.jpg"])
        var textFrame = BuildImage.new("RGBA", SizeInt(365, 120))
        kotlin.runCatching {
            textFrame.drawText(
                listOf(40, 10, 325, 110),
                text,
                allowWrap = true,
                maxFontSize = 50,
                minFontSize = 10,
                vAlign = VerticalAlign.TOP,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        textFrame = textFrame.perspective(listOf(Point(52, 10), Point(391, 0), Point(364, 110), Point(0, 120)))
            .filter(GaussianBlurFilter(radius = 0.8))
        frame.paste(textFrame, Point(48, 246), alpha = true)
        frame.saveJpg()
    }

    @Meme("可莉吃")
    val kleeEat: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(SizeInt(83, 83))
        val locs = listOf(
            Point(0, 174), Point(0, 174), Point(0, 174), Point(0, 174), Point(0, 174), Point(12, 160), Point(19, 152),
            Point(23, 148), Point(26, 145), Point(32, 140), Point(37, 136), Point(42, 131), Point(49, 127), Point(70, 126),
            Point(88, 128), Point(-30, 210), Point(-19, 207), Point(-14, 200), Point(-10, 188), Point(-7, 179),
            Point(-3, 170), Point(-3, 175), Point(-1, 174), Point(0, 174), Point(0, 174), Point(0, 174), Point(0, 174),
            Point(0, 174), Point(0, 174), Point(0, 174), Point(0, 174)
        )
        val frames = (0 until 31).map {
            val frame = BuildImage.open(imgDir["klee_eat/$it.png"])
            frame.paste(img, locs[it], below = true).image
        }
        saveGif(frames, 0.1)

    }

    @Meme("ph", "某hub", help = "需要两个文本")
    val pxxxHub: Maker = { _, rawTexts ->
        val texts = if (rawTexts.size < 2) listOf("You", "Tube") else rawTexts
        val leftImg = Text2Image.fromText(
            texts[0], fontSize = 200, fill = Colors.WHITE, align = HorizontalAlign.LEFT
        ).toImage(
            bgColor = Colors.BLACK, padding = listOf(20, 10)
        )
        val rightImg = BuildImage(
            Text2Image.fromText(
                texts[1],
                fontSize = 200,
                fill = Colors.BLACK,
                fontName = "Glow Sans SC Normal Bold",
                align = HorizontalAlign.LEFT
            ).toImage(bgColor = RGBA(247, 152, 23), padding = listOf(20, 10))
        ).circleCorner(20.0)

        var frame = BuildImage.new(
            "RGBA",
            SizeInt(leftImg.width + rightImg.width, max(leftImg.height, rightImg.height)),
            Colors.BLACK
        )
        frame.paste(leftImg, Point(0, frame.height - leftImg.height)).paste(
            rightImg, Point(leftImg.width, frame.height - rightImg.height), alpha = true
        )
        frame = frame.resizeCanvas(
            SizeInt(frame.width + 100, frame.height + 100),
            bgColor = Colors.BLACK
        )
        frame.saveJpg()
    }

    @Meme("上香", help = "可以附带参数 --black 来开启黑白模式")
    val mourning: Maker = { images, texts ->
        val arg = texts.ifBlank { "" }
        val frame = BuildImage.open(imgDir["mourning/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert(if (arg == "--black") "L" else "RGBA")
                .resize(SizeInt(635, 725), keepRatio = true)
            frame.copy().paste(img, Point(645, 145), below = true)
        }
    }

    @Meme("口号", help = "需要六段文本")
    val slogan: Maker = { _, rawTexts ->
        val texts = if (rawTexts.size >= 6) rawTexts else listOf(
            "我们是谁？", "XX人！", "到XX大学来做什么？", "混！", "将来毕业后要做什么样的人？", "混混！"
        )
        val frame = BuildImage.open(imgDir["slogan/0.jpg"])
        val draw: suspend (List<Int>, String) -> Unit = { pos, text ->
            kotlin.runCatching {
                frame.drawText(
                    pos, text, maxFontSize = 40, minFontSize = 15, allowWrap = true
                )
            }.onFailure {
                throw TextOverLengthException()
            }
        }
        draw(listOf(10, 0, 294, 50), texts[0])
        draw(listOf(316, 0, 602, 50), texts[1])
        draw(listOf(10, 230, 294, 280), texts[2])
        draw(listOf(316, 230, 602, 280), texts[3])
        draw(listOf(10, 455, 294, 505), texts[4])
        draw(listOf(316, 455, 602, 505), texts[5])
        frame.saveJpg()
    }

    @Meme("低情商xx高情商xx", "低情商,高情商,低情商高情商")
    val highEQ: Maker = { _, rawTexts ->
        val texts = if (rawTexts.size >= 2) rawTexts else listOf(
            "高情商", "低情商"
        )
        val frame = BuildImage.open(imgDir["high_EQ/0.jpg"])
        val draw: suspend (List<Int>, String) -> Unit = { pos, text ->
            kotlin.runCatching {
                frame.drawText(
                    pos,
                    text,
                    maxFontSize = 100,
                    minFontSize = 50,
                    allowWrap = true,
                    fill = Colors.WHITE,
                    strokeFill = Colors.BLACK,
                    strokeRatio = 0.05,
                )
            }.onFailure {
                throw TextOverLengthException()
            }
        }

        draw(listOf(40, 540, 602, 1140), texts[0])
        draw(listOf(682, 540, 1244, 1140), texts[1])
        frame.saveJpg()
    }

    @Meme("偷学")
    val learn: Maker = { images, texts ->
        val text = texts.ifBlank { "偷学群友数理基础" }
        val frame = BuildImage.open(imgDir["learn/0.png"])
        kotlin.runCatching {
            frame.drawText(
                listOf(100, 1360, frame.width - 100, 1730),
                text,
                maxFontSize = 350,
                minFontSize = 200,
                fontName = "Glow Sans SC Heavy Bold",
            )
        }.onFailure {
            throw TextOverLengthException()
        }

        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resize(SizeInt(1751, 1347), keepRatio = true)
            frame.copy().paste(img, Point(1440, 0), alpha = true)
        }
    }

    @Meme("整点薯条", help = "需要三段文本")
    val findChips: Maker = { _, rawTexts ->
        val texts = if (rawTexts.size >= 4) rawTexts else listOf(
            "我们要飞向何方",
            "我打算待会去码头整点薯条",
            "我说的是归根结底，活着是为了什么",
            "为了待会去码头整点薯条"
        )
        val frame = BuildImage.open(imgDir["find_chips/0.jpg"])

        val draw: suspend (List<Int>, String) -> Unit = { pos, text ->
            kotlin.runCatching {
                frame.drawText(
                    pos, text, maxFontSize = 30, minFontSize = 12, allowWrap = true
                )
            }.onFailure {
                throw TextOverLengthException()
            }
        }

        draw(listOf(405, 54, 530, 130), texts[0])
        draw(listOf(570, 62, 667, 160), texts[1])
        draw(listOf(65, 400, 325, 463), texts[2])
        draw(listOf(430, 400, 630, 470), texts[3])
        frame.saveJpg()

    }

    @Meme("这是鸡", "🐔")
    val thisChicken: Maker = { images, texts ->
        val text = texts.ifBlank { "这是十二生肖中的鸡" }
        val img = images[0].convert("RGBA").resize(SizeInt(640, 640), keepRatio = true)

        val frame = BuildImage.open(imgDir["this_chicken/0.png"])
        kotlin.runCatching {
            frame.drawText(
                listOf(0, 900, 1440, 1080),
                text,
                maxFontSize = 80,
                minFontSize = 40,
                fill = Colors.WHITE,
                strokeRatio = 1.0 / 15,
                strokeFill = Colors.BLACK,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.paste(
            img.perspective(listOf(Point(507, 0), Point(940, 351), Point(383, 625), Point(0, 256))),
            Point(201, 201), below = true,
        )
        frame.saveJpg()
    }

    @Meme("满脑子", help = "需要图片和文本")
    val fillHead: Maker = { images, texts ->
        val name = texts.ifBlank { "TA" }
        val text = "满脑子都是$name"
        val frame = BuildImage.open(imgDir["fill_head/0.jpg"])
        kotlin.runCatching {
            frame.drawText(
                listOf(20, 458, frame.width - 20, 550), text, maxFontSize = 65, minFontSize = 30
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resize(SizeInt(210, 170), keepRatio = true, inside = true)
            frame.copy().paste(img, Point(150, 2), alpha = true)
        }
    }

    @Meme("一起", help = "图片和需要文本")
    val together: Maker = { images, texts ->
        val frame = BuildImage.open(imgDir["together/0.png"])
        val text = texts.ifBlank { "一起玩吧" }
        kotlin.runCatching {
            frame.drawText(
                listOf(10, 140, 190, 190),
                text,
                fontName = "Glow Sans SC Normal Bold",
                maxFontSize = 50,
                minFontSize = 10,
                allowWrap = true,
            )
        }.onFailure {
            throw TextOverLengthException()
        }

        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resize(SizeInt(63, 63), keepRatio = true)
            frame.copy().paste(img, Point(132, 36), alpha = true)
        }
    }

    @Meme("万能表情", "空白表情", help = "需要图片和文本")
    val universal: Maker = { images, texts ->
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resizeWidth(500)
            val frames = mutableListOf(img)
            texts.forEach { text ->
                val textImg = BuildImage(
                    Text2Image.fromText(text, fontSize = 45, align = HorizontalAlign.CENTER)
                        .wrap(480.0)
                        .toImage()
                )
                frames.add(textImg.resizeCanvas(SizeInt(500, textImg.height)))
            }
            val frame = BuildImage.new(
                "RGBA", SizeInt(500, frames.sumOf { it.height } + 10), Colors.WHITE
            )
            var currentH = 0
            frames.forEach { f ->
                frame.paste(f, Point(0, currentH), alpha = true)
                currentH += f.height
            }
            frame
        }
    }

    @Meme("讲课", "敲黑板", help = "需要图片和文本")
    val teach: Maker = { images, texts ->
        val frame = BuildImage.open(imgDir["teach/0.png"]).resizeWidth(960).convert("RGBA")
        val text = texts.ifBlank { "我老婆" }
        kotlin.runCatching {
            frame.drawText(
                listOf(10, frame.height - 80, frame.width - 10, frame.height - 5),
                text,
                maxFontSize = 50,
                fill = Colors.WHITE,
                strokeFill = Colors.BLACK,
                strokeRatio = 0.06,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resize(SizeInt(550, 395), keepRatio = true)
            frame.copy().paste(img, Point(313, 60), below = true)
        }
    }

    @Meme("亲", "亲亲", help = "需要两张图片")
    val kiss: Maker = { images, _ ->
        val selfHead = images[0].convert("RGBA").circle().resize(SizeInt(40, 40))
        val userHead = images[1].convert("RGBA").circle().resize(SizeInt(50, 50))
        val userLocs = listOf(
            Point(58, 90), Point(62, 95), Point(42, 100), Point(50, 100), Point(56, 100), Point(18, 120), Point(28, 110),
            Point(54, 100), Point(46, 100), Point(60, 100), Point(35, 115), Point(20, 120), Point(40, 96)
        )
        val selfLocs = listOf(
            Point(92, 64), Point(135, 40), Point(84, 105), Point(80, 110), Point(155, 82), Point(60, 96), Point(50, 80),
            Point(98, 55), Point(35, 65), Point(38, 100), Point(70, 80), Point(84, 65), Point(75, 65)
        )
        val frames = (0 until 13).map {
            val frame = BuildImage.open(imgDir["kiss/$it.png"])
            frame.paste(userHead, userLocs[it], alpha = true)
            frame.paste(selfHead, selfLocs[it], alpha = true)
            frame.image
        }
        saveGif(frames, 0.05)
    }

    @Meme("胡桃放大")
    val walnut_zoom: Maker = { images, _ ->
        val locs = listOf(
            listOf(-222, 30, 695, 430), listOf(-212, 30, 695, 430), listOf(0, 30, 695, 430),
            listOf(41, 26, 695, 430), listOf(-100, -67, 922, 570), listOf(-172, -113, 1059, 655),
            listOf(-273, -192, 1217, 753)
        )
        val seq = listOf(0, 0, 0, 1, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 5, 6, 6, 6, 6)

        makeGifOrCombinedGif(
            images[0], 24, 0.2, FrameAlignPolicy.ExtendLast
        ) {
            val frame = BuildImage.open(imgDir["walnut_zoom/$it.png"])
            val (x, y, w, h) = locs[seq[it]]
            val img = convert("RGBA").resize(SizeInt(w, h), keepRatio = true)
            frame.paste(img.rotate(4.2, expand = true).image, Point(x, y), below = true)
        }
    }

    @Meme("看图标", help = "需要图片和文本")
    val look_this_icon: Maker = { images, texts ->
        val text = texts.ifBlank { "朋友\n先看看这个图标再说话" }
        val frame = BuildImage.open(imgDir["look_this_icon/nmsl.png"])
        kotlin.runCatching {
            frame.drawText(
                listOf(0, 933, 1170, 1143),
                text,
                linesAlign = HorizontalAlign.CENTER,
                fontName = "Glow Sans SC Heavy Bold",
                maxFontSize = 100,
                minFontSize = 50,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resize(SizeInt(515, 515), keepRatio = true)
            frame.copy().paste(img, Point(599, 403), below = true)
        }
    }

    @Meme("诈尸", "秽土转生")
    val riseDead: Maker = { images, _ ->
        val locs = listOf(
            Pair(Point(81, 55), listOf(Point(0, 2), Point(101, 0), Point(103, 105), Point(1, 105))),
            Pair(Point(74, 49), listOf(Point(0, 3), Point(104, 0), Point(106, 108), Point(1, 108))),
            Pair(Point(-66, 36), listOf(Point(0, 0), Point(182, 5), Point(184, 194), Point(1, 185))),
            Pair(Point(-231, 55), listOf(Point(0, 0), Point(259, 4), Point(276, 281), Point(13, 278))),
        )
        val img = images[0].convert("RGBA").square().resize(SizeInt(150, 150))
        val imgs = locs.map { (_, points) ->
            img.perspective(points)
        }
        val frames = (0 until 34).map { i ->
            val frame = BuildImage.open(imgDir["rise_dead/$i.png"])
            if (i <= 28) {
                val idx = if (i <= 25) 0 else i - 25
                var (x, y) = locs[idx].first
                if (i % 2 == 1) {
                    x += 1
                    y -= 1
                }
                frame.paste(imgs[idx], Point(x, y), below = true)
            }
            frame.image
        }
        saveGif(frames, 0.15)
    }

    @Meme("兑换券", help = "需要图片和文本")
    val coupon: Maker = { images, texts ->
        val img = images[0].convert("RGBA").circle().resize(SizeInt(60, 60))
        val name = texts[0]
        val text = name + texts.getOrElse(1) { "陪睡券" } + "\n（永久有效）"

        val textImg = BuildImage.new("RGBA", SizeInt(250, 100))
        kotlin.runCatching {
            textImg.drawText(
                listOf(0, 0, textImg.width, textImg.height),
                text,
                linesAlign = HorizontalAlign.CENTER,
                maxFontSize = 30,
                minFontSize = 15,
            )
        }.onFailure {
            throw TextOverLengthException()
        }

        val frame = BuildImage.open(imgDir["coupon/0.png"])
        frame.paste(img.rotate(22.0, expand = true), Point(164, 85), alpha = true)
        frame.paste(textImg.rotate(22.0, expand = true), Point(94, 108), alpha = true)
        frame.saveJpg()
    }

    @Meme("不文明", "需要图片和文本")
    val incivilization: Maker = { images, texts ->
        val frame = BuildImage.open(imgDir["incivilization/0.png"])
        val points = listOf(Point(0, 20), Point(154, 0), Point(164, 153), Point(22, 180))
        val img = images[0].convert("RGBA").circle().resize(SizeInt(150, 150)).perspective(points)
        val image = img.filter(BrightnessFilter(0.8F))
        frame.paste(image, Point(137, 151), alpha = true)
        val text = texts.ifBlank { "你刚才说的话不是很礼貌！" }
        kotlin.runCatching {
            frame.drawText(
                listOf(57, 42, 528, 117),
                text,
                fontName = "Glow Sans SC Normal Bold",
                maxFontSize = 50,
                minFontSize = 20,
                allowWrap = true,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.saveJpg()
    }

    @Meme("咖波画")
    val capooDraw: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(SizeInt(175, 120), keepRatio = true)
        val params = listOf(
            Pair(listOf(Point(27, 0), Point(207, 12), Point(179, 142), Point(0, 117)), Point(30, 16)),
            Pair(listOf(Point(28, 0), Point(207, 13), Point(180, 137), Point(0, 117)), Point(34, 17))
        )
        val rawFrames = (0 until 6).map { i -> BuildImage.open(imgDir["capoo_draw/$i.png"]) }
        (0 until 2).forEach { i ->
            val (points, pos) = params[i]
            rawFrames[4 + i].paste(img.perspective(points), pos, below = true)
        }
        val frames = mutableListOf(rawFrames[0].image)

        repeat(4) {
            frames.add(rawFrames[1].image)
            frames.add(rawFrames[2].image)
        }
        frames.add(rawFrames[3].image)
        repeat(6) {
            frames.add(rawFrames[4].image)
            frames.add(rawFrames[5].image)
        }
        saveGif(frames, 0.1)
    }

    @Meme("推锅", "甩锅", help = "需要图片和文本")
    val passTheBuck: Maker = { images, texts ->
        val img = images[0].convert("RGBA").square().resize(SizeInt(27, 27))
        val locs = listOf(
            Point(2, 26), Point(10, 24), Point(15, 27), Point(17, 29), Point(10, 20), Point(2, 29), Point(3, 31), Point(1, 30)
        )
        val frames = (0 until 8).map {
            val frame = BuildImage.open(imgDir["pass_the_buck/$it.png"])
            val text = texts.ifBlank { "你写!" }
            kotlin.runCatching {
                frame.drawText(
                    listOf(0, 0, frame.width, 20), text, maxFontSize = 20, minFontSize = 10
                )
            }.onFailure {
                throw TextOverLengthException()
            }
            frame.paste(img, locs[it], alpha = true).image
        }
        saveGif(frames, 0.1)
    }

    @Meme("紧贴", "紧紧贴着")
    val tightly: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(SizeInt(640, 400), keepRatio = true)
        val locs = listOf(
            listOf(39, 169, 267, 141), listOf(40, 167, 264, 143), listOf(38, 174, 270, 135),
            listOf(40, 167, 264, 143), listOf(38, 174, 270, 135), listOf(40, 167, 264, 143),
            listOf(38, 174, 270, 135), listOf(40, 167, 264, 143), listOf(38, 174, 270, 135),
            listOf(28, 176, 293, 134), listOf(5, 215, 333, 96), listOf(10, 210, 321, 102),
            listOf(3, 210, 330, 104), listOf(4, 210, 328, 102), listOf(4, 212, 328, 100),
            listOf(4, 212, 328, 100), listOf(4, 212, 328, 100), listOf(4, 212, 328, 100),
            listOf(4, 212, 328, 100), listOf(29, 195, 285, 120)
        )
        val frames = (0 until 20).map {
            val frame = BuildImage.open(imgDir["tightly/$it.png"])
            val (x, y, w, h) = locs[it]
            frame.paste(img.resize(SizeInt(w, h)), Point(x, y), below = true).image
        }
        saveGif(frames, 0.08)
    }

    @Meme("安全感")
    val safeSense: Maker = { images, texts ->
        val img = images[0].convert("RGBA").resize(SizeInt(215, 343), keepRatio = true)
        val frame = BuildImage.open(imgDir["safe_sense/0.png"])
        frame.paste(img, Point(215, 135))

        val text = texts.ifBlank { "你给我的安全感\n远不及TA的万分之一" }
        kotlin.runCatching {
            frame.drawText(
                listOf(30, 0, 400, 130),
                text,
                maxFontSize = 50,
                allowWrap = true,
                linesAlign = HorizontalAlign.CENTER,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.saveJpg()

    }

    @Meme("草神啃")
    val caoshenBite: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(SizeInt(160, 140), keepRatio = true)
        val locs = listOf(
            listOf(123, 356, 158, 124), listOf(123, 356, 158, 124), listOf(123, 355, 158, 125),
            listOf(122, 352, 159, 128), listOf(122, 350, 159, 130), listOf(122, 348, 159, 132),
            listOf(122, 345, 159, 135), listOf(121, 343, 160, 137), listOf(121, 342, 160, 138),
            listOf(121, 341, 160, 139), listOf(121, 341, 160, 139), listOf(121, 342, 160, 138),
            listOf(121, 344, 160, 136), listOf(121, 346, 160, 134), listOf(122, 349, 159, 131),
            listOf(122, 351, 159, 129), listOf(122, 353, 159, 127), listOf(123, 355, 158, 125)
        )
        val frames = (0 until 38).map {
            val frame = BuildImage.open(imgDir["caoshen_bite/$it.png"])
            val (x, y, w, h) = locs[it % locs.size]
            frame.paste(img.resize(SizeInt(w, h)), Point(x, y), below = true).image
        }
        saveGif(frames, 0.1)

    }

    @Meme("加载中")
    val loading: Maker = { images, _ ->
        val imgBig = images[0].convert("RGBA").resizeWidth(500).filter(GaussianBlurFilter(3.0))
        val h1 = imgBig.height
        val mask = BuildImage.new("RGBA", imgBig.size, RGBA(0, 0, 0, 32))
        val icon = BuildImage.open(imgDir["loading/icon.png"])
        imgBig.paste(mask, alpha = true).paste(icon, Point(200, (h1 / 2) - 50), alpha = true)

        makeJpgOrGif(images[0]) {
            val imgSmall = convert("RGBA").resizeWidth(100)
            val h2 = max(imgSmall.height, 80)
            val frame = BuildImage.new("RGBA", SizeInt(500, h1 + h2 + 10), Colors.WHITE)
            frame.paste(imgBig, alpha = true).paste(
                imgSmall, Point(100, h1 + 5 + (h2 - imgSmall.height) / 2), alpha = true
            )
            frame.drawText(
                listOf(210, h1 + 5, 480, h1 + h2 + 5), "不出来", hAlign = HorizontalAlign.LEFT, maxFontSize = 60
            )
            frame
        }

    }

    @Meme("拍头")
    val beatHead: Maker = { images, texts ->
        val text = texts.ifBlank { "怎么说话的你" }
        val img = images[0].convert("RGBA")
        val locs = listOf(listOf(160, 121, 76, 76), listOf(172, 124, 69, 69), listOf(208, 166, 52, 52))
        val frames = (0 until 3).map {
            val (x, y, w, h) = locs[it]
            val head = img.resize(SizeInt(w, h), keepRatio = true).circle()
            val frame = BuildImage.open(imgDir["beat_head/$it.png"])
            frame.paste(head, Point(x, y), below = true)
            kotlin.runCatching {
                frame.drawText(
                    listOf(175, 28, 316, 82),
                    text,
                    maxFontSize = 50,
                    minFontSize = 10,
                    allowWrap = true,
                )
            }.onFailure {
                throw TextOverLengthException()
            }
            frame.image
        }
        saveGif(frames, 0.05)

    }

    @Meme("等价无穷小")
    val lim_x_0: Maker = { images, _ ->
        val img = images[0]
        val frame = BuildImage.open(imgDir["lim_x_0/0.png"])
        val imgC = img.convert("RGBA").circle().resize(SizeInt(72, 72))
        val imgTp = img.convert("RGBA").circle().resize(SizeInt(51, 51))
        frame.paste(imgTp, Point(948, 247), alpha = true)
        val locs = listOf(
            Point(143, 32), Point(155, 148), Point(334, 149), Point(275, 266), Point(486, 266), Point(258, 383),
            Point(439, 382), Point(343, 539), Point(577, 487), Point(296, 717), Point(535, 717), Point(64, 896),
            Point(340, 896), Point(578, 897), Point(210, 1038), Point(644, 1039), Point(64, 1192), Point(460, 1192),
            Point(698, 1192), Point(1036, 141), Point(1217, 141), Point(1243, 263), Point(1140, 378), Point(1321, 378),
            Point(929, 531), Point(1325, 531), Point(1592, 531), Point(1007, 687), Point(1390, 687), Point(1631, 686),
            Point(1036, 840), Point(1209, 839), Point(1447, 839), Point(1141, 1018), Point(1309, 1019), Point(1546, 1019),
            Point(1037, 1197), Point(1317, 1198), Point(1555, 1197)
        )
        (0 until 39).forEach {
            val (x, y) = locs[it]
            frame.paste(imgC, Point(x, y), alpha = true)
        }
        frame.saveJpg()
    }

    @Meme("击剑", "🤺", help = "需要两张图片")
    val fencing: Maker = { images, _ ->
        val selfHead = images[0].convert("RGBA").circle().resize(SizeInt(27, 27))
        val userHead = images[1].convert("RGBA").circle().resize(SizeInt(27, 27))
        val userLocs = listOf(
            Point(57, 4), Point(55, 5), Point(58, 7), Point(57, 5), Point(53, 8), Point(54, 9), Point(64, 5), Point(66, 8),
            Point(70, 9), Point(73, 8), Point(81, 10), Point(77, 10), Point(72, 4), Point(79, 8), Point(50, 8), Point(60, 7),
            Point(67, 6), Point(60, 6), Point(50, 9)
        )
        val selfLocs = listOf(
            Point(10, 6), Point(3, 6), Point(32, 7), Point(22, 7), Point(13, 4), Point(21, 6), Point(30, 6), Point(22, 2),
            Point(22, 3), Point(26, 8), Point(23, 8), Point(27, 10), Point(30, 9), Point(17, 6), Point(12, 8), Point(11, 7),
            Point(8, 6), Point(-2, 10), Point(4, 9)
        )
        val frames = (0 until 19).map {
            val frame = BuildImage.open(imgDir["fencing/$it.png"])
            frame.paste(userHead, userLocs[it], alpha = true)
            frame.paste(selfHead, selfLocs[it], alpha = true).image
        }
        saveGif(frames, 0.05)

    }

    @Meme("贴", "贴贴,蹭,蹭蹭", help = "需要两张图片")
    val rub: Maker = { images, _ ->
        val selfHead = images[0].convert("RGBA").circle()
        val userHead = images[1].convert("RGBA").circle()
        val userLocs = listOf(
            listOf(39, 91, 75, 75), listOf(49, 101, 75, 75), listOf(67, 98, 75, 75),
            listOf(55, 86, 75, 75), listOf(61, 109, 75, 75), listOf(65, 101, 75, 75)
        )
        val selfLocs = listOf(
            listOf(102, 95, 70, 80, 0), listOf(108, 60, 50, 100, 0), listOf(97, 18, 65, 95, 0),
            listOf(65, 5, 75, 75, -20), listOf(95, 57, 100, 55, -70), listOf(109, 107, 65, 75, 0)
        )
        val frames = (0 until 6).map {
            val frame = BuildImage.open(imgDir["rub/$it.png"])
            userLocs[it].let { (x, y, w, h) ->
                frame.paste(userHead.resize(SizeInt(w, h)), Point(x, y), alpha = true)
            }
            selfLocs[it].let { (x, y, w, h, angle) ->
                frame.paste(
                    selfHead.resize(SizeInt(w, h)).rotate(angle.toDouble(), expand = true), Point(x, y), alpha = true
                )
            }
            frame.image
        }
        saveGif(frames, 0.05)

    }

    @Meme("关注", help = "需要文本和图片")
    val follow: Maker = { images, texts ->
        val img = images[0].circle().resize(SizeInt(200, 200))
        val name = texts.ifBlank { "男同" }

        val nameImg = Text2Image.fromText(name, 60).toImage()
        val followImg = Text2Image.fromText("关注了你", 60, fill = Colors.DIMGREY).toImage()
        val textWidth = max(nameImg.width, followImg.width)
        if (textWidth >= 1000) {
            throw TextOverLengthException()
        }

        val frame = BuildImage.new(
            "RGBA", SizeInt(300 + textWidth + 50, 300),
            RGBA(255, 255, 255, 0)
        )
        frame.paste(img, Point(50, 50), alpha = true)
        frame.paste(nameImg, Point(300, 135 - nameImg.height), alpha = true)
        frame.paste(followImg, Point(300, 145), alpha = true)
        frame.saveJpg()
    }

    @Meme("采访")
    val interview: Maker = { images, texts ->
        var (selfImg, userImg) = if (images.size >= 2) {
            Pair(images[0], images[1])
        } else {
            Pair(BuildImage.open(imgDir["interview/huaji.png"]), images[0])
        }
        selfImg = selfImg.convert("RGBA").square().resize(SizeInt(124, 124))
        userImg = userImg.convert("RGBA").square().resize(SizeInt(124, 124))
        val text = texts.ifBlank { "采访大佬经验" }
        val frame = BuildImage.new("RGBA", SizeInt(600, 310), Colors.WHITE)
        val microphone = BuildImage.open(imgDir["interview/microphone.png"])
        frame.paste(microphone, Point(330, 103), alpha = true)
        frame.paste(selfImg, Point(419, 40), alpha = true)
        frame.paste(userImg, Point(57, 40), alpha = true)
        kotlin.runCatching {
            frame.drawText(listOf(20, 200, 580, 310), text, maxFontSize = 50, minFontSize = 20)
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.saveJpg()

    }

    @Meme("玩游戏")
    val playGame: Maker = { images, texts ->
        val text = texts.ifBlank { "来玩休闲游戏啊" }
        val frame = BuildImage.open(imgDir["play_game/0.png"])
        kotlin.runCatching {
            frame.drawText(
                listOf(20, frame.height - 70, frame.width - 20, frame.height),
                text,
                maxFontSize = 40,
                minFontSize = 25,
                strokeFill = Colors.WHITE,
                fontName = "Glow Sans SC Normal Regular",
                strokeRatio = 0.15,
            )
        }.onFailure {
            throw TextOverLengthException()
        }

        makeJpgOrGif(images[0]) {
            val points = listOf(Point(0, 5), Point(227, 0), Point(216, 150), Point(0, 165))
            val screen = (
                    convert("RGBA").resize(SizeInt(220, 160), keepRatio = true).perspective(points)
                    )
            frame.copy().paste(screen.rotate(9.0, expand = true), Point(161, 117), below = true)
        }
    }

    @Meme("远离")
    val keep_away: Maker = { images, texts ->
        var count = 0
        val frame = BuildImage.new("RGB", SizeInt(400, 290), Colors.WHITE)
        val trans: suspend (BuildImage, Int) -> BuildImage = { image, n ->
            val img = image.convert("RGBA").square().resize(SizeInt(100, 100))
            if (n < 4) {
                img.rotate(n * 90.0)
            } else {
                img.image.flipX().toBuildImage().rotate((n - 4) * 90.0)
            }
        }

        val paste: (BuildImage) -> Unit = { img ->
            val y = if (count < 4) 90 else 190
            frame.paste(img, Point((count % 4) * 100, y))
            count += 1
        }

        val text = texts.ifBlank { "如何提高社交质量 : \n远离以下头像的人" }
        frame.drawText(listOf(10, 10, 390, 80), text, maxFontSize = 40, hAlign = HorizontalAlign.LEFT)
        val numPerUser = 8 / images.size
        images.forEach { image ->
            (0 until numPerUser).forEach { n ->
                paste(trans(image, n))
            }
        }
        val numLeft = 8 - numPerUser * images.size
        (0 until numLeft).forEach { n ->
            paste(trans(images.last(), n + numPerUser))
        }

        frame.saveJpg()

    }

    @Meme("咖波撞", "咖波头槌")
    val capoo_strike: Maker = { images, _ ->
        val params = listOf(
            Pair(listOf(Point(0, 4), Point(153, 0), Point(138, 105), Point(0, 157)), Point(28, 47)),
            Pair(listOf(Point(1, 13), Point(151, 0), Point(130, 104), Point(0, 156)), Point(28, 48)),
            Pair(listOf(Point(9, 10), Point(156, 0), Point(152, 108), Point(0, 155)), Point(18, 51)),
            Pair(listOf(Point(0, 21), Point(150, 0), Point(146, 115), Point(7, 145)), Point(17, 53)),
            Pair(listOf(Point(0, 19), Point(156, 0), Point(199, 109), Point(31, 145)), Point(2, 62)),
            Pair(listOf(Point(0, 28), Point(156, 0), Point(171, 115), Point(12, 154)), Point(16, 58)),
            Pair(listOf(Point(0, 25), Point(157, 0), Point(169, 113), Point(13, 147)), Point(18, 63))
        )

        makeGifOrCombinedGif(
            images[0], 7, 0.05, FrameAlignPolicy.ExtendLoop
        ) {
            val img = convert("RGBA").resize(SizeInt(200, 160), keepRatio = true)
            val (points, pos) = params[it]
            val frame = BuildImage.open(imgDir["capoo_strike/$it.png"])
            frame.paste(img.perspective(points).image, pos, below = true)
            frame
        }
    }

    @Meme("原神启动")
    val genshinStart: Maker = { images, texts ->
        val frame = BuildImage.open(imgDir["genshin_start/0.png"])
        val text = texts.ifBlank { "原神，启动！" }

        kotlin.runCatching {
            frame.drawText(
                listOf(100, frame.height - 150, frame.width - 100, frame.height),
                text,
                maxFontSize = 100,
                minFontSize = 70,
                fill = Colors.WHITE,
                strokeFill = Colors.BLACK,
                strokeRatio = 0.15,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        makeJpgOrGif(images[0]) {
            val points = listOf(Point(0, 116), Point(585, 0), Point(584, 319), Point(43, 385))
            val screen = (
                    convert("RGBA").resize(SizeInt(600, 330), keepRatio = true).perspective(points)
                    )
            frame.copy().paste(screen, Point(412, 121), below = true)
        }

    }

    @Meme("交个朋友", help = "需要文本")
    val make_friend: Maker = { images, texts ->
        val img = images[0].convert("RGBA")

        if (texts.isEmpty() || texts.first().isBlank()) {
            throw TextOrNameNotEnoughException()
        }
        val name = texts[0]

        val bg = BuildImage.open(imgDir["make_friend/0.png"])
        val frame = img.resizeWidth(1000)
        frame.paste(
            img.resizeWidth(250).rotate(9.0, expand = true),
            Point(743, frame.height - 155),
            alpha = true,
        )
        frame.paste(img.square().resize(SizeInt(55, 55)).rotate(9.0, expand = true),
            Point(836, frame.height - 278),
            alpha = true,
        )
        frame.paste(bg, Point(0, frame.height - 1000), alpha = true)

        val textImg = Text2Image.fromText(name, 20, fill = Colors.WHITE).toImage()
        if (textImg.width > 230) {
            throw TextOverLengthException()
        }

        frame.paste(textImg, Point(710, frame.height - 308), alpha = true)
        frame.saveJpg()
    }

    @Meme("最想要的东西")
    val whatHeWants: Maker = { images, texts ->
        val date = texts.ifBlank { "今年520" }
        val text = "${date}我会给你每个男人都最想要的东西···"
        val frame =
            BuildImage.open(imgDir["what_he_wants/0.png"])

        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resize(SizeInt(538, 538), keepRatio = true, inside = true)
            val newFrame = frame.copy()
            kotlin.runCatching {
                newFrame.drawText(
                    listOf(0, 514, 1024, 614),
                    text,
                    fill = Colors.BLACK,
                    maxFontSize = 80,
                    minFontSize = 20,
                    strokeRatio = 0.2,
                    strokeFill = Colors.WHITE,
                    vAlign = VerticalAlign.BOTTOM,
                )
            }.onFailure {
                throw TextOverLengthException()
            }
            newFrame.paste(img, Point(486, 616), alpha = true)
        }
    }
    @Meme("顶", "玩")
    val play: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square()
        val locs = listOf(
            listOf(180, 60, 100,  100), listOf(184, 75, 100,  100), listOf(183, 98, 100,  100),
            listOf(179, 118, 110,  100), listOf(156, 194, 150,  48), listOf(178, 136, 122,  69),
            listOf(175, 66, 122,  85), listOf(170, 42, 130,  96), listOf(175, 34, 118,  95),
            listOf(179, 35, 110,  93), listOf(180, 54, 102,  93), listOf(183, 58, 97,  92),
            listOf(174, 35, 120,  94), listOf(179, 35, 109,  93), listOf(181, 54, 101,  92),
            listOf(182, 59, 98,  92), listOf(183, 71, 90,  96), listOf(180, 131, 92,  101)
        )
        val rawFrames = (0 until 38).map {
            BuildImage.open(imgDir["play/$it.png"])
        }
        val imgFrames = mutableListOf<BuildImage>()
        (locs.indices).forEach { i ->
            val frame = rawFrames[i]
            val (x, y, w, h) = locs[i]
            frame.paste(img.resize(SizeInt(w,  h)), Point(x,  y),  below=true)
            imgFrames.add(frame)
        }
        val frames = imgFrames.subList(0, 12)
        frames.addAll(imgFrames.subList(0, 12))
        frames.addAll(imgFrames.subList(0, 8))
        frames.addAll(imgFrames.subList(12, 18))
        frames.addAll(rawFrames.subList(18, 38))
        saveGif(frames.map { it.image }, 0.06)
    }
    @Meme("摸", "摸摸,摸头,rua", help="可以附带参数 --circle 将图片变为圆形")
    val petpet: Maker = { images, texts ->
        var img = images[0].convert("RGBA").square()
        if (texts.firstOrNull() == "--circle") {
            img = img.circle()
        }

        val locs = listOf(
            listOf(14, 20, 98,  98),
            listOf(12, 33, 101,  85),
            listOf(8, 40, 110,  76),
            listOf(10, 33, 102,  84),
            listOf(12, 20, 98,  98)
        )
        val frames = (0 until 5).map {
            val hand = BuildImage.open(imgDir["petpet/$it.png"])
            val frame = BuildImage.new("RGBA", hand.size, RGBA(255, 255, 255, 0))
            val (x, y, w, h) = locs[it]
            frame.paste(img.resize(SizeInt(w,  h)).image, Point(x,  y),  alpha=true)
            frame.paste(hand,  alpha=true).image
        }
        saveGif(frames, 0.06)

    }
    @Meme("波纹")
    val wave: Maker = { images, _ ->
        val image = images[0]
        val imgW = min(max(image.width, 360), 720)
        val period = imgW / 6
        val amp = imgW / 60
        val frameNum = 8
        var phase = 0

        val sin: (Int) -> Double = { x ->
            amp * sin(2 * Math.PI / period * (x + phase)) / 2
        }
        makeGifOrCombinedGif(
            image, frameNum, 0.01, FrameAlignPolicy.ExtendLoop
        ) {
            val img = convert("RGBA").resizeHeight(imgW)
            val imgH = img.height
            var frame = img.copy()
            (0 until imgW).forEach { i ->
                (0 until imgH).forEach { j ->
                    val dx = (sin(i) * (imgH - j) / imgH).toInt()
                    val dy = (sin(j) * j / imgH).toInt()
                    if (i + dx in 0 until imgW && j + dy in 0 until imgH)
                        frame.image.setRgba(i, j, img.image.getRgba(i + dx,  j + dy))
                }
            }

            frame = frame.resizeCanvas(SizeInt(imgW - amp, imgH - amp))
            phase += period / frameNum
            frame
        }

    }
    @Meme("我老婆", "这是我老婆")
    val myWife: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resizeWidth(400)
        val imgW = img.width
        val imgH = img.height
        val frame = BuildImage.new("RGBA", SizeInt(650, imgH + 500), Colors.WHITE)
        frame.paste(img, Point((325 - imgW / 2),  105),  alpha=true)

        var text = "如果你的老婆长这样"
        frame.drawText(
            listOf(27, 12, 27 + 596,  12 + 79),
            text,
            maxFontSize=70,
            minFontSize=30,
            allowWrap=true
        )
        text = "那么这就不是你的老婆\n这是我的老婆"
        frame.drawText(
            listOf(27, imgH + 120, 27 + 593,  imgH + 120 + 135),
            text,
            maxFontSize=70,
            minFontSize=30,
            allowWrap=true
        )
        text = "滚去找你\n自己的老婆去"
        frame.drawText(
            listOf(27, imgH + 295, 27 + 374,  imgH + 295 + 135),
            text,
            maxFontSize=70,
            minFontSize=30,
            allowWrap=true
        )

        val imgPoint = BuildImage.open(imgDir["my_wife/1.png"]).resizeWidth(200)
        frame.paste(imgPoint, Point(421,  imgH + 270))

        frame.saveJpg()

    }
    @Meme("吴京xx中国xx", "吴京,吴京中国", help = "需要两段文本")
    val wujing: Maker = { _, texts ->
        val frame = BuildImage.open(imgDir["wujing/0.jpg"])
        val draw: suspend (List<Int>, String, HorizontalAlign) -> Unit = { pos, text, align ->
            kotlin.runCatching {
                frame.drawText(
                    pos,
                    text,
                    hAlign = align,
                    maxFontSize = 100,
                    minFontSize = 50,
                    fill = Colors.WHITE,
                    strokeFill = Colors.BLACK,
                    strokeRatio = 0.05,
                )
            }.onFailure {
                throw TextOverLengthException()
            }
        }

        val part1 = texts.getOrElse(0) { "不买华为不是" }
        draw(listOf(20, 560, 350, 690), part1, HorizontalAlign.RIGHT)
        val part2 = texts.getOrElse(1) { "人" }
        draw(listOf(610, 540, 917, 670), part2, HorizontalAlign.LEFT)
        frame.saveJpg()
    }
    @Meme("波奇手稿")
    val bocchiDraft: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(SizeInt(350, 400), keepRatio=true)
        val params = listOf(
            Pair(listOf(Point(54,  62), Point(353,  1), Point(379,  382), Point(1,  399)), Point(146,  173)),
            Pair(listOf(Point(54,  61), Point(349,  1), Point(379,  381), Point(1,  398)), Point(146,  174)),
            Pair(listOf(Point(54,  61), Point(349,  1), Point(379,  381), Point(1,  398)), Point(152,  174)),
            Pair(listOf(Point(54,  61), Point(335,  1), Point(379,  381), Point(1,  398)), Point(158,  167)),
            Pair(listOf(Point(54,  61), Point(335,  1), Point(370,  381), Point(1,  398)), Point(157,  149)),
            Pair(listOf(Point(41,  59), Point(321,  1), Point(357,  379), Point(1,  396)), Point(167,  108)),
            Pair(listOf(Point(41,  57), Point(315,  1), Point(357,  377), Point(1,  394)), Point(173,  69)),
            Pair(listOf(Point(41,  56), Point(309,  1), Point(353,  380), Point(1,  393)), Point(175,  43)),
            Pair(listOf(Point(41,  56), Point(314,  1), Point(353,  380), Point(1,  393)), Point(174,  30)),
            Pair(listOf(Point(41,  50), Point(312,  1), Point(348,  367), Point(1,  387)), Point(171,  18)),
            Pair(listOf(Point(35,  50), Point(306,  1), Point(342,  367), Point(1,  386)), Point(178,  14))
        )
        val idx = listOf(
            0, 0, 0, 0, 0, 0, 0, 1, 1, 2, 2, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10,
        )
        val frames = (0 until 23).map {
            val frame = BuildImage.open(imgDir["bocchi_draft/$it.png"])
            val (points, pos) = params[idx[it]]
            frame.paste(img.perspective(points), pos,  below=true).image
        }
        saveGif(frames, 0.08)
    }

    @Meme("小天使", help = "需要发送昵称和头像")
    val littleAngel: Maker = { images, texts ->
        val imgW = 500
        val imgH = images[0].heightIfResized(500)
        val frame = BuildImage.new("RGBA", SizeInt(600, imgH + 230), Colors.WHITE)
        var text = "非常可爱！简直就是小天使"
        frame.drawText(
            listOf(10, imgH + 120, 590,  imgH + 185), text, maxFontSize=48
        )

        val ta = if (texts.size >= 2 && texts[1].isNotBlank()) texts[1] else "她"
        text = "${ta}没失踪也没怎么样  我只是觉得你们都该看一下"
        frame.drawText(
            listOf(20, imgH + 180, 580,  imgH + 215), text, maxFontSize=26
        )

        val name = texts[0]
        text = "请问你们看到${name}了吗?"
        kotlin.runCatching {
            frame.drawText(
                listOf(20, 0, 580,  110), text, maxFontSize=70, minFontSize=25
            )
        }.onFailure {
            throw TextOverLengthException()
        }

        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resizeWidth(500)
            frame.copy().paste(img, Point((300 - imgW / 2),  110),  alpha=true)
        }
    }
    @Meme("看扁", help="可以自定义比例和文本。\n\t例：/生成 看扁 4.0\n\t例：/生成 看扁 0.5 可恶...被人看高了")
    val look_flat: Maker = { images, texts ->
        val text = if (texts.size >= 2 && texts[1].isNotBlank()) texts[1] else "可恶...被人看扁了"
        val ratio = if (texts.isNotEmpty()) texts[0].toDoubleOrNull() ?: 3.0 else 3.0

        val imgW = 500
        val textH = 80
        val textFrame = BuildImage.new("RGBA", SizeInt(imgW, textH), Colors.WHITE)
        kotlin.runCatching {
            textFrame.drawText(
                listOf(10, 0, imgW - 10,  textH),
                text,
                maxFontSize=55,
                minFontSize=30
            )
        }.onFailure {
            throw TextOverLengthException()
        }

        makeJpgOrGif(images[0]) {
            var img = convert("RGBA").resizeWidth(imgW)
            img = img.resize(SizeInt(imgW, (img.height / ratio).toInt()))
            val imgH = img.height
            val frame = BuildImage.new("RGBA", SizeInt(imgW, imgH + textH), Colors.WHITE)
            frame.paste(img,  alpha=true).paste(textFrame, Point(0,  imgH),  alpha=true)
        }

    }
    @Meme("咖波说")
    val capooSay: Maker = { _, rawTexts ->
        val capooSayOneLoop: suspend (String) -> List<Bitmap> =  { text ->
            val textFrame = BuildImage.new("RGBA", SizeInt(80, 80))
            kotlin.runCatching {
                textFrame.drawText(
                    listOf(0, 0, 80, 80),
                    text,
                    maxFontSize = 80,
                    minFontSize = 20,
                    allowWrap = true,
                    fontName = "FZKaTong-M19S",
                    linesAlign = HorizontalAlign.CENTER,
                )
            }.onFailure {
                throw TextOverLengthException()
            }

            val params = listOf(
                null,
                null,
                null,
                listOf(45, 45, 74, 112, 25),
                listOf(73, 73, 41, 42, 17),
                listOf(80, 80, 43, 36, 0),
                listOf(80, 80, 43, 30, 0),
                listOf(78, 78, 44, 30, 0),
                listOf(78, 78, 44, 29, 0),
                null,
            )

            (0 until 10).mapNotNull {
                val frame = BuildImage.open(imgDir["capoo_say/$it.png"])
                val param = params[it]
                if (param != null) {
                    val (x, y, w, h, angle) = param
                    frame.paste(
                        textFrame.resize(SizeInt(x, y)).rotate(angle.toDouble(), expand = true), Point(w, h), alpha = true
                    ).image
                } else {
                    null
                }
            }
        }
        val texts = if (rawTexts.isEmpty() || rawTexts.first().isBlank()) listOf("寄") else rawTexts
        val frames = texts.map { capooSayOneLoop(it) }.flatten()
        saveGif(frames, 0.1)

    }
    class HandArgs(parser: ArgParser) {
        val pos by parser.storing("枪的位置").default("left")
    }
    @Meme("手枪", help = "可以用 --pos <位置> 指定枪的位置，如left, right, both")
    val gun: Maker = { images, texts ->
        val frame = images[0].convert("RGBA").resize(SizeInt(500, 500), keepRatio = true)
        val gun = BuildImage.open(imgDir["gun/0.png"])
        val args = ArgParser(texts.toTypedArray()).parseInto(::HandArgs)
        val position = args.pos
        val left = position in listOf("left", "both")
        val right = position in listOf("right", "both")
        if (left) {
            frame.paste(gun, alpha = true)
        }
        if (right) {
            frame.paste(gun.image.toImmutableImage().flipX().toBitmap(), alpha = true)
        }
        frame.saveJpg()
    }
    @Meme("奶茶")
    val bubbleTea: Maker = { images, texts ->
        val frame = images[0].convert("RGBA").resize(SizeInt(500, 500), keepRatio = true)
        val bubbleTea = BuildImage.open(imgDir["bubble_tea/0.png"])
        val args = ArgParser(texts.toTypedArray()).parseInto(::HandArgs)
        val position = args.pos
        if (position in listOf("right", "both")) {
            frame.paste(bubbleTea, alpha = true)
        }
        if (position in listOf("left", "both")) {
            frame.paste(bubbleTea.image.toImmutableImage().flipX().toBitmap(), alpha = true)
        }
        frame.saveJpg()
    }
    @Meme("入典", "典中典,黑白草图")
    val dianzhongdian: Maker =  { images, rawTexts ->
        val maker: suspend (BuildImage, String, String) -> ByteArray = { image, text, trans ->
            val img = image.convert("L").resizeWidth(500)
            val textImg1 = BuildImage.new("RGBA", SizeInt(500, 60))
            val textImg2 = BuildImage.new("RGBA", SizeInt(500, 35))

            kotlin.runCatching {
                textImg1.drawText(
                    listOf(20, 0, textImg1.width - 20, textImg1.height),
                    text,
                    maxFontSize = 50,
                    minFontSize = 25,
                    fill = Colors.WHITE,
                )
            }.onFailure {
                throw TextOverLengthException()
            }

            kotlin.runCatching {
                textImg2.drawText(
                    listOf(20, 0, textImg2.width - 20, textImg2.height),
                    trans,
                    maxFontSize = 25,
                    minFontSize = 10,
                    fill = Colors.WHITE,
                )
            }.onFailure {
                throw TextOverLengthException()
            }

            val frame = BuildImage.new("RGBA", SizeInt(500, img.height + 100), Colors.BLACK)
            frame.paste(img, alpha = true)
            frame.paste(textImg1, Point(0, img.height), alpha = true)
            frame.paste(textImg2, Point(0, img.height + 60), alpha = true)
            frame.saveJpg()
        }

        val texts = if (rawTexts.isEmpty() || rawTexts.first().isBlank()) listOf("救命啊") else rawTexts
        val (text, trans) = if (texts.size == 1) {
            Pair(texts[0], LibreTranslate.translate(texts[0], LibreTranslate.Language.Auto, LibreTranslate.Language.Japanese))
        } else {
            Pair(texts[0], texts[1])
        }
        maker(images[0], text,  trans)
    }
    @Meme("唐可可举牌")
    val tankukuRaiseSign: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(SizeInt(300, 230), keepRatio = true)
        val params = listOf(
            Pair(listOf(Point(0, 46), Point(320, 0), Point(350, 214), Point(38, 260)), Point(68, 91)),
            Pair(listOf(Point(18, 0), Point(328, 28), Point(298, 227), Point(0, 197)), Point(184, 77)),
            Pair(listOf(Point(15, 0), Point(294, 28), Point(278, 216), Point(0, 188)), Point(194, 65)),
            Pair(listOf(Point(14, 0), Point(279, 27), Point(262, 205), Point(0, 178)), Point(203, 55)),
            Pair(listOf(Point(14, 0), Point(270, 25), Point(252, 195), Point(0, 170)), Point(209, 49)),
            Pair(listOf(Point(15, 0), Point(260, 25), Point(242, 186), Point(0, 164)), Point(215, 41)),
            Pair(listOf(Point(10, 0), Point(245, 21), Point(230, 180), Point(0, 157)), Point(223, 35)),
            Pair(listOf(Point(13, 0), Point(230, 21), Point(218, 168), Point(0, 147)), Point(231, 25)),
            Pair(listOf(Point(13, 0), Point(220, 23), Point(210, 167), Point(0, 140)), Point(238, 21)),
            Pair(listOf(Point(27, 0), Point(226, 46), Point(196, 182), Point(0, 135)), Point(254, 13)),
            Pair(listOf(Point(27, 0), Point(226, 46), Point(196, 182), Point(0, 135)), Point(254, 13)),
            Pair(listOf(Point(27, 0), Point(226, 46), Point(196, 182), Point(0, 135)), Point(254, 13)),
            Pair(listOf(Point(0, 35), Point(200, 0), Point(224, 133), Point(25, 169)), Point(175, 9)),
            Pair(listOf(Point(0, 35), Point(200, 0), Point(224, 133), Point(25, 169)), Point(195, 17)),
            Pair(listOf(Point(0, 35), Point(200, 0), Point(224, 133), Point(25, 169)), Point(195, 17))
        )
        val frames = (0 until 15).map {
            val (points, pos) = params[it]
            val frame = BuildImage.open(imgDir["tankuku_raisesign/$it.png"])
            frame.paste(img.perspective(points), pos, below = true).image
        }
        saveGif(frames, 0.2)
    }
    @Meme("万花筒", "万华镜,万花镜", help = "可以指定参数 --circle 来将图片变为圆形")
    val kaleidoscope: Maker = { images, texts ->
        val isCircle = texts.isNotEmpty() && texts[0] == "--circle"
        makeJpgOrGif(images[0]) {
            val circleNum = 10
            val imgPerCircle = 4
            var initAngle = 0.0
            val angleStep = 360.0 / imgPerCircle
            val radius: (Int) -> Int = { n ->
                n * 50 + 100
            }
            val cx = radius(circleNum)
            val cy = radius(circleNum)

            val img = convert("RGBA")
            val frame = BuildImage.new("RGBA", SizeInt(cx * 2, cy * 2), Colors.WHITE)
            (0 until circleNum).forEach { i ->
                val r = radius(i)
                val imgW = i * 35 + 100
                var im = img.resizeWidth(imgW)
                if (isCircle)
                    im = im.circle()
                (0 until imgPerCircle).forEach { j ->
                    val angle = initAngle + angleStep * j
                    val imRot = im.rotate(angle - 90, expand = true)
                    val x = (cx + r * cos(angle.degrees.radians) - imRot.width / 2.0).roundToInt()
                    val y = (cy - r * sin(angle.degrees.radians) - imRot.height / 2.0).roundToInt()
                    frame.paste(imRot, Point(x, y), alpha = true)
                }
                initAngle += angleStep / 2.0
            }
            frame
        }
    }
    @Meme("滚屏", help = "需要文本")
    val scroll: Maker = { _, texts ->
        val text = texts.ifBlank { "你们说话啊" }
        val text2image = Text2Image.fromText(text, 40).wrap(600.0)
        if (text2image.lines.size > 5) {
            throw TextOverLengthException()
        }
        val textImg = text2image.toImage()
        val textW = textImg.width
        val textH = textImg.height

        val boxW = textW + 140
        val boxH = max(textH + 103, 150)
        val box = BuildImage.new("RGBA", SizeInt(boxW, boxH), "#eaedf4".hexToRGBA())
        val corner1 = BuildImage.open(imgDir["scroll/corner1.png"])
        val corner2 = BuildImage.open(imgDir["scroll/corner2.png"])
        val corner3 = BuildImage.open(imgDir["scroll/corner3.png"])
        val corner4 = BuildImage.open(imgDir["scroll/corner4.png"])
        box.paste(corner1, Point(0,  0))
        box.paste(corner2, Point(0,  boxH - 75))
        box.paste(corner3, Point(textW + 70,  0))
        box.paste(corner4, Point(textW + 70,  boxH - 75))
        box.paste(BuildImage.new("RGBA", SizeInt(textW,  boxH - 40),  Colors.WHITE), Point(70,  20))
        box.paste(BuildImage.new("RGBA", SizeInt(textW + 88,  boxH - 150),  Colors.WHITE), Point(27,  75))
        box.paste(textImg, Point(70, 17 + (boxH - 40 - textH) / 2),  alpha=true)

        val dialog = BuildImage.new("RGBA", SizeInt(boxW, boxH * 4), "#eaedf4".hexToRGBA())
        (0 until 4).forEach { i ->
            dialog.paste(box, Point(0,  boxH * i))
        }

        val num = 30
        val dy = dialog.height / num
        val frames = (0 until num).map { i ->
            val frame = BuildImage.new("RGBA", dialog.size)
            frame.paste(dialog, Point(0,  -dy * i))
            frame.paste(dialog, Point(0,  dialog.height - dy * i)).image
        }
        saveGif(frames, 0.05)

    }
    @Meme("看书")
    val readBook: Maker = { images, texts ->
        val frame = BuildImage.open(imgDir["read_book/0.png"])
        val points = listOf(Point(0, 108), Point(1092, 0), Point(1023, 1134), Point(29, 1134))
        val img = images[0].convert("RGBA")
            .resize(SizeInt(1000, 1100), keepRatio = true, direction = BuildImage.DirectionType.North)
        val cover = img.perspective(points)
        frame.paste(cover, Point(1138, 1172), below = true)

        val chars = if (texts.isEmpty() || texts.first().isBlank()) "エロ本" else texts.joinToString(" ")

        val pieces = chars.map { char ->
            val piece = BuildImage(
                Text2Image.fromText(char.toString(), 150, fill = Colors.WHITE).toImage()
            )
            if (Regex("[a-zA-Z0-9\\s]").matches(char.toString()))
                piece.rotate(-90.0, expand = true)
            else
                piece.resizeCanvas(SizeInt(piece.width, piece.height - 40), BuildImage.DirectionType.North)
        }
        var w = pieces.maxOf { it.width }
        var h = pieces.sumOf { it.height }
        if (w > 265 || h > 3000) {
            throw TextOverLengthException()
        }

        var textImg = BuildImage.new("RGBA", SizeInt(w, h))
        h = 0
        pieces.forEach { piece ->
            textImg.paste(piece, Point((w - piece.width) / 2, h), alpha = true)
            h += piece.height
        }
        if (h > 780) {
            val ratio = 780.0 / h
            textImg = textImg.resize(SizeInt((w * ratio).toInt(), (h * ratio).toInt()))
        }
        w = textImg.width
        h = textImg.height
        frame.paste(textImg, Point(870 + (240 - w) / 2, 1500 + (780-h) / 2), alpha = true)

        frame.saveJpg()
    }
    @Meme("狗都不玩", help = "可以指定参数 --circle 来将图片变为圆形")
    val dogDislike: Maker = { images, texts ->
        val isCircle = texts.isNotEmpty() && texts[0] == "--circle"
        val location = listOf(
            Point(36, 408), Point(36, 410), Point(40, 375), Point(40, 355), Point(36, 325), Point(28, 305), Point(28, 305),
            Point(28, 305), Point(28, 305), Point(28, 285), Point(28, 285), Point(28, 285), Point(28, 285), Point(28, 290),
            Point(30, 295), Point(30, 300), Point(30, 300), Point(30, 300), Point(30, 300), Point(30, 300), Point(30, 300),
            Point(28, 298), Point(26, 296), Point(24, 294), Point(28, 294), Point(26, 294), Point(24, 294), Point(35, 294),
            Point(115, 330), Point(150, 355), Point(180, 420), Point(180, 450), Point(150, 450), Point(150, 450)
        )
        var head = images[0].convert("RGBA").resize(SizeInt(122, 122), keepRatio=true)
        if (isCircle) {
            head = head.circle()
        }
        val frames = (0 until 34).map {
            val frame = BuildImage.open(imgDir["dog_dislike/$it.png"])
            frame.paste(head, location[it],  alpha=true).image
        }
        saveGif(frames, 0.08)
    }
    @Meme("闪瞎", "可以自定义文本")
    val flashBlind: Maker = { images, texts ->
        val img = images[0].convert("RGB").resizeWidth(500)
        val frames = mutableListOf<BuildImage>()
        frames.add(img)
        frames.add(img.filter(InvertFilter()))
        val imgEnlarge = img.resizeCanvas(SizeInt(450, img.height * 450 / 500)).resize(SizeInt(500,  img.height))
        frames.add(imgEnlarge)
        frames.add(img.filter(InvertFilter()))

        val text = texts.ifBlank { "闪瞎你们的狗眼" }
        val textH = 65

        kotlin.runCatching {
            val textFrameBlack = BuildImage.new("RGB", SizeInt(500, textH), Colors.BLACK)
            val textFrameWhite = BuildImage.new("RGB", SizeInt(500, textH), Colors.WHITE)
            textFrameBlack.drawText(
                listOf(10, 0, 490,  textH),
                text,
                maxFontSize=50,
                minFontSize=20,
                fill= Colors.WHITE,
            )
            textFrameWhite.drawText(
                listOf(10, 0, 490,  textH),
                text,
                maxFontSize=50,
                minFontSize=20,
                fill= Colors.BLACK,
            )
            frames[0].paste(textFrameBlack.image, Point(0,  img.height - textH))
            frames[1].paste(textFrameWhite.image, Point(0,  img.height - textH))
            frames[2].paste(textFrameBlack.image, Point(0,  img.height - textH))
            frames[3].paste(textFrameWhite.image, Point(0,  img.height - textH))
        }.onFailure {
            throw TextOverLengthException()
        }

        saveGif(frames.map { it.image }, 0.03)
    }
    @Meme("打穿", "打穿屏幕")
    val hit_screen: Maker = { images, _ ->
        val params = listOf(
            Pair(listOf(Point(1,  10), Point(138,  1), Point(140,  119), Point(7,  154)), Point(32,  37)),
            Pair(listOf(Point(1,  10), Point(138,  1), Point(140,  121), Point(7,  154)), Point(32,  37)),
            Pair(listOf(Point(1,  10), Point(138,  1), Point(139,  125), Point(10,  159)), Point(32,  37)),
            Pair(listOf(Point(1,  12), Point(136,  1), Point(137,  125), Point(8,  159)), Point(34,  37)),
            Pair(listOf(Point(1,  9), Point(137,  1), Point(139,  122), Point(9,  154)), Point(35,  41)),
            Pair(listOf(Point(1,  8), Point(144,  1), Point(144,  123), Point(12,  155)), Point(30,  45)),
            Pair(listOf(Point(1,  8), Point(140,  1), Point(141,  121), Point(10,  155)), Point(29,  49)),
            Pair(listOf(Point(1,  9), Point(140,  1), Point(139,  118), Point(10,  153)), Point(27,  53)),
            Pair(listOf(Point(1,  7), Point(144,  1), Point(145,  117), Point(13,  153)), Point(19,  57)),
            Pair(listOf(Point(1,  7), Point(144,  1), Point(143,  116), Point(13,  153)), Point(19,  57)),
            Pair(listOf(Point(1,  8), Point(139,  1), Point(141,  119), Point(12,  154)), Point(19,  55)),
            Pair(listOf(Point(1,  13), Point(140,  1), Point(143,  117), Point(12,  156)), Point(16,  57)),
            Pair(listOf(Point(1,  10), Point(138,  1), Point(142,  117), Point(11,  149)), Point(14,  61)),
            Pair(listOf(Point(1,  10), Point(141,  1), Point(148,  125), Point(13,  153)), Point(11,  57)),
            Pair(listOf(Point(1,  12), Point(141,  1), Point(147,  130), Point(16,  150)), Point(11,  60)),
            Pair(listOf(Point(1,  15), Point(165,  1), Point(175,  135), Point(1,  171)), Point(-6,  46))
        )

        makeGifOrCombinedGif(
            images[0], 29, 0.2, FrameAlignPolicy.ExtendFirst
        ) {
            val img = convert("RGBA").resize(SizeInt(140, 120), keepRatio=true)
            val frame = BuildImage.open(imgDir["hit_screen/$it.png"])
            if (it in 6 until 22) {
                val (points, pos) = params[it - 6]
                frame.paste(img.perspective(points), pos, below = true)
            }
            frame
        }
    }
    @Meme("咖波撕")
    val capooRip: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(SizeInt(150, 100), keepRatio=true)
        val imgLeft = img.crop(listOf(0, 0, 75, 100))
        val imgRight = img.crop(listOf(75, 0, 150, 100))
        val params1 = listOf(
            Pair(Point(61,  196), listOf(Point(140,  68), Point(0,  59), Point(33,  0), Point(165,  8))),
            Pair(Point(63,  196), listOf(Point(136,  68), Point(0,  59), Point(29,  0), Point(158,  13))),
            Pair(Point(62,  195), listOf(Point(137,  72), Point(0,  58), Point(27,  0), Point(167,  11))),
            Pair(Point(95,  152), listOf(Point(0,  8), Point(155,  0), Point(163,  107), Point(13,  112))),
            Pair(Point(108,  129), listOf(Point(0,  6), Point(128,  0), Point(136,  113), Point(10,  117))),
            Pair(Point(84,  160), listOf(Point(0,  6), Point(184,  0), Point(190,  90), Point(10,  97)))
        )
        val params2 = listOf(
            Pair(
                Pair(Point(78,  158), listOf(Point(0,  3), Point(86,  0), Point(97,  106), Point(16,  106))),
                Pair(Point(195,  156), listOf(Point(0,  4), Point(82,  0), Point(85,  106), Point(15,  110)))
            ),
            Pair(
                Pair(Point(89,  156), listOf(Point(0,  0), Point(80,  0), Point(94,  100), Point(14,  100))),
                Pair(Point(192,  151), listOf(Point(0,  7), Point(79,  3), Point(82,  107), Point(11,  112)))
            )
        )
        val rawFrames = (0 until 8).map { BuildImage.open(imgDir["capoo_rip/$it.png"]) }
        (0 until 6).forEach {
            val (pos, points) = params1[it]
            rawFrames[it].paste(img.perspective(points), pos,  below=true)
        }
        (0 until 2).forEach {
            val (a, b) = params2[it]
            val (pos1, points1) = a
            val (pos2, points2) = b
            rawFrames[it + 6].paste(imgLeft.perspective(points1), pos1, below = true)
            rawFrames[it + 6].paste(imgRight.perspective(points2), pos2, below = true)
        }

        val newFrames = mutableListOf<BuildImage>()
        repeat(3) {
            newFrames.addAll(rawFrames.subList(0, 3))
            newFrames.addAll(rawFrames.subList(3, rawFrames.size))
        }
        saveGif(newFrames.map { it.image }, 0.1)
    }

    @Meme("我推的网友")
    val oshiNoKo: Maker = { images, texts ->
        val name = texts.ifBlank { "网友" }

        val textFrame1 = BuildImage.open(imgDir["oshi_no_ko/text1.png"])
        val textFrame2 = BuildImage.open(imgDir["oshi_no_ko/text2.png"])

        val biasY = 5
        val textFrame3 = BuildImage(
            Text2Image.fromText(
                name,
                fontName="HiraginoMin",
                fontSize=150,
                strokeWidth=8,
                strokeFill= Colors.WHITE
            ).toImage()
        ).resizeHeight(textFrame1.height + biasY)
        if (textFrame3.width > 800) {
            throw TextOverLengthException()
        }

        var textFrame = BuildImage.new(
            "RGBA",
            SizeInt(textFrame1.width + textFrame2.width + textFrame3.width,  textFrame2.height)
        )
        textFrame.paste(textFrame1, Point(0,  0),  alpha=true)
            .paste(textFrame3, Point(textFrame1.width,  biasY), alpha=true)
        textFrame = textFrame.resizeWidth(663)

        val background = BuildImage.open(imgDir["oshi_no_ko/background.png"])
        val foreground = BuildImage.open(imgDir["oshi_no_ko/foreground.png"])

        makePngOrGif(images[0]) {
            val img = convert("RGBA").resize(SizeInt(681, 692), keepRatio=true)
            background.copy()
                .paste(img,  alpha=true)
                .paste(textFrame, Point(9,  102 - textFrame.height / 2),  alpha=true)
                .paste(foreground,  alpha=true)
        }

    }
    @Meme("卡比锤", "卡比重锤", help = "可以指定参数 --circle 来将图片变为圆形")
    val kirbyHammer: Maker = { images, texts ->
        val isCircle = texts.isNotEmpty() && texts[0] == "--circle"
        val positions = listOf(
            Point(318, 163), Point(319, 173), Point(320, 183), Point(317, 193), Point(312, 199), Point(297, 212),
            Point(289, 218), Point(280, 224), Point(278, 223), Point(278, 220), Point(280, 215), Point(280, 213),
            Point(280, 210), Point(280, 206), Point(280, 201), Point(280, 192), Point(280, 188), Point(280, 184),
            Point(280, 179)
        )
        makeGifOrCombinedGif(
            images[0], 62, 0.05, FrameAlignPolicy.ExtendLoop
        ) {

            var img = convert("RGBA")
            if (isCircle)
                img = img.circle()
            img = img.resizeHeight(80)
            if (img.width < 80)
                img = img.resize(SizeInt(80, 80), keepRatio = true)
            val frame = BuildImage.open(imgDir["kirby_hammer/$it.png"])
            if (it <= 18) {
                var (x, y) = positions[it]
                x = x + 40 - img.width / 2
                frame.paste(img, Point(x, y),  alpha=true)
            } else if (it <= 39) {
                var (x, y) = positions[18]
                x = x + 40 - img.width / 2
                frame.paste(img, Point(x, y), alpha = true)
            }
            frame
        }
    }
    @Meme("yt", "youtube", help="需要两段文本")
    val youtube: Maker = { _, rawTexts ->
        val texts = if (rawTexts.isEmpty() || rawTexts.first().isBlank()) listOf("Bili", "Bili") else rawTexts
        val leftImg = Text2Image.fromText(texts[0], fontSize = 200, fill= Colors.BLACK)
            .toImage(bgColor= Colors.WHITE, padding=listOf(30,  20))

        val rightImg = BuildImage(Text2Image.fromText(
            texts[1], fontSize=200, fill= Colors.WHITE
        ).toImage(bgColor = RGBA(230, 33, 23), padding = listOf(50, 20))).run {
            resizeCanvas(
                SizeInt(max(width,  400),  height), bgColor= RGBA(230, 33,  23)
            ).circleCorner(height / 2.0)
        }

        var frame = BuildImage.new(
            "RGBA",
            SizeInt(leftImg.width + rightImg.width, max(leftImg.height,  rightImg.height)),
            Colors.WHITE
        )
        frame.paste(leftImg, Point(0,  frame.height - leftImg.height))
        frame = frame.resizeCanvas(SizeInt(frame.width + 100,  frame.height + 100), bgColor= Colors.WHITE)

        var corner = BuildImage.open(imgDir["youtube/corner.png"])
        val ratio = rightImg.height / 2.0 / corner.height
        corner = corner.resize(SizeInt((corner.width * ratio).toInt(), (corner.height * ratio).toInt()))
        val x0 = leftImg.width + 50
        val y0 = frame.height - rightImg.height - 50
        val x1 = frame.width - corner.width - 50
        val y1 = frame.height - corner.height - 50
        frame.paste(corner, Point(x0,  y0 - 1),  alpha=true).paste(
            corner.image.toImmutableImage().flipY().toBitmap(), Point(x0,  y1 + 1), alpha=true
        ).paste(
            corner.image.toImmutableImage().flipX().toBitmap(), Point(x1,  y0 - 1), alpha=true
        ).paste(
            corner.image.toImmutableImage().flipX().flipY().toBitmap(), Point(x1,  y1 + 1), alpha=true
        ).paste(
            rightImg, Point(x0,  y0), alpha=true
        )
        frame.saveJpg()
    }
    @Meme("小丑")
    val clown: Maker = { images, _ ->
        val avatar = images[0].convert("RGBA").resize(SizeInt(554, 442), keepRatio=true)
        val frame = BuildImage.open(imgDir["clown/circle.png"]).convert("RGBA")

        val imgSize = frame.size
        val bg = BuildImage.new("RGBA", imgSize, RGBA(255, 255, 255))

        val leftPart = avatar.crop(
            listOf(0, 0, avatar.width / 2,  avatar.height)
        ).rotate(26.0, expand = true)
        val rightPart = avatar.crop(
            listOf(avatar.width / 2, 0, avatar.width,  avatar.height)
        ).rotate(-26.0, expand = true)

        val imgW = bg.width
        val (leftCenterX, centerY) = Point(153, 341)
        val leftTopX = leftCenterX - leftPart.width / 2
        val topY = centerY - leftPart.height / 2
        val rightTopX = imgW - leftTopX - rightPart.width

        bg.paste(leftPart, Point(leftTopX,  topY),  alpha=true)
        bg.paste(rightPart, Point(rightTopX,  topY),  alpha=true)
        bg.paste(frame,  alpha=true)

        bg.savePng()

    }
    @Meme("拿捏", "戏弄")
    val tease: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square()
        val params = listOf(
            Pair(Point(21,  75), listOf(Point(0,  0), Point(129,  3), Point(155,  123), Point(12,  142))),
            Pair(Point(18,  73), listOf(Point(0,  29), Point(128,  0), Point(149,  118), Point(30,  147))),
            Pair(Point(22,  78), listOf(Point(0,  37), Point(136,  1), Point(160,  97), Point(16,  152))),
            Pair(Point(22,  58), listOf(Point(0,  58), Point(169,  1), Point(194,  92), Point(24,  170))),
            Pair(Point(43,  23), listOf(Point(0,  114), Point(166,  1), Point(168,  98), Point(41,  205))),
            Pair(Point(38,  24), listOf(Point(0,  112), Point(171,  0), Point(169,  113), Point(45,  195))),
            Pair(Point(31,  54), listOf(Point(0,  73), Point(148,  0), Point(172,  81), Point(45,  170))),
            Pair(Point(24,  62), listOf(Point(0,  62), Point(159,  1), Point(177,  81), Point(47,  155))),
            Pair(Point(31,  75), listOf(Point(1,  45), Point(126,  1), Point(158,  81), Point(29,  145))),
            Pair(Point(18,  61), listOf(Point(0,  63), Point(161,  1), Point(190,  88), Point(42,  153))),
            Pair(Point(20,  66), listOf(Point(0,  57), Point(152,  0), Point(195,  82), Point(40,  149))),
            Pair(Point(16,  77), listOf(Point(0,  41), Point(141,  0), Point(170,  90), Point(27,  138))),
            Pair(Point(28,  105), listOf(Point(0,  1), Point(132,  0), Point(131,  112), Point(1,  114))),
            Pair(Point(21,  107), listOf(Point(0,  1), Point(132,  0), Point(131,  112), Point(1,  114))),
            Pair(Point(11,  113), listOf(Point(1,  7), Point(138,  0), Point(141,  126), Point(4,  131))),
            Pair(Point(10,  114), listOf(Point(0,  0), Point(142,  0), Point(142,  131), Point(0,  131))),
            Pair(Point(5,  121), listOf(Point(0,  0), Point(147,  0), Point(147,  115), Point(0,  115))),
            Pair(Point(0,  119), listOf(Point(0,  0), Point(158,  0), Point(158,  102), Point(0,  102))),
            Pair(Point(0,  116), listOf(Point(0,  0), Point(158,  0), Point(158,  107), Point(0,  107))),
            Pair(Point(0,  119), listOf(Point(0,  0), Point(158,  0), Point(158,  103), Point(0,  101))),
            Pair(Point(2,  101), listOf(Point(0,  0), Point(153,  0), Point(153,  122), Point(0,  120))),
            Pair(Point(-18,  85), listOf(Point(61,  0), Point(194,  15), Point(143,  146), Point(0,  133))),
            Pair(Point(0,  66), listOf(Point(88,  1), Point(173,  17), Point(123,  182), Point(0,  131))),
            Pair(Point(0,  29), listOf(Point(118,  3), Point(201,  48), Point(111,  220), Point(1,  168)))
        )
        val frames = (0 until 24).map {
            val frame = BuildImage.open(imgDir["tease/$it.png"])
            val (pos, points) = params[it]
            frame.paste(img.perspective(points), pos,  below=true).image
        }

        saveGif(frames, 0.05)
    }
    @Meme("为什么要有手", help = "需要文本和图片")
    val why_have_hands: Maker = { images, texts ->
        val img = images[0].convert("RGBA")

        if (texts.isEmpty() || texts.first().isEmpty()) {
            throw TextOrNameNotEnoughException()
        }
        val name = texts[0]

        val frame = BuildImage.open(imgDir["why_have_hands/0.png"])
        frame.paste(img.circle().resize(SizeInt(250,  250)), Point(350,  670),  alpha=true)
        frame.paste(
            img.resize(SizeInt(250,  250),  keepRatio=true).rotate(15.0), Point(1001,  668), below=true
        )
        frame.paste(img.resize(SizeInt(250,  170),  keepRatio=true), Point(275,  1100),  below=true)
        frame.paste(
            img.resize(
                SizeInt(300,  400), keepRatio=true, inside=true,
                direction=BuildImage.DirectionType.Northwest),
            Point(1100,  1060),
            alpha=true
        )
        kotlin.runCatching {
            val textFrame = BuildImage.new("RGBA", SizeInt(600, 100)).drawText(
                listOf(0, 0, 600,  100),
                "摸摸$name!",
                maxFontSize=70,
                minFontSize=30,
                hAlign= HorizontalAlign.LEFT
            )
            frame.paste(textFrame.rotate(-15.0,  expand=true), Point(75,  825),  alpha=true)
            frame.drawText(
                listOf(840, 960, 1440,  1060),
                "托托$name!",
                maxFontSize=70,
                minFontSize=30,
            )
            frame.drawText(
                listOf(50, 1325, 650,  1475),
                "赞美$name!",
                maxFontSize=90,
                minFontSize=30,
                vAlign= VerticalAlign.TOP,
            )
            frame.drawText(
                listOf(700, 1340, 1075,  1490),
                "为${name}奉献所有财产!",
                maxFontSize=70,
                minFontSize=30,
                allowWrap=true,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.saveJpg()
    }
    @Meme("douyin", help = "需要文本")
    val douyin: Maker = { _, texts ->
        val text = texts[0]
        val fontsize = 200
        val offset = (fontsize * 0.05).roundToInt()
        val px = 70
        val py = 30
        val bgColor = "#1C0B1B".hexToRGBA()
        val image = Text2Image.fromText(
            text, fontsize, fill="#FF0050".hexToRGBA(), strokeFill="#FF0050".hexToRGBA(), strokeWidth=5
        ).toImage(bgColor = bgColor, padding = listOf(px + offset * 2, py + offset * 2, px, py))
        Text2Image.fromText(
            text, fontsize, fill="#00F5EB".hexToRGBA(), strokeFill="#00F5EB".hexToRGBA(), strokeWidth=5
        ).drawOnImage(image, Point(px.toDouble(), py.toDouble()))
        Text2Image.fromText(
            text, fontsize, fill= Colors.WHITE, strokeFill= Colors.WHITE, strokeWidth=5
        ).drawOnImage(image, Point((px + offset).toDouble(), (py + offset).toDouble()))
        val frame = BuildImage(image)

        val width = frame.width - px
        val height = frame.height - py
        val frameNum = 10
        val devideNum = 6
        val seed = 20 * 0.05
        val frames = (0 until frameNum).map {
            var newFrame = frame.copy()
            var hSeeds = (0 until devideNum).map {
                (sin(Random.nextDouble() * devideNum)).absoluteValue
            }
            val hSeedSum = hSeeds.sum()
            hSeeds = hSeeds.map { s ->
                s / hSeedSum
            }
            val direction = 1
            var lastYn = 0
            var lastH = 0
            (0 until devideNum).forEach { i ->
                val yn = lastYn + lastH
                val h = max((height * hSeeds[i]).roundToInt(), 2)
                lastYn = yn
                lastH = h
                val piece = newFrame.copy().crop(listOf(px, yn, px + width, yn + h))
                newFrame.paste(piece, Point(px + (i * direction * seed).roundToInt(),  yn))
            }
            val moveX = 64
            val points = listOf(
                Point(moveX,  0),
                Point(newFrame.width + moveX,  0),
                Point(newFrame.width,  newFrame.height),
                Point(0,  newFrame.height)
            )
            newFrame = newFrame.perspective(points)
            val bg = BuildImage.new("RGBA", newFrame.size, bgColor)
            bg.paste(newFrame,  alpha=true).image
        }

        saveGif(frames, 0.2)
    }
    @Meme("奖状", "证书", "支持3~4段参数")
    val certificate: Maker = { _, rawTexts ->
        val texts = if (rawTexts.size < 3) listOf("小王", "优秀学生", "一年一班") else rawTexts
        val time = LocalDateTime.now()
        val frame = BuildImage.open(imgDir["certificate/0.png"])

        kotlin.runCatching {
            frame.drawText(
                listOf(340, 660, 770,  800),
                texts[0],
                allowWrap=false,
                maxFontSize=80,
                minFontSize=20,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        kotlin.runCatching {
            frame.drawText(
                listOf(565, 1040, 2100,  1320),
                texts[1],
                fill= Colors.RED,
                allowWrap=true,
                maxFontSize=120,
                minFontSize=60,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        kotlin.runCatching {
            frame.drawText(
                listOf(1500, 1400, 2020,  1520),
                texts[2],
                allowWrap=false,
                maxFontSize=60,
                minFontSize=20,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        kotlin.runCatching {
            frame.drawText(
                listOf(450, 850, 2270,  1080),
                if (texts.size >= 4) texts[3]  else "　　在本学年第一学期中表现优秀，被我校决定评为",
                allowWrap=true,
                maxFontSize=80,
                minFontSize=40,
                hAlign= HorizontalAlign.LEFT,
                vAlign= VerticalAlign.TOP,
            )
        }.onFailure {
            throw TextOverLengthException()
        }

        frame.drawText(
            listOf(1565,  1527),
            "%04d".format(time.year),
            allowWrap=false,
            fontSize=60,
        )
        frame.drawText(
            listOf(1752,  1527),
            "%02d".format(time.monthValue),
            allowWrap=false,
            fontSize=60,
        )
        frame.drawText(
            listOf(1865,  1527),
            "%02d".format(time.dayOfMonth),
            allowWrap=false,
            fontSize=60,
        )

        frame.savePng()
    }
    @Meme("可达鸭", help = "需要两段文本")
    val psyduck: Maker = { _, texts ->
        val leftImg = BuildImage.new("RGBA", SizeInt(155, 100))
        val rightImg = BuildImage.new("RGBA", SizeInt(155, 100))

        val draw: suspend (BuildImage, String) -> Unit = { frame, text ->
            kotlin.runCatching {
                frame.drawText(
                    listOf(5, 5, 150,  95),
                    text,
                    maxFontSize=80,
                    minFontSize=30,
                    allowWrap=true,
                    fontName="FZSJ-QINGCRJ",
                )
            }.onFailure {
                throw TextOverLengthException()
            }
        }

        draw(leftImg,  texts[0])
        draw(rightImg,  texts[1])

        val params = listOf(
            listOf("left", listOf(Point(0,  11), Point(154,  0), Point(161,  89), Point(20,  104)), Point(18,  42)),
            listOf("left", listOf(Point(0,  9), Point(153,  0), Point(159,  89), Point(20,  101)), Point(15,  38)),
            listOf("left", listOf(Point(0,  7), Point(148,  0), Point(156,  89), Point(21,  97)), Point(14,  23)),
            null,
            listOf("right", listOf(Point(10,  0), Point(143,  17), Point(124,  104), Point(0,  84)), Point(298,  18)),
            listOf("right", listOf(Point(13,  0), Point(143,  27), Point(125,  113), Point(0,  83)), Point(298,  30)),
            listOf("right", listOf(Point(13,  0), Point(143,  27), Point(125,  113), Point(0,  83)), Point(298,  26)),
            listOf("right", listOf(Point(13,  0), Point(143,  27), Point(125,  113), Point(0,  83)), Point(298,  30)),
            listOf("right", listOf(Point(13,  0), Point(143,  27), Point(125,  113), Point(0,  83)), Point(302,  20)),
            listOf("right", listOf(Point(13,  0), Point(141,  23), Point(120,  102), Point(0,  82)), Point(300,  24)),
            listOf("right", listOf(Point(13,  0), Point(140,  22), Point(118,  100), Point(0,  82)), Point(299,  22)),
            listOf("right", listOf(Point(9,  0), Point(128,  16), Point(109,  89), Point(0,  80)), Point(303,  23)),
            null,
            listOf("left", listOf(Point(0,  13), Point(152,  0), Point(158,  89), Point(17,  109)), Point(35,  36)),
            listOf("left", listOf(Point(0,  13), Point(152,  0), Point(158,  89), Point(17,  109)), Point(31,  29)),
            listOf("left", listOf(Point(0,  17), Point(149,  0), Point(155,  90), Point(17,  120)), Point(45,  33)),
            listOf("left", listOf(Point(0,  14), Point(152,  0), Point(156,  91), Point(17,  115)), Point(40,  27)),
            listOf("left", listOf(Point(0,  12), Point(154,  0), Point(158,  90), Point(17,  109)), Point(35,  28))
        )

        val frames = (0 until 18).map {
            val frame = BuildImage.open(imgDir["psyduck/$it.jpg"])
            val param = params[it]
            if (param != null) {
                val (side, points, pos) = param
                if (side == "left") {
                    frame.paste(leftImg.perspective(points as List<Point>), pos as Point,  alpha=true)
                } else if (side == "right") {
                    frame.paste(rightImg.perspective(points as List<Point>), pos as Point, alpha = true)
                }
            }
            frame.image
        }
        saveGif(frames, 0.2)
    }
    @Meme("追列车", "追火车")
    val chase_train: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(SizeInt(42, 42))
        val locs = listOf(
            listOf(35, 34, 128, 44), listOf(35, 33, 132, 40), listOf(33, 34, 133, 36), listOf(33, 38, 135, 41),
            listOf(34, 34, 136, 38), listOf(35, 35, 136, 33), listOf(33, 34, 138, 38), listOf(36, 35, 138, 34),
            listOf(38, 34, 139, 32), listOf(40, 35, 139, 37), listOf(36, 35, 139, 33), listOf(39, 36, 138, 28),
            listOf(40, 35, 138, 33), listOf(37, 34, 138, 31), listOf(43, 36, 135, 27), listOf(36, 37, 136, 32),
            listOf(38, 40, 135, 26), listOf(37, 35, 133, 26), listOf(33, 36, 132, 30), listOf(33, 39, 132, 25),
            listOf(32, 36, 131, 23), listOf(33, 36, 130, 31), listOf(35, 39, 128, 25), listOf(33, 35, 127, 23),
            listOf(34, 36, 126, 29), listOf(34, 40, 124, 25), listOf(39, 36, 119, 23), listOf(35, 36, 119, 32),
            listOf(35, 37, 116, 27), listOf(36, 38, 113, 23), listOf(34, 35, 113, 32), listOf(39, 36, 113, 23),
            listOf(36, 35, 114, 17), listOf(36, 38, 111, 13), listOf(34, 37, 114, 15), listOf(34, 39, 111, 10),
            listOf(33, 39, 109, 11), listOf(36, 35, 104, 17), listOf(34, 36, 102, 14), listOf(34, 35, 99, 14),
            listOf(35, 38, 96, 16), listOf(35, 35, 93, 14), listOf(36, 35, 89, 15), listOf(36, 36, 86, 18),
            listOf(36, 39, 83, 14), listOf(34, 36, 81, 16), listOf(40, 41, 74, 17), listOf(38, 36, 74, 15),
            listOf(39, 35, 70, 16), listOf(33, 35, 69, 20), listOf(36, 35, 66, 17), listOf(36, 35, 62, 17),
            listOf(37, 36, 57, 21), listOf(35, 39, 57, 15), listOf(35, 36, 53, 17), listOf(35, 38, 51, 20),
            listOf(37, 36, 47, 19), listOf(37, 35, 47, 18), listOf(40, 36, 43, 19), listOf(38, 35, 42, 22),
            listOf(40, 34, 38, 20), listOf(38, 34, 37, 21), listOf(39, 32, 35, 24), listOf(39, 33, 33, 22),
            listOf(39, 36, 32, 22), listOf(38, 35, 32, 25), listOf(35, 37, 31, 22), listOf(37, 37, 31, 23),
            listOf(36, 31, 31, 28), listOf(37, 34, 32, 25), listOf(36, 37, 32, 23), listOf(36, 33, 33, 30),
            listOf(35, 34, 33, 27), listOf(38, 33, 33, 28), listOf(37, 34, 33, 29), listOf(36, 35, 35, 28),
            listOf(36, 37, 36, 27), listOf(43, 39, 33, 30), listOf(35, 34, 38, 31), listOf(37, 34, 39, 30),
            listOf(36, 34, 40, 30), listOf(39, 35, 41, 30), listOf(41, 36, 41, 29), listOf(40, 37, 44, 32),
            listOf(40, 37, 45, 29), listOf(39, 38, 48, 28), listOf(38, 33, 50, 33), listOf(35, 38, 53, 28),
            listOf(37, 34, 54, 31), listOf(38, 34, 57, 32), listOf(41, 35, 57, 29), listOf(35, 34, 63, 29),
            listOf(41, 35, 62, 29), listOf(38, 35, 66, 28), listOf(35, 33, 70, 29), listOf(40, 39, 70, 28),
            listOf(36, 36, 74, 28), listOf(37, 35, 77, 26), listOf(37, 35, 79, 28), listOf(38, 35, 81, 27),
            listOf(36, 35, 85, 27), listOf(37, 36, 88, 29), listOf(36, 34, 91, 27), listOf(38, 39, 94, 24),
            listOf(39, 34, 95, 27), listOf(37, 34, 98, 26), listOf(36, 35, 103, 24), listOf(37, 36, 99, 28),
            listOf(34, 36, 97, 34), listOf(34, 38, 102, 38), listOf(37, 37, 99, 40), listOf(39, 36, 101, 47),
            listOf(36, 36, 106, 43), listOf(35, 35, 109, 40), listOf(35, 39, 112, 43), listOf(33, 36, 116, 41),
            listOf(36, 36, 116, 39), listOf(34, 37, 121, 45), listOf(35, 41, 123, 38), listOf(34, 37, 126, 35))
        val frames = (0 until 120).map {
            val frame = BuildImage.open(imgDir["chase_train/$it.png"])
            val (w, h, x, y) = locs[it]
            frame.paste(img.resize(SizeInt(w,  h)), Point(x,  y),  below=true).image
        }
        saveGif(frames, 0.05)
    }
}
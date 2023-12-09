@file:Suppress("unused", "UNCHECKED_CAST")

package xyz.xszq.bot.image

import com.sksamuel.scrimage.filter.BrightnessFilter
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.text.HorizontalAlign
import com.soywiz.korim.text.VerticalAlign
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korma.geom.Size
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
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
            val frame = BuildImage.new("RGBA", Size(500, h1 + h2 + 10), Colors.WHITE)
            frame.paste(imgBig, alpha = true).paste(
                imgSmall, Pair(290, h1 + 5 + (h2 - imgSmall.height) / 2), alpha = true
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
        val textFrame = BuildImage.new("RGBA", Size(500, frameH), Colors.WHITE)
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
        val maker: BuildImage.(Int) -> BuildImage = { i ->
            val now = resizeWidth(500)
            val baseFrame = textFrame.copy().paste(now, alpha = true)
            val frame = BuildImage.new("RGBA", baseFrame.size, Colors.WHITE)
            var r = coeff.pow(i)
            repeat(4) {
                val x = (358 * (1 - r)).roundToInt()
                val y = (frameH * (1 - r)).roundToInt()
                val w = (500 * r).roundToInt()
                val h = (frameH * r).roundToInt()
                frame.paste(baseFrame.resize(Size(w, h)), Pair(x, y))
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
                Size(246, 286), direction = BuildImage.DirectionType.North, bgColor = Colors.WHITE
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
            frame.copy().paste(resize(Size(91, 91), keepRatio = true), alpha = true)
        }
    }

    @Meme("一样")
    val alike: Maker = { images, _ ->
        val frame = BuildImage.new("RGBA", Size(470, 180), Colors.WHITE)
        frame.drawText(
            listOf(10, 10, 185, 140), "你怎么跟", maxFontSize = 40, minFontSize = 30, hAlign = HorizontalAlign.RIGHT
        ).drawText(
            listOf(365, 10, 460, 140), "一样", maxFontSize = 40, minFontSize = 30, hAlign = HorizontalAlign.LEFT
        )
        makeJpgOrGif(images[0]) {
            frame.copy().paste(resize(Size(150, 150), keepRatio = true), Pair(200, 15), alpha = true)
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
        frame.paste(img.resize(Size(350, 400), keepRatio = true, inside = true), Pair(25, 35), alpha = true)
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
            val ratio = min((frame.width - 40) / textW, 1.0)
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
                    image.resize(Size(350, 400), keepRatio = true, inside = true),
                    Pair(10 + Random.nextInt(0, 50), 20 + Random.nextInt(0, 70)),
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
                    val nameW = min(Text2Image.fromText(name, 70).width, 380.0)
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
        val img = images[0].convert("RGBA").resize(Size(450, 450), keepRatio = true)
        val frame = BuildImage.open(imgDir["anti_kidnap/0.png"])
        frame.paste(img, Pair(30, 78), below = true)
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
            val img = resize(Size(305, 235), keepRatio = true)
            frame.copy().paste(img, Pair(106, 72), below = true)
        }
    }

    @Meme("鼓掌")
    val applaud: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(Size(110, 110))
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
            frame.paste(img.resize(Size(w, h)), Pair(x, y), below = true)
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
        val img = images[0].convert("RGBA").square().resize(Size(458, 458))
        frame.paste(img.rotate(-5.0), Pair(531, 15), below = true)
        frame.saveJpg()
    }

    @Meme("抱紧")
    val holdTight: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(Size(159, 171), keepRatio = true)
        val frame = BuildImage.open(imgDir["hold_tight/0.png"])
        frame.paste(img, Pair(113, 205), below = true)
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
        val img = images[0].convert("RGBA").resize(Size(310, 460), keepRatio = true, inside = true)
        frame.paste(img, Pair(313, 64), alpha = true)
        frame.saveJpg()
    }

    @Meme("无响应")
    val noResponse: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(Size(1050, 783), keepRatio = true)
        val frame = BuildImage.open(imgDir["no_response/0.png"])
        frame.paste(img, Pair(0, 581), below = true)
        frame.saveJpg()
    }

    @Meme("像样的亲亲")
    val decentKiss: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(Size(589, 340), keepRatio = true)
        val frame = BuildImage.open(imgDir["decent_kiss/0.png"])
        frame.paste(img, Pair(0, 91), below = true)
        frame.saveJpg()
    }

    @Meme("凯露指")
    val karylPoint: Maker = { images, _ ->
        val img = images[0].convert("RGBA").rotate(7.5, expand = true).resize(Size(225, 225))
        val frame = BuildImage.open(imgDir["karyl_point/0.png"])
        frame.paste(img, Pair(87, 790), alpha = true)
        frame.savePng()
    }

    @Meme("这像画吗")
    val paint: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(Size(117, 135), keepRatio = true)
        val frame = BuildImage.open(imgDir["paint/0.png"])
        frame.paste(img.rotate(4.0, expand = true), Pair(95, 107), below = true)
        frame.saveJpg()
    }

    @Meme("为什么@我")
    val whyAtMe: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(Size(265, 265), keepRatio = true)
        val frame = BuildImage.open(imgDir["why_at_me/0.png"])
        frame.paste(img.rotate(19.0), Pair(42, 13), below = true)
        frame.saveJpg()
    }

    @Meme("精神支柱")
    val support: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(Size(815, 815)).rotate(23.0, expand = true)
        val frame = BuildImage.open(imgDir["support/0.png"])
        frame.paste(img, Pair(-172, -17), below = true)
        frame.saveJpg()
    }

    @Meme("加班")
    val overtime: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["overtime/0.png"])
        val img = images[0].convert("RGBA").resize(Size(250, 250), keepRatio = true)
        frame.paste(img.rotate(-25.0, expand = true), Pair(165, 220), below = true)
        frame.saveJpg()
    }

    @Meme("小画家")
    val painter: Maker = { images, _ ->
        val img = images[0].convert("RGBA")
            .resize(Size(240, 345), keepRatio = true, direction = BuildImage.DirectionType.North)
        val frame = BuildImage.open(imgDir["painter/0.png"])
        frame.paste(img, Pair(125, 91), below = true)
        frame.saveJpg()
    }

    @Meme("捂脸")
    val coverFace: Maker = { images, _ ->
        val points = listOf(Pair(15, 15), Pair(448, 0), Pair(445, 456), Pair(0, 465))
        val img = images[0].convert("RGBA").square().resize(Size(450, 450)).perspective(points)
        val frame = BuildImage.open(imgDir["cover_face/0.png"])
        frame.paste(img, Pair(120, 150), below = true)
        frame.saveJpg()
    }

    @Meme("木鱼")
    val woodenFish: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(Size(85, 85))
        val frames = (0 until 66).map { i ->
            BuildImage.open(imgDir["wooden_fish/$i.png"]).paste(img, Pair(116, 153), below = true).image
        }
        saveGif(frames, 0.1)
    }

    @Meme("丢", "扔")
    val throwBall: Maker = { images, _ ->
        val img = images[0]
            .convert("RGBA")
            .circle()
            .rotate(Random.nextInt(1, 360).toDouble())
            .resize(Size(143, 143))
        val frame = BuildImage.open(imgDir["throw/0.png"])
        frame.paste(img, Pair(15, 178), alpha = true)
        frame.saveJpg()
    }

    @Meme("怒撕")
    val ripAngrily: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(Size(105, 105))
        val frame = BuildImage.open(imgDir["rip_angrily/0.png"])
        frame.paste(img.rotate(-24.0, expand = true), Pair(18, 170), below = true)
        frame.paste(img.rotate(24.0, expand = true), Pair(163, 65), below = true)
        frame.saveJpg()
    }

    @Meme("继续干活", "打工人")
    val backToWork: Maker = { images, _ ->
        val img = images[0].convert("RGBA")
            .resize(Size(220, 310), keepRatio = true, direction = BuildImage.DirectionType.North)
        val frame = BuildImage.open(imgDir["back_to_work/0.png"])
        frame.paste(img.rotate(25.0, expand = true), Pair(56, 32), below = true)
        frame.saveJpg()
    }

    @Meme("嘲讽")
    val taunt: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["taunt/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").square().resize(Size(230, 230))
            frame.copy().paste(img, Pair(245, 245))
        }
    }

    @Meme("吃")
    val eat: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(Size(34, 34))
        val frames = (0 until 3).map { i ->
            BuildImage.open(imgDir["eat/$i.png"]).paste(img, Pair(2, 38), below = true).image
        }
        saveGif(frames, 0.05)
    }

    @Meme("白天黑夜", "白天晚上", help = "需要两张图片")
    val dayNight: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(Size(333, 360), keepRatio = true)
        val img1 = images[1].convert("RGBA").resize(Size(333, 360), keepRatio = true)
        val frame = BuildImage.open(imgDir["daynight/0.png"])
        frame.paste(img, Pair(349, 0))
        frame.paste(img1, Pair(349, 361))
        frame.saveJpg()
    }

    @Meme("需要", "你可能需要")
    val need: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["need/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").square().resize(Size(115, 115))
            frame.copy().paste(img, Pair(327, 232), below = true)
        }
    }

    @Meme("想什么")
    val thinkWhat: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["think_what/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resize(Size(534, 493), keepRatio = true)
            frame.copy().paste(img, Pair(530, 0), below = true)
        }
    }

    @Meme("胡桃平板")
    val walnutPad: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["walnut_pad/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resize(Size(540, 360), keepRatio = true)
            frame.copy().paste(img, Pair(368, 65), below = true)
        }
    }

    @Meme("恐龙", "小恐龙")
    val dinosaur: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["dinosaur/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resize(Size(680, 578), keepRatio = true)
            frame.copy().paste(img, Pair(294, 369), below = true)
        }
    }

    @Meme("震惊")
    val shock: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(Size(300, 300))
        val frames = (0 until 30).map { _ ->
            NativeImage(300, 300).modify {
                fillStyle = Colors.WHITE
                fillRect(0.0, 0.0, 300.0, 300.0)
                drawImage(
                    img.motionBlur(Random.nextInt(-90, 90).toDouble(), Random.nextInt(0, 50))
                        .rotate(Random.nextInt(-20, 20).toDouble()).image, 0, 0
                )
            }
        }
        saveGif(frames, 0.01)
    }

    @Meme("不要靠近")
    val dontGoNear: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["dont_go_near/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resize(Size(170, 170), keepRatio = true)
            frame.copy().paste(img, Pair(23, 231), alpha = true)
        }
    }

    @Meme("高血压")
    val bloodPressure: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["blood_pressure/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resize(Size(414, 450), keepRatio = true)
            frame.copy().paste(img, Pair(16, 17), below = true)
        }
    }

    @Meme("旅行伙伴加入")
    val maimaiJoin: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["maimai_join/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").square().resize(Size(400, 400))
            frame.copy().paste(img, Pair(50, 50), alpha = true, below = true)
        }
    }

    @Meme("捏", "捏脸")
    val pinch: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["pinch/0.png"])
        makeJpgOrGif(images[0]) {
            frame.paste(
                convert("RGBA").resize(Size(1800, 1440), keepRatio = true),
                Pair(1080, 0), below = true
            )
        }
    }

    @Meme("啾啾")
    val jiujiu: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(Size(75, 51), keepRatio = true)
        val frames = (0 until 8).map { i ->
            BuildImage.open(imgDir["jiujiu/$i.png"]).paste(img, below = true).image
        }
        saveGif(frames, 0.06)
    }

    @Meme("转")
    val turn: Maker = { images, _ ->
        val img = images[0].convert("RGBA").circle()
        var frames = (0 until 360 step 10).map { i ->
            val frame = BuildImage.new("RGBA", Size(250, 250), Colors.WHITE)
            frame.paste(img.rotate(i.toDouble()).resize(Size(250, 250)), alpha = true)
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
            val img = convert("RGBA").circle().resize(Size(270, 270))
            frame.copy().paste(img, Pair(350, 590), alpha = true)
        }
    }

    @Meme("遇到困难请拨打", help = "需要两张图片代表 1 和 0")
    val call110: Maker = { images, _ ->
        val img1 = images[0].convert("RGBA").square().resize(Size(250, 250))
        val img0 = images[1].convert("RGBA").square().resize(Size(250, 250))

        val frame = BuildImage.new("RGB", Size(900, 500), Colors.WHITE)
        frame.drawText(listOf(0, 0, 900, 200), "遇到困难请拨打", maxFontSize = 100)
        frame.paste(img1, Pair(50, 200), alpha = true)
        frame.paste(img1, Pair(325, 200), alpha = true)
        frame.paste(img0, Pair(600, 200), alpha = true)

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
            val img = convert("RGBA").square().resize(Size(250, 250)).rotate(-25.0, expand = true)
            frame.copy().paste(img, Pair(134, 134), alpha = true, below = true)
        }
    }

    @Meme("注意力涣散")
    val distracted: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["distracted/1.png"])
        val label = BuildImage.open(imgDir["distracted/0.png"])
        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").square().resize(Size(500, 500))
            frame.copy().paste(img, below = true).paste(label, Pair(140, 320), alpha = true)
        }
    }

    @Meme("上坟", "坟前比耶")
    val tombYeah: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["tomb_yeah/0.jpg"])
        frame.paste(
            images[0].convert("RGBA").circle().resize(Size(145, 145)), Pair(138, 265), alpha = true
        )
        if (images.size > 1) {
            frame.paste(
                images[1].convert("RGBA").circle().rotate(30.0).resize(Size(145, 145)),
                Pair(371, 312), alpha = true
            )
        }
        frame.saveJpg()
    }

    @Meme("砸")
    val smash: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["smash/0.png"])
        makeJpgOrGif(images[0]) {
            val points = listOf(Pair(1, 237), Pair(826, 1), Pair(832, 508), Pair(160, 732))
            val screen = convert("RGBA").resize(Size(800, 500), keepRatio = true).perspective(points)
            frame.copy().paste(screen, Pair(-136, -81), below = true)
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
                    .resize(Size(215, 215)), Pair(100, 100), below = true
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
        val frame = BuildImage.new("RGB", Size(width, height1), Colors.WHITE)
        frame.paste(img, Pair(0, (height * 0.1).toInt()))
        ((height * 0.1).toInt() downTo 1).forEach { i ->
            frame.paste(img.image.alpha(16), Pair(0, i), alpha = true)
        }
        ((height * 0.1).toInt() downTo (height * 0.1 * 2).toInt() + 1).forEach { i ->
            frame.paste(img.image.alpha(16), Pair(0, i), alpha = true)
        }
        frame.saveJpg()
    }

    @Meme("管人痴")
    val dogOfVtb: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["dog_of_vtb/0.png"])
        makeJpgOrGif(images[0]) {
            val points = listOf(Pair(0, 0), Pair(579, 0), Pair(584, 430), Pair(5, 440))
            val img = convert("RGBA").resize(Size(600, 450), keepRatio = true)
            frame.copy().paste(img.perspective(points), Pair(97, 32), below = true)
        }
    }

    @Meme("舔", "舔屏,prpr")
    val prpr: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["prpr/0.png"])

        makeJpgOrGif(images[0]) {
            val points = listOf(Pair(0, 19), Pair(236, 0), Pair(287, 264), Pair(66, 351))
            val screen = convert("RGBA").resize(Size(330, 330), keepRatio = true).perspective(points)
            frame.copy().paste(screen, Pair(56, 284), below = true)
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
        val points = listOf(Pair(0, -30), Pair(135, 17), Pair(135, 145), Pair(0, 140))
        val paint = img.square().resize(Size(150, 150)).perspective(points)
        val frames = (0 until 10).map {
            val frame = BuildImage.open(imgDir["worship/$it.png"])
            frame.paste(paint, below = true).image
        }
        saveGif(frames, 0.04)
    }

    @Meme("群青")
    val cyan: Maker = { images, _ ->
        val color = RGBA.invoke(78, 114, 184)
        val frame = images[0].convert("RGB").square().resize(Size(500, 500))
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
        val img = images[0].convert("RGBA").square().resize(Size(100, 100))
        val frames = listOf(listOf(98, 101, 108, 234), listOf(96, 100, 108, 237)).mapIndexed { i, locs ->
            val frame = BuildImage.open(imgDir["hutao_bite/$i.png"])
            val (w, h, x, y) = locs
            frame.paste(img.resize(Size(w, h)), Pair(x, y), below = true).image
        }
        saveGif(frames, 0.1)
    }

    @Meme("墙纸")
    val wallpaper: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(Size(515, 383), keepRatio = true)
        val frames = (0 until 8).map { BuildImage.open(imgDir["wallpaper/$it.png"]).image }.toMutableList()
        (8 until 20).forEach {
            val frame = BuildImage.open(imgDir["wallpaper/$it.png"])
            frame.paste(img, Pair(176, -9), below = true)
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
            frame.paste(img.resize(Size(w, h)), Pair(x, y), below = true)
            frame.image
        }
        saveGif(frames, 0.04)
    }

    @Meme("捶爆", "爆捶")
    val thumpWildly: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(Size(122, 122), keepRatio = true)
        val rawFrames = (0 until 31).map { BuildImage.open(imgDir["thump_wildly/$it.png"]) }
        (0 until 14).forEach {
            rawFrames[it].paste(img, Pair(203, 196), below = true)
        }
        rawFrames[14].paste(img, Pair(207, 239), below = true)
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
        val mask = BuildImage.new("RGBA", Size(500, 60), RGBA(53, 49, 65, 230))
        val logo = BuildImage.open(imgDir["mihoyo/logo.png"]).resizeHeight(50)
        makePngOrGif(images[0]) {
            val img = convert("RGBA").resize(Size(500, 500), keepRatio = true)
            img.paste(mask, Pair(0, 440), alpha = true)
            img.paste(logo, Pair((img.width - logo.width) / 2, 445), alpha = true)
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
        val frame = img.resizeCanvas(Size(imgW, imgH)).resizeHeight(1080)
        val left = BuildImage.open(imgDir["marriage/0.png"])
        val right = BuildImage.open(imgDir["marriage/1.png"])
        frame.paste(left, alpha = true).paste(
            right, Pair(frame.width - right.width, 0), alpha = true
        )
        frame.saveJpg()
    }

    @Meme("打印")
    val printing: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(
            Size(304, 174), keepRatio = true,
            inside = true, bgColor = Colors.WHITE, direction = BuildImage.DirectionType.South
        )
        val frames = (0 until 115).map {
            BuildImage.open(imgDir["printing/$it.png"])
        }
        (50 until 115).forEach {
            frames[it].paste(img, Pair(146, 164), below = true)
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
            frame.paste(img.resize(Size(w, h)), Pair(x, y), alpha = true).paste(heart, alpha = true)
            frames.add(frame.image)
        }
        saveGif(frames, 0.2)
    }

    @Meme("搓")
    val twist: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(Size(78, 78))
        val locs = listOf(
            listOf(25, 66, 0), listOf(25, 66, 60), listOf(23, 68, 120),
            listOf(20, 69, 180), listOf(22, 68, 240), listOf(25, 66, 300)
        )
        val frames = (0 until 5).map {
            val frame = BuildImage.open(imgDir["twist/$it.png"])
            val (x, y, a) = locs[it]
            frame.paste(img.rotate(a.toDouble()), Pair(x, y), below = true).image
        }
        saveGif(frames, 0.1)

    }

    @Meme("快跑")
    val run: Maker = { _, texts ->
        val text = texts[0]
        val frame = BuildImage.open(imgDir["run/0.png"])
        val textImg = BuildImage.new("RGBA", Size(122, 53))
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
        frame.paste(textImg.rotate(7.0, expand = true), Pair(200, 195), alpha = true)
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
        val img = images[0].convert("RGBA").square().resize(Size(180, 180))
        val locs = listOf(
            listOf(178, 184, 78, 260),
            listOf(178, 174, 84, 269),
            listOf(178, 174, 84, 269),
            listOf(178, 178, 84, 264)
        )
        val frames = (0 until 4).map {
            val frame = BuildImage.open(imgDir["capoo_rub/$it.png"])
            val (w, h, x, y) = locs[it]
            frame.paste(img.resize(Size(w, h)), Pair(x, y), below = true).image
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
            frame.paste(img.resize(Size(w, h)), Pair(x, y), below = true).image
        }
        saveGif(frames, 0.06)
    }

    @Meme("滚")
    val roll: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(Size(210, 210))
        val locs = listOf(
            listOf(87, 77, 0), listOf(96, 85, -45), listOf(92, 79, -90), listOf(92, 78, -135),
            listOf(92, 75, -180), listOf(92, 75, -225), listOf(93, 76, -270), listOf(90, 80, -315)
        )
        val frames = (0 until 8).map {
            val frame = BuildImage.open(imgDir["roll/$it.png"])
            val (x, y, a) = locs[it]
            frame.paste(img.rotate(a.toDouble()), Pair(x, y), below = true).image
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

        val frame = BuildImage.new("RGBA", Size(640, 440 * images.size), Colors.WHITE)
        (0 until images.size).map { i ->
            var bg = if (i < images.size - 2) bg0 else if (i == images.size - 2) bg1 else bg2
            images[i] = images[i].convert("RGBA").square().resize(Size(250, 250))
            bg = bg.copy().paste(images[i], Pair(350, 85))
            frame.paste(bg, Pair(0, 440 * i))
        }
        frame.saveJpg()
    }

    @Meme("挠头")
    val scratchHead: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(Size(68, 68))
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
            frame.paste(img.resize(Size(w, h)), Pair(x, y), below = true).image
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
            frame.paste(img.resize(Size(w, h)), Pair(x, y), below = true).image
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
            frame.paste(img.resize(Size(w, h)), Pair(x, y), below = true).image
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
        val img = images[0].convert("RGBA").square().resize(Size(79, 79))
        val locs = mutableListOf<Pair<Int, Int>>()
        repeat(3) {
            locs.add(Pair(39, 40))
        }
        repeat(2) {
            locs.add(Pair(39, 30))
        }
        repeat(10) {
            locs.add(Pair(39, 32))
        }
        locs.addAll(
            listOf(
                Pair(39, 30), Pair(39, 27), Pair(39, 32), Pair(37, 49), Pair(37, 64),
                Pair(37, 67), Pair(37, 67), Pair(39, 69), Pair(37, 70), Pair(37, 70)
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
            frame.paste(img.resize(Size(w, h)), Pair(x, y), below = true).image
        }
        saveGif(frames, 0.05)
    }

    @Meme("踢球")
    val kickBall: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(Size(78, 78))
        val locs = listOf(
            Pair(57, 136), Pair(56, 117), Pair(55, 99), Pair(52, 113), Pair(50, 126),
            Pair(48, 139), Pair(47, 112), Pair(47, 85), Pair(47, 57), Pair(48, 97),
            Pair(50, 136), Pair(51, 176), Pair(52, 169), Pair(55, 181), Pair(58, 153)
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
            frame.paste(img.resize(Size(w, h)), Pair(x, y), below = true)
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
        val img = images[0].convert("RGBA").resize(Size(100, 100), keepRatio = true)
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
            frame.paste(img.resize(Size(w, h)).rotate(angle.toDouble(), expand = true), Pair(x, y), below = true).image
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
        userImg = userImg!!.convert("RGBA").square().resize(Size(385, 385))
        selfImg?.let {
            selfImg = it.convert("RGBA").square().resize(Size(230, 230))
            frame!!.paste(it, Pair(408, 418), below = true)
        }
        frame!!.paste(userImg.rotate(24.0, expand = true), Pair(-5, 355), below = true)
        frame.paste(userImg.rotate(-11.0, expand = true), Pair(649, 310), below = true)
        frame.saveJpg()
    }

    @Meme("举")
    val raiseImage: Maker = { images, _ ->
        val innerSize = Size(599, 386)
        val pastePos = Pair(134, 91)

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
        val img = images[0].convert("RGBA").square().resize(Size(260, 260))
        val locs = listOf(
            Pair(-50, 20), Pair(-40, 10), Pair(-30, 0), Pair(-20, -10), Pair(-10, -10), Pair(0, 0),
            Pair(10, 10), Pair(20, 20), Pair(10, 10), Pair(0, 0), Pair(-10, -10), Pair(10, 0), Pair(-30, 10)
        )
        val frames = (0 until 13).map {
            val fist = BuildImage.open(imgDir["punch/$it.png"])
            val frame = BuildImage.new("RGBA", fist.size, Colors.WHITE)
            val (x, y) = locs[it]
            frame.paste(img, Pair(x, y - 15), alpha = true).paste(fist, alpha = true).image
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
                frame.paste(img.resize(Size(w, h)), Pair(x, y), alpha = true)
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
            frame.paste(img.resize(Size(w, h)), Pair(x, y), below = true).image
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
            val img = convert("RGBA").resize(Size(300, 300), keepRatio = true)
            val frame = BuildImage.new("RGBA", Size(600, 600), Colors.WHITE)
            frame.paste(img, alpha = true)
            frame.paste(img.rotate(90.0), Pair(0, 300), alpha = true)
            frame.paste(img.rotate(180.0), Pair(300, 300), alpha = true)
            frame.paste(img.rotate(270.0), Pair(300, 0), alpha = true)
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

        val img = images[0].convert("RGBA").circle().resize(Size(100, 100))
        val frame = BuildImage.open(imgDir["crawl/%02d.jpg".format(num)])
        frame.paste(img, Pair(0, 400), alpha = true)
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
                Pair(0, 0),
                Pair(w / 25, w / 25),
                Pair(w / 50, w / 50),
                Pair(0, w / 25),
                Pair(w / 25, 0)
            )
            val frame = BuildImage.new("RGBA", Size(w + w / 25, w + w / 25), Colors.WHITE)
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
            "RGB", Size(frame.width, frame.height + textImg.height + 20),
            Colors.WHITE
        )
        bg.paste(frame).paste(textImg, Pair(30, frame.height + 5), alpha = true)
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
            frame.paste(img.resize(Size(w, h)), Pair(x, y), alpha = true).paste(bg, alpha = true).image
        }
        saveGif(frames, 0.08)

    }

    @Meme("举牌", help = "需要文本")
    val raiseSign: Maker = { _, texts ->
        val text = texts.ifBlank { "大佬带带我" }
        val frame = BuildImage.open(imgDir["raise_sign/0.jpg"])
        var textImg = BuildImage.new("RGBA", Size(360, 260))
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
        textImg = textImg.perspective(listOf(Pair(33, 0), Pair(375, 120), Pair(333, 387), Pair(0, 258)))
        frame.paste(textImg, Pair(285, 24), alpha = true)
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
        val img = images[0].convert("RGBA").circle().resize(Size(150, 150)).rotate(-10.0, expand = true)
        frame.paste(img, Pair(268, 344), alpha = true)
        frame.saveJpg()
    }

    @Meme("诺基亚", "有内鬼", help = "需要文本")
    val nokia: Maker = { _, texts ->
        val text = texts.joinToString(" ").ifBlank { "无内鬼，继续交易" }.take(900)
        val textImg = BuildImage(
            Text2Image.fromText(text, 70, fontName = "FZXS14", fill = Colors.BLACK, spacing = 30)
                .wrap(700.0)
                .toImage()
        ).resizeCanvas(Size(700, 450), direction = BuildImage.DirectionType.Northwest)
            .rotate(-9.3, expand = true)

        val headImg = BuildImage(
            Text2Image.fromText(
                "${text.length}/900", 70, fontName = "FZXS14", fill = RGBA(129, 212, 250)
            ).toImage()
        ).rotate(-9.3, expand = true)

        val frame = BuildImage.open(imgDir["nokia/0.jpg"])
        frame.paste(textImg, Pair(205, 330), alpha = true)
        frame.paste(headImg, Pair(790, 320), alpha = true)
        frame.saveJpg()
    }

    @Meme("罗永浩说", help = "需要文本")
    val luoyonghaoSay: Maker = { _, texts ->
        val text = texts.ifBlank { "又不是不能用" }
        val frame = BuildImage.open(imgDir["luoyonghao_say/0.jpg"])
        var textFrame = BuildImage.new("RGBA", Size(365, 120))
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
        textFrame = textFrame.perspective(listOf(Pair(52, 10), Pair(391, 0), Pair(364, 110), Pair(0, 120)))
            .filter(GaussianBlurFilter(radius = 0.8))
        frame.paste(textFrame, Pair(48, 246), alpha = true)
        frame.saveJpg()
    }

    @Meme("可莉吃")
    val kleeEat: Maker = { images, _ ->
        val img = images[0].convert("RGBA").square().resize(Size(83, 83))
        val locs = listOf(
            Pair(0, 174), Pair(0, 174), Pair(0, 174), Pair(0, 174), Pair(0, 174), Pair(12, 160), Pair(19, 152),
            Pair(23, 148), Pair(26, 145), Pair(32, 140), Pair(37, 136), Pair(42, 131), Pair(49, 127), Pair(70, 126),
            Pair(88, 128), Pair(-30, 210), Pair(-19, 207), Pair(-14, 200), Pair(-10, 188), Pair(-7, 179),
            Pair(-3, 170), Pair(-3, 175), Pair(-1, 174), Pair(0, 174), Pair(0, 174), Pair(0, 174), Pair(0, 174),
            Pair(0, 174), Pair(0, 174), Pair(0, 174), Pair(0, 174)
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
            Size(leftImg.width + rightImg.width, max(leftImg.height, rightImg.height)),
            Colors.BLACK
        )
        frame.paste(leftImg, Pair(0, frame.height - leftImg.height)).paste(
            rightImg, Pair(leftImg.width, frame.height - rightImg.height), alpha = true
        )
        frame = frame.resizeCanvas(
            Size(frame.width + 100, frame.height + 100),
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
                .resize(Size(635, 725), keepRatio = true)
            frame.copy().paste(img, Pair(645, 145), below = true)
        }
    }

    @Meme("口号", help = "需要六段文本")
    val slogan: Maker = { _, rawTexts ->
        val texts = if (rawTexts.size >= 6) rawTexts else listOf(
            "我们是谁？", "XX人！", "到XX大学来做什么？", "混！", "将来毕业后要做什么样的人？", "混混！"
        )
        val frame = BuildImage.open(imgDir["slogan/0.jpg"])
        val draw: (List<Int>, String) -> Unit = { pos, text ->
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
        val draw: (List<Int>, String) -> Unit = { pos, text ->
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
            val img = convert("RGBA").resize(Size(1751, 1347), keepRatio = true)
            frame.copy().paste(img, Pair(1440, 0), alpha = true)
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

        val draw: (List<Int>, String) -> Unit = { pos, text ->
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
        val img = images[0].convert("RGBA").resize(Size(640, 640), keepRatio = true)

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
            img.perspective(listOf(Pair(507, 0), Pair(940, 351), Pair(383, 625), Pair(0, 256))),
            Pair(201, 201), below = true,
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
            val img = convert("RGBA").resize(Size(210, 170), keepRatio = true, inside = true)
            frame.copy().paste(img, Pair(150, 2), alpha = true)
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
            val img = convert("RGBA").resize(Size(63, 63), keepRatio = true)
            frame.copy().paste(img, Pair(132, 36), alpha = true)
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
                frames.add(textImg.resizeCanvas(Size(500, textImg.height)))
            }
            val frame = BuildImage.new(
                "RGBA", Size(500, frames.sumOf { it.height } + 10), Colors.WHITE
            )
            var currentH = 0
            frames.forEach { f ->
                frame.paste(f, Pair(0, currentH), alpha = true)
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
            val img = convert("RGBA").resize(Size(550, 395), keepRatio = true)
            frame.copy().paste(img, Pair(313, 60), below = true)
        }
    }

    @Meme("亲", "亲亲", help = "需要两张图片")
    val kiss: Maker = { images, _ ->
        val selfHead = images[0].convert("RGBA").circle().resize(Size(40, 40))
        val userHead = images[1].convert("RGBA").circle().resize(Size(50, 50))
        val userLocs = listOf(
            Pair(58, 90), Pair(62, 95), Pair(42, 100), Pair(50, 100), Pair(56, 100), Pair(18, 120), Pair(28, 110),
            Pair(54, 100), Pair(46, 100), Pair(60, 100), Pair(35, 115), Pair(20, 120), Pair(40, 96)
        )
        val selfLocs = listOf(
            Pair(92, 64), Pair(135, 40), Pair(84, 105), Pair(80, 110), Pair(155, 82), Pair(60, 96), Pair(50, 80),
            Pair(98, 55), Pair(35, 65), Pair(38, 100), Pair(70, 80), Pair(84, 65), Pair(75, 65)
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
            val img = convert("RGBA").resize(Size(w, h), keepRatio = true)
            frame.paste(img.rotate(4.2, expand = true).image, Pair(x, y), below = true)
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
            val img = convert("RGBA").resize(Size(515, 515), keepRatio = true)
            frame.copy().paste(img, Pair(599, 403), below = true)
        }
    }

    @Meme("诈尸", "秽土转生")
    val riseDead: Maker = { images, _ ->
        val locs = listOf(
            Pair(Pair(81, 55), listOf(Pair(0, 2), Pair(101, 0), Pair(103, 105), Pair(1, 105))),
            Pair(Pair(74, 49), listOf(Pair(0, 3), Pair(104, 0), Pair(106, 108), Pair(1, 108))),
            Pair(Pair(-66, 36), listOf(Pair(0, 0), Pair(182, 5), Pair(184, 194), Pair(1, 185))),
            Pair(Pair(-231, 55), listOf(Pair(0, 0), Pair(259, 4), Pair(276, 281), Pair(13, 278))),
        )
        val img = images[0].convert("RGBA").square().resize(Size(100, 100))
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
                frame.paste(imgs[idx], Pair(x, y), below = true)
            }
            frame.image
        }
        saveGif(frames, 0.15)
    }

    @Meme("兑换券", help = "需要图片和文本")
    val coupon: Maker = { images, texts ->
        val img = images[0].convert("RGBA").circle().resize(Size(60, 60))
        val name = texts[0]
        val text = name + texts.getOrElse(1) { "陪睡券" } + "\n（永久有效）"

        val textImg = BuildImage.new("RGBA", Size(250, 100))
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
        frame.paste(img.rotate(22.0, expand = true), Pair(164, 85), alpha = true)
        frame.paste(textImg.rotate(22.0, expand = true), Pair(94, 108), alpha = true)
        frame.saveJpg()
    }

    @Meme("不文明", "需要图片和文本")
    val incivilization: Maker = { images, texts ->
        val frame = BuildImage.open(imgDir["incivilization/0.png"])
        val points = listOf(Pair(0, 20), Pair(154, 0), Pair(164, 153), Pair(22, 180))
        val img = images[0].convert("RGBA").circle().resize(Size(150, 150)).perspective(points)
        val image = img.filter(BrightnessFilter(0.8F))
        frame.paste(image, Pair(137, 151), alpha = true)
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
        val img = images[0].convert("RGBA").resize(Size(175, 120), keepRatio = true)
        val params = listOf(
            Pair(listOf(Pair(27, 0), Pair(207, 12), Pair(179, 142), Pair(0, 117)), Pair(30, 16)),
            Pair(listOf(Pair(28, 0), Pair(207, 13), Pair(180, 137), Pair(0, 117)), Pair(34, 17))
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
        val img = images[0].convert("RGBA").square().resize(Size(27, 27))
        val locs = listOf(
            Pair(2, 26), Pair(10, 24), Pair(15, 27), Pair(17, 29), Pair(10, 20), Pair(2, 29), Pair(3, 31), Pair(1, 30)
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
        val img = images[0].convert("RGBA").resize(Size(640, 400), keepRatio = true)
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
            frame.paste(img.resize(Size(w, h)), Pair(x, y), below = true).image
        }
        saveGif(frames, 0.08)
    }

    @Meme("安全感")
    val safeSense: Maker = { images, texts ->
        val img = images[0].convert("RGBA").resize(Size(215, 343), keepRatio = true)
        val frame = BuildImage.open(imgDir["safe_sense/0.png"])
        frame.paste(img, Pair(215, 135))

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
        val img = images[0].convert("RGBA").resize(Size(160, 140), keepRatio = true)
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
            frame.paste(img.resize(Size(w, h)), Pair(x, y), below = true).image
        }
        saveGif(frames, 0.1)

    }

    @Meme("加载中")
    val loading: Maker = { images, _ ->
        val imgBig = images[0].convert("RGBA").resizeWidth(500).filter(GaussianBlurFilter(3.0))
        val h1 = imgBig.height
        val mask = BuildImage.new("RGBA", imgBig.size, RGBA(0, 0, 0, 32))
        val icon = BuildImage.open(imgDir["loading/icon.png"])
        imgBig.paste(mask, alpha = true).paste(icon, Pair(200, (h1 / 2) - 50), alpha = true)

        makeJpgOrGif(images[0]) {
            val imgSmall = convert("RGBA").resizeWidth(100)
            val h2 = max(imgSmall.height, 80)
            val frame = BuildImage.new("RGBA", Size(500, h1 + h2 + 10), Colors.WHITE)
            frame.paste(imgBig, alpha = true).paste(
                imgSmall, Pair(100, h1 + 5 + (h2 - imgSmall.height) / 2), alpha = true
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
            val head = img.resize(Size(w, h), keepRatio = true).circle()
            val frame = BuildImage.open(imgDir["beat_head/$it.png"])
            frame.paste(head, Pair(x, y), below = true)
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
        val imgC = img.convert("RGBA").circle().resize(Size(72, 72))
        val imgTp = img.convert("RGBA").circle().resize(Size(51, 51))
        frame.paste(imgTp, Pair(948, 247), alpha = true)
        val locs = listOf(
            Pair(143, 32), Pair(155, 148), Pair(334, 149), Pair(275, 266), Pair(486, 266), Pair(258, 383),
            Pair(439, 382), Pair(343, 539), Pair(577, 487), Pair(296, 717), Pair(535, 717), Pair(64, 896),
            Pair(340, 896), Pair(578, 897), Pair(210, 1038), Pair(644, 1039), Pair(64, 1192), Pair(460, 1192),
            Pair(698, 1192), Pair(1036, 141), Pair(1217, 141), Pair(1243, 263), Pair(1140, 378), Pair(1321, 378),
            Pair(929, 531), Pair(1325, 531), Pair(1592, 531), Pair(1007, 687), Pair(1390, 687), Pair(1631, 686),
            Pair(1036, 840), Pair(1209, 839), Pair(1447, 839), Pair(1141, 1018), Pair(1309, 1019), Pair(1546, 1019),
            Pair(1037, 1197), Pair(1317, 1198), Pair(1555, 1197)
        )
        (0 until 39).forEach {
            val (x, y) = locs[it]
            frame.paste(imgC, Pair(x, y), alpha = true)
        }
        frame.saveJpg()
    }

    @Meme("击剑", "🤺", help = "需要两张图片")
    val fencing: Maker = { images, _ ->
        val selfHead = images[0].convert("RGBA").circle().resize(Size(27, 27))
        val userHead = images[1].convert("RGBA").circle().resize(Size(27, 27))
        val userLocs = listOf(
            Pair(57, 4), Pair(55, 5), Pair(58, 7), Pair(57, 5), Pair(53, 8), Pair(54, 9), Pair(64, 5), Pair(66, 8),
            Pair(70, 9), Pair(73, 8), Pair(81, 10), Pair(77, 10), Pair(72, 4), Pair(79, 8), Pair(50, 8), Pair(60, 7),
            Pair(67, 6), Pair(60, 6), Pair(50, 9)
        )
        val selfLocs = listOf(
            Pair(10, 6), Pair(3, 6), Pair(32, 7), Pair(22, 7), Pair(13, 4), Pair(21, 6), Pair(30, 6), Pair(22, 2),
            Pair(22, 3), Pair(26, 8), Pair(23, 8), Pair(27, 10), Pair(30, 9), Pair(17, 6), Pair(12, 8), Pair(11, 7),
            Pair(8, 6), Pair(-2, 10), Pair(4, 9)
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
                frame.paste(userHead.resize(Size(w, h)), Pair(x, y), alpha = true)
            }
            selfLocs[it].let { (x, y, w, h, angle) ->
                frame.paste(
                    selfHead.resize(Size(w, h)).rotate(angle.toDouble(), expand = true), Pair(x, y), alpha = true
                )
            }
            frame.image
        }
        saveGif(frames, 0.05)

    }

    @Meme("关注", help = "需要文本和图片")
    val follow: Maker = { images, texts ->
        val img = images[0].circle().resize(Size(200, 200))
        val name = texts.ifBlank { "男同" }

        val nameImg = Text2Image.fromText(name, 60).toImage()
        val followImg = Text2Image.fromText("关注了你", 60, fill = Colors.DIMGREY).toImage()
        val textWidth = max(nameImg.width, followImg.width)
        if (textWidth >= 1000) {
            throw TextOverLengthException()
        }

        val frame = BuildImage.new(
            "RGBA", Size(300 + textWidth + 50, 300),
            RGBA(255, 255, 255, 0)
        )
        frame.paste(img, Pair(50, 50), alpha = true)
        frame.paste(nameImg, Pair(300, 135 - nameImg.height), alpha = true)
        frame.paste(followImg, Pair(300, 145), alpha = true)
        frame.saveJpg()
    }

    @Meme("采访")
    val interview: Maker = { images, texts ->
        var (selfImg, userImg) = if (images.size >= 2) {
            Pair(images[0], images[1])
        } else {
            Pair(BuildImage.open(imgDir["interview/huaji.png"]), images[0])
        }
        selfImg = selfImg.convert("RGBA").square().resize(Size(124, 124))
        userImg = userImg.convert("RGBA").square().resize(Size(124, 124))
        val text = texts.ifBlank { "采访大佬经验" }
        val frame = BuildImage.new("RGBA", Size(600, 310), Colors.WHITE)
        val microphone = BuildImage.open(imgDir["interview/microphone.png"])
        frame.paste(microphone, Pair(330, 103), alpha = true)
        frame.paste(selfImg, Pair(419, 40), alpha = true)
        frame.paste(userImg, Pair(57, 40), alpha = true)
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
            val points = listOf(Pair(0, 5), Pair(227, 0), Pair(216, 150), Pair(0, 165))
            val screen = (
                    convert("RGBA").resize(Size(220, 160), keepRatio = true).perspective(points)
                    )
            frame.copy().paste(screen.rotate(9.0, expand = true), Pair(161, 117), below = true)
        }
    }
    @Meme("远离")
    val keep_away: Maker = { images, texts ->
        var count = 0
        val frame = BuildImage.new("RGB", Size(400, 290), Colors.WHITE)
        val trans: suspend (BuildImage, Int) -> BuildImage = { image, n ->
            val img = image.convert("RGBA").square().resize(Size(100, 100))
            if (n < 4) {
                img.rotate(n * 90.0)
            } else {
                img.image.flipX().toMemeBuilder().rotate((n - 4) * 90.0)
            }
        }

        val paste: (BuildImage) -> Unit = { img ->
            val y = if (count < 4) 90 else 190
            frame.paste(img, Pair((count % 4) * 100, y))
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
            Pair(listOf(Pair(0, 4), Pair(153, 0), Pair(138, 105), Pair(0, 157)), Pair(28, 47)),
            Pair(listOf(Pair(1, 13), Pair(151, 0), Pair(130, 104), Pair(0, 156)), Pair(28, 48)),
            Pair(listOf(Pair(9, 10), Pair(156, 0), Pair(152, 108), Pair(0, 155)), Pair(18, 51)),
            Pair(listOf(Pair(0, 21), Pair(150, 0), Pair(146, 115), Pair(7, 145)), Pair(17, 53)),
            Pair(listOf(Pair(0, 19), Pair(156, 0), Pair(199, 109), Pair(31, 145)), Pair(2, 62)),
            Pair(listOf(Pair(0, 28), Pair(156, 0), Pair(171, 115), Pair(12, 154)), Pair(16, 58)),
            Pair(listOf(Pair(0, 25), Pair(157, 0), Pair(169, 113), Pair(13, 147)), Pair(18, 63))
        )

        makeGifOrCombinedGif(
            images[0], 7, 0.05, FrameAlignPolicy.ExtendLoop
        ) {
            val img = convert("RGBA").resize(Size(200, 160), keepRatio = true)
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
            val points = listOf(Pair(0, 116), Pair(585, 0), Pair(584, 319), Pair(43, 385))
            val screen = (
                    convert("RGBA").resize(Size(600, 330), keepRatio = true).perspective(points)
                    )
            frame.copy().paste(screen, Pair(412, 121), below = true)
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
            Pair(743, frame.height - 155),
            alpha = true,
        )
        frame.paste(img.square().resize(Size(55, 55)).rotate(9.0, expand = true),
            Pair(836, frame.height - 278),
            alpha = true,
        )
        frame.paste(bg, Pair(0, frame.height - 1000), alpha = true)

        val textImg = Text2Image.fromText(name, 20, fill = Colors.WHITE).toImage()
        if (textImg.width > 230) {
            throw TextOverLengthException()
        }

        frame.paste(textImg, Pair(710, frame.height - 308), alpha = true)
        frame.saveJpg()
    }

    @Meme("最想要的东西")
    val whatHeWants: Maker = { images, texts ->
        val date = texts.ifBlank { "今年520" }
        val text = "${date}我会给你每个男人都最想要的东西···"
        val frame =
            BuildImage.open(imgDir["what_he_wants/0.png"])

        makeJpgOrGif(images[0]) {
            val img = convert("RGBA").resize(Size(538, 538), keepRatio = true, inside = true)
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
            newFrame.paste(img, Pair(486, 616), alpha = true)
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
            frame.paste(img.resize(Size(w,  h)), Pair(x,  y),  below=true)
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
            frame.paste(img.resize(Size(w,  h)).image, Pair(x,  y),  alpha=true)
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

            frame = frame.resizeCanvas(Size(imgW - amp, imgH - amp))
            phase += period / frameNum
            frame
        }

    }
    @Meme("我老婆", "这是我老婆")
    val myWife: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resizeWidth(400)
        val imgW = img.width
        val imgH = img.height
        val frame = BuildImage.new("RGBA", Size(650, imgH + 500), Colors.WHITE)
        frame.paste(img, Pair((325 - imgW / 2),  105),  alpha=true)

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
        frame.paste(imgPoint, Pair(421,  imgH + 270))

        frame.saveJpg()

    }
    @Meme("吴京xx中国xx", "吴京,吴京中国", help = "需要两段文本")
    val wujing: Maker = { _, texts ->
        val frame = BuildImage.open(imgDir["wujing/0.jpg"])
        val draw: (List<Int>, String, HorizontalAlign) -> Unit = { pos, text, align ->
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
        val img = images[0].convert("RGBA").resize(Size(350, 400), keepRatio=true)
        val params = listOf(
            Pair(listOf(Pair(54,  62), Pair(353,  1), Pair(379,  382), Pair(1,  399)), Pair(146,  173)),
            Pair(listOf(Pair(54,  61), Pair(349,  1), Pair(379,  381), Pair(1,  398)), Pair(146,  174)),
            Pair(listOf(Pair(54,  61), Pair(349,  1), Pair(379,  381), Pair(1,  398)), Pair(152,  174)),
            Pair(listOf(Pair(54,  61), Pair(335,  1), Pair(379,  381), Pair(1,  398)), Pair(158,  167)),
            Pair(listOf(Pair(54,  61), Pair(335,  1), Pair(370,  381), Pair(1,  398)), Pair(157,  149)),
            Pair(listOf(Pair(41,  59), Pair(321,  1), Pair(357,  379), Pair(1,  396)), Pair(167,  108)),
            Pair(listOf(Pair(41,  57), Pair(315,  1), Pair(357,  377), Pair(1,  394)), Pair(173,  69)),
            Pair(listOf(Pair(41,  56), Pair(309,  1), Pair(353,  380), Pair(1,  393)), Pair(175,  43)),
            Pair(listOf(Pair(41,  56), Pair(314,  1), Pair(353,  380), Pair(1,  393)), Pair(174,  30)),
            Pair(listOf(Pair(41,  50), Pair(312,  1), Pair(348,  367), Pair(1,  387)), Pair(171,  18)),
            Pair(listOf(Pair(35,  50), Pair(306,  1), Pair(342,  367), Pair(1,  386)), Pair(178,  14))
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
}
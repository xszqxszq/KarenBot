@file:Suppress("unused", "UNCHECKED_CAST")

package xyz.xszq.bot.image

import com.sksamuel.scrimage.canvas.painters.LinearGradient
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.showImageAndWait
import com.soywiz.korim.text.HorizontalAlign
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korma.geom.Size
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import xyz.xszq.nereides.hexToRGBA
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
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
            func.annotations.filterIsInstance<Meme>().firstOrNull() ?.let { settings ->
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
            func.annotations.filterIsInstance<Meme>().firstOrNull() ?.let { settings ->
                append(" ")
                append(settings.command)
            }
        }
    }
    fun getHelpText(type: String): String {
        MemeGenerator::class.declaredMemberProperties.forEach { func ->
            func.annotations.filterIsInstance<Meme>().firstOrNull() ?.let { settings ->
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
        return makeGifOrCombinedGif(img, frameNum, 0.1, FrameAlignPolicy.ExtendLoop, maker=maker)
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
    @Meme("我永远喜欢", help = "需要附带一对或多对文字+图片，且文字数量需要与图片相等\n\t例：/生成 我永远喜欢 心爱 [图片]\n\t例：/生成 我永远喜欢 心爱 虹夏 [图片] [图片]")
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
        images.mapIndexed { index, memeBuilder -> Pair(memeBuilder, texts[index]) }.subList(1, images.size).forEachIndexed { index, (image, name) ->
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
        val img = images[0].convert("RGBA").square().resize(Size (110, 110))
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
            frame.paste(img.resize(Size(w, h)), Pair(x, y), below=true)
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
                allowWrap=true,
                maxFontSize=50,
                minFontSize=20,
            )
        }.onFailure {
            throw TextOverLengthException()
        }
        frame.saveJpg()
    }
//    val ask: Maker = { images, texts ->
//        if (texts.isEmpty() || texts.first().isBlank())
//            throw TextOverLengthException()
//
//        val name = texts[0]
//        val ta = "TA"
//
//        val img = images[0].resizeWidth(640)
//        val imgW = img.width
//        val imgH = img.height
//        val gradientH = 150
//        val gradient = LinearGradient(
//        (0, 0, 0, gradient_h),
//        [ColorStop(0, (0, 0, 0, 220)), ColorStop(1, (0, 0, 0, 30))],
//        )
//        val gradientImg = gradient.create_image((img_w, gradient_h))
//        var mask = BuildImage.new("RGBA", img.size)
//        mask.paste(gradientImg, (0, img_h - gradient_h), alpha=True)
//        mask = mask.filter(ImageFilter.GaussianBlur(radius=3))
//        img.paste(mask, alpha=True)
//
//        start_w = 20
//        start_h = img_h - gradient_h + 5
//        text1 = name
//        text2 = f"{name}不知道哦。"
//        text2img1 = Text2Image.from_text(text1, 28, weight="bold")
//        text2img2 = Text2Image.from_text(text2, 28, weight="bold")
//        img.draw_text(
//        (start_w + 40 + (text2img2.width - text2img1.width) // 2, start_h),
//        text1,
//        fontsize=28,
//        fill="orange",
//        weight="bold",
//        )
//        img.draw_text(
//        (start_w + 40, start_h + text2img1.height + 10),
//        text2,
//        fontsize=28,
//        fill="white",
//        weight="bold",
//        )
//
//        line_h = start_h + text2img1.height + 5
//        img.draw_line(
//        (start_w, line_h, start_w + text2img2.width + 80, line_h),
//        fill="orange",
//        width=2,
//        )
//
//        sep_w = 30
//        sep_h = 80
//        frame = BuildImage.new("RGBA", (img_w + sep_w * 2, img_h + sep_h * 2), "white")
//        try:
//        frame.draw_text(
//        (sep_w, 0, img_w + sep_w, sep_h),
//        f"让{name}告诉你吧",
//        max_fontsize=35,
//        halign="left",
//        )
//        frame.draw_text(
//        (sep_w, img_h + sep_h, img_w + sep_w, img_h + sep_h * 2),
//        f"啊这，{ta}说不知道",
//        max_fontsize=35,
//        halign="left",
//        )
//        except ValueError:
//        raise TextOverLength(name)
//        frame.paste(img, (sep_w, sep_h), alpha=True)
//        return frame.save_png()
//        }
    @Meme("土豆")
    val potato: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["potato/0.png"])
        val img = images[0].convert("RGBA").square().resize(Size(458, 458))
        frame.paste(img.rotate(-5.0), Pair(531, 15), below=true)
        frame.saveJpg()
    }
    @Meme("抱紧")
    val holdTight: Maker = { images, _ ->
        val img = images[0].convert("RGBA").resize(Size(159, 171), keepRatio = true)
        val frame = BuildImage.open(imgDir["hold_tight/0.png"])
        frame.paste(img, Pair(113, 205), below=true)
        frame.saveJpg()
    }
    @Meme("离婚协议", "离婚申请")
    val divorce: Maker = { images, _ ->
        val frame = BuildImage.open(imgDir["divorce/0.png"])
        val img = images[0].convert("RGBA").resize(frame.size, keepRatio = true)
        frame.paste(img, below=true)
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
        val img = images[0].convert("RGBA").resize(Size(240, 345), keepRatio = true, direction = BuildImage.DirectionType.North)
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
        val img = images[0].convert("RGBA").resize(Size(220, 310), keepRatio = true, direction = BuildImage.DirectionType.North)
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
                drawImage(img.motionBlur(Random.nextInt(-90, 90).toDouble(), Random.nextInt(0, 50))
                    .rotate(Random.nextInt(-20, 20).toDouble()).image, 0, 0)
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
                Pair(1080, 0), below = true)
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
}
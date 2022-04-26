package xyz.xszq.otomadbot.image

import com.sksamuel.scrimage.DisposeMethod
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.StreamingGifWriter
import com.soywiz.korim.awt.toAwt
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.bitmap.resized
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korim.format.readNativeImage
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseNameWithoutExtension
import com.soywiz.korio.file.std.toVfs
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.PointInt
import com.soywiz.korma.geom.ScaleMode
import com.soywiz.korma.geom.vector.arc
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.nextMessage
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.core.Quota
import xyz.xszq.otomadbot.core.available
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Duration
import kotlin.math.max


object ImageTemplateHandler: EventHandler("图像模板", "image.template") {
    private val quota = Quota("image_template")
    override fun register() {
        GlobalEventChannel.subscribeMessages {
            startsWithSimple("/生成") { raw, _ ->
                requireNot(denied) {
                    if (available(quota)) {
                        val args = raw.replace("@", " ").replace("[", " ").toArgsList()
                        handle(args, this)
                    } else {
                        quoteReply(quotaExceededMessage)
                        return@requireNot
                    }
                }
            }
        }
        super.register()
    }
    private suspend fun handle(args: Args, event: MessageEvent): Unit = event.run {
        when {
            args.isEmpty() -> quoteReply(unknownMessage)
            !ImageTemplateConfigs.templates.containsKey(args[0]) -> quoteReply(unknownMessage)
            else -> {
                val config = ImageTemplateConfigs.templates[args[0]]!!
                val target = when {
                    this is GroupMessageEvent && message.anyIsInstance<At>() -> {
                        group.members[message.filterIsInstance<At>().last().target] ?.avatarUrl
                    }
                    message.anyIsInstance<MarketFace>() -> message.firstIsInstance<MarketFace>().queryUrl()
                    message.anyIsInstance<Image>() -> message.firstIsInstance<Image>().queryUrl()
                    args.size > 1 -> {
                        when {
                            args[1] == "自己" -> sender.avatarUrl
                            this is GroupMessageEvent && args[1].any { it.isDigit() } ->
                                bot.getStranger(args[1].filter { it.isDigit() } .toLong()) ?.avatarUrl
                            else -> null
                        }
                    }
                    else -> {
                        quoteReply("请回复需要处理的图片：")
                        nextMessage().firstIsInstanceOrNull<Image>() ?.queryUrl()
                    }
                }
                if (target == null) {
                    quoteReply(unknownMessage)
                    return
                }
                val before = NetworkUtils.downloadTempFile(target)!!
                ImageTemplateFactory.fromConfig(config) ?.let { t ->
                    t.generate(before.toVfs().readNativeImage()).toExternalResource().use {
                        quoteReply(subject.uploadImage(it))
                        quota.update(subject)
                    }
                }
                before.deleteOnExit()
            }
        }
    }
    private fun getTemplateNameList() = ImageTemplateConfigs.templates.map { it.key }
    private val unknownMessage: String
        get() = "使用方法：/生成 模板名 @一个人/qq号/自己/后面跟张图片\n当前支持的模板：" +
                getTemplateNameList().joinToString(", ")
    private const val quotaExceededMessage = "今日该功能限额已经用完了哦~"
}

interface ImageTemplate {
    suspend fun generate(target: Bitmap): ByteArray
}

class StaticTemplate(templatePath: String, configMap: Map<String, String>): ImageTemplate {
    private val width: Int
    private val height: Int
    private val x: Int
    private val y: Int
    private val psLocations: List<PointInt>?
    private var template: Bitmap
    private val templateOnTop: Boolean
    private val circle: Boolean
    private val random: Boolean
    private val angle: Double
    private val rotateCenter: Boolean
    init {
        width = configMap["width"]!!.toInt()
        height = configMap["height"]!!.toInt()
        x = configMap["x"]!!.toInt()
        y = configMap["y"]!!.toInt()
        circle = configMap.getOrDefault("circle", "false").toBoolean()
        random = configMap.getOrDefault("random", "false").toBoolean()
        templateOnTop = configMap.getOrDefault("on_top", "true").toBoolean()
        angle = configMap.getOrDefault("angle", "0").toDouble()
        rotateCenter = configMap.getOrDefault("rotate_center", "true").toBoolean()
        psLocations = configMap["perspective"] ?.split("\n") ?.map { raw ->
            raw.split(",").let {
                PointInt(it.first().toInt(), it.last().toInt())
            }
        }
        runBlocking {
            template = if (random)
                OtomadBotCore
                    .resolveConfigFile("image/template/${templatePath}").toVfs().list().toList().random()
                    .readNativeImage()
            else
                OtomadBotCore
                    .resolveConfigFile("image/template/${templatePath}").toVfs()
                    .readNativeImage()
        }
    }
    override suspend fun generate(target: Bitmap): ByteArray {
        var pre = target.resized(width, height, ScaleMode.EXACT, Anchor.CENTER)
        if (circle)
            pre = pre.circle()
        if (psLocations != null)
            Pseudo3D.computeImage(pre, psLocations[0], psLocations[3], psLocations[2], psLocations[1])

        return NativeImage(template.width, template.height).context2d {
            if (templateOnTop) {
                if (rotateCenter) {
                    drawImage(pre.rotate(angle), x, y)
                } else {
                    rotateDeg(angle)
                    drawImage(pre, x, y)
                    rotateDeg(-angle)
                }
                drawImage(template, 0, 0)
            } else {
                drawImage(template, 0, 0)
                if (rotateCenter) {
                    drawImage(pre.rotate(angle), x, y)
                } else {
                    rotateDeg(angle)
                    drawImage(pre, x, y)
                    rotateDeg(-angle)
                }
            }
        }.encode(PNG)
    }
}

fun getKorimCircle(size: Int, color: RGBA = Colors.WHITE, bg: RGBA = RGBA(0, 0, 0, 0)) =
    NativeImage(size, size).context2d {
        fillStyle = bg
        fillRect(0, 0, width, height)
        beginPath()
        arc(size / 2, size / 2, size / 2, Angle.fromDegrees(0), Angle.fromDegrees(360))
        fillStyle = color
        fill()
    }
fun Bitmap.circle(): Bitmap {
    val circle = getKorimCircle(max(width, height))
    forEach { _, x, y ->
        if (circle.getRgba(x, y).a == 0)
            setRgba(x, y, RGBA(0, 0, 0, 0))
    }
    return this
}

class AnimatedTemplate(templateFolder: String, configMap: Map<String, String>): ImageTemplate {
    private val locations: List<List<Int>>
    private val psLocations: List<PointInt>?
    private val templateOnTop: Boolean
    private var templates: List<VfsFile>
    private val frameRate: Int
    private val circle: Boolean
    init {
        locations = configMap["locations"]!!.split("\n").map {
            it.split(",").map { v -> v.toInt() }
        }
        psLocations = configMap["perspective"] ?.split("\n") ?.map { raw ->
            raw.split(",").let {
                PointInt(it.first().toInt(), it.last().toInt())
            }
        }
        templateOnTop = configMap.getOrDefault("on_top", "true").toBoolean()
        frameRate = configMap.getOrDefault("frame_rate", "60").toInt()
        circle = configMap.getOrDefault("circle", "false").toBoolean()
        runBlocking {
            templates = OtomadBotCore
                .resolveConfigFile("image/template/${templateFolder}").toVfs().list().toList()
                .sortedBy { it.baseNameWithoutExtension.toInt() }
        }
    }
    override suspend fun generate(target: Bitmap): ByteArray {
        var pre = target.resized(locations.maxOf { it[2] }, locations.maxOf { it[3] }, ScaleMode.EXACT, Anchor.CENTER)
        if (circle)
            pre = pre.circle()
        if (psLocations != null)
            Pseudo3D.computeImage(pre, psLocations[0], psLocations[3], psLocations[2], psLocations[1])
        val frames = templates.mapIndexed { i, frame ->
            val template = frame.readNativeImage()
            NativeImage(template.width, template.height).context2d {
                val now = if (psLocations == null)
                    pre.resized(locations[i][2], locations[i][3], ScaleMode.EXACT, Anchor.CENTER)
                else
                    pre
                if (templateOnTop) {
                    locations[i].getOrNull(4) ?.let { deg ->
                        drawImage(now.rotate(deg), locations[i][0], locations[i][1])
                    } ?: run {
                        drawImage(now, locations[i][0], locations[i][1])
                    }
                    drawImage(template, 0, 0)
                } else {
                    drawImage(template, 0, 0)
                    locations[i].getOrNull(4) ?.let { deg ->
                        drawImage(now.rotate(deg), locations[i][0], locations[i][1])
                    } ?: run {
                        drawImage(now, locations[i][0], locations[i][1])
                    }
                }
            }.toAwt()
        }
        return ByteArrayOutputStream().apply {
            StreamingGifWriter(Duration.ofMillis(1000L * frames.size / frameRate), true)
                .prepareStream(this, BufferedImage.TYPE_INT_ARGB).use { gif ->
                    frames.forEach {
                        gif.writeFrame(ImmutableImage.fromAwt(it), DisposeMethod.RESTORE_TO_BACKGROUND_COLOR)
                    }
                }
        }.toByteArray()
    }
}
fun Bitmap.rotate(deg: Number) = let { before ->
    NativeImage(width, height).context2d {
        translate(width / 2, height / 2)
        rotateDeg(deg)
        translate(-width / 2, -height / 2)
        drawImage(before, 0, 0)
    }
}

object ImageTemplateFactory {
    fun fromConfig(config: ImageTemplateConfig): ImageTemplate? = when (config.type) {
        "static" -> StaticTemplate(config.template, config.values)
        "animated" -> AnimatedTemplate(config.template, config.values)
        else -> null
    }
}

object ImageTemplateConfigs: AutoSavePluginConfig("image_template") {
    val templates: Map<String, ImageTemplateConfig> by value(buildMap {
        put("舔", ImageTemplateConfig("static", "prpr.png", buildMap {
            put("width", "330")
            put("height", "330")
            put("x", "56")
            put("y", "284")
            put("perspective", "0,19\n236,0\n287,264\n66,351")
        }))
        put("摸", ImageTemplateConfig("animated", "petpet", buildMap {
            put("locations", "14,20,98,98\n12,33,101,85\n8,40,110,76\n10,33,102,84\n12,20,98,98")
            put("frame_rate", "80")
        }))
        put("膜", ImageTemplateConfig("animated", "worship", buildMap {
            put("perspective", "0,-30\n135,17\n135,145\n0,140")
            put("locations", "0,0,150,150\n".repeat(10).trimEnd { it == '\n' })
            put("frame_rate", "250")
        }))
        put("爬", ImageTemplateConfig("static", "crawl", buildMap {
            put("random", "true")
            put("circle", "true")
            put("width", "100")
            put("height", "100")
            put("x", "0")
            put("y", "400")
            put("on_top", "false")
        }))
        put("精神支柱", ImageTemplateConfig("static", "support.png", buildMap {
            put("angle", "-23")
            put("width", "815")
            put("height", "815")
            put("x", "-275")
            put("y", "210")
            put("rotate_center", "false")
        }))
        put("不要靠近", ImageTemplateConfig("static", "dont_touch.png", buildMap {
            put("width", "170")
            put("height", "170")
            put("x", "23")
            put("y", "231")
            put("on_top", "false")
        }))
        put("一样", ImageTemplateConfig("static", "alike.png", buildMap {
            put("width", "90")
            put("height", "90")
            put("x", "131")
            put("y", "14")
            put("on_top", "false")
        }))
        put("搓", ImageTemplateConfig("animated", "twist", buildMap {
            put("locations", "25,66,78,78,0\n25,66,78,78,60\n23,68,78,78,120\n20,69,78,78,180\n22,68,78,78,240\n25,66,78,78,300")
            put("frame_rate", "50")
        }))
        put("听音乐", ImageTemplateConfig("animated", "listen_music", buildMap {
            put("locations", (0 until 360 step 10).joinToString("\n") { "100,100,215,215,$it" })
            put("frame_rate", "720")
        }))
        put("打拳", ImageTemplateConfig("animated", "punch", buildMap {
            put("locations", "-50,20,260,230\n-40,10,260,230\n-30,0,260,230\n-20,-10,260,230\n-10,-10,260,230\n0,0,260,230\n10,10,260,230\n20,20,260,230\n10,10,260,230\n0,0,260,230\n-10,-10,260,230\n10,0,260,230\n-30,10,260,230")
            put("frame_rate", "433")
        }))
    })
}

@Serializable
open class ImageTemplateConfig(
    val type: String,
    val template: String,
    val values: Map<String, String> = mapOf()
)
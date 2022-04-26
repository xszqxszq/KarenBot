package xyz.xszq.otomadbot.image

import com.sksamuel.scrimage.DisposeMethod
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.composite.MultiplyComposite
import com.sksamuel.scrimage.filter.AlphaMaskFilter
import com.sksamuel.scrimage.nio.StreamingGifWriter
import com.soywiz.korim.awt.toAwt
import com.soywiz.korim.awt.toAwtNativeImage
import com.soywiz.korim.awt.toBMP32
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImageOrBitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.bitmap.resized
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korim.format.readNativeImage
import com.soywiz.korim.paint.Paint
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
import thirdparty.jhlabs.image.MaskFilter
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.core.Quota
import xyz.xszq.otomadbot.core.available
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Duration


object ImageTemplate: EventHandler("图像模板", "image.template") {
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
                    args.size > 1 -> {
                        when {
                            args[1] == "自己" -> sender.avatarUrl
                            this is GroupMessageEvent && args[1].any { it.isDigit() } ->
                                group.members[args[1].filter { it.isDigit() } .toLong()] ?.avatarUrl
                            else -> null
                        }
                    }
                    message.anyIsInstance<MarketFace>() -> message.firstIsInstance<MarketFace>().queryUrl()
                    message.anyIsInstance<Image>() -> message.firstIsInstance<Image>().queryUrl()
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
        get() = "使用方法：/生成 模板名 @一个人/qq号(需在群内)/自己/后面跟张图片\n当前支持的模板：" +
                getTemplateNameList().joinToString(", ")
    private const val quotaExceededMessage = "今日该功能限额已经用完了哦~"
}

interface Template {
    suspend fun generate(target: Bitmap): ByteArray
}

class StaticTemplate(templatePath: String, configMap: Map<String, String>): Template {
    private val width: Int
    private val height: Int
    private val x: Int
    private val y: Int
    private val psLocations: List<PointInt>?
    private var template: Bitmap
    private val templateOnTop: Boolean
    private val circle: Boolean
    private val random: Boolean
    init {
        width = configMap["width"]!!.toInt()
        height = configMap["height"]!!.toInt()
        x = configMap["x"]!!.toInt()
        y = configMap["y"]!!.toInt()
        circle = configMap.getOrDefault("circle", "false").toBoolean()
        random = configMap.getOrDefault("random", "false").toBoolean()
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
        var pre = if (circle)
            target.circle()
        else
            target
        pre = if (psLocations == null)
            pre.resized(width, height, ScaleMode.EXACT, Anchor.CENTER)
        else
            Pseudo3D.computeImage(pre.resized(width, height, ScaleMode.EXACT, Anchor.CENTER),
                psLocations[0], psLocations[3], psLocations[2], psLocations[1])

        return NativeImageOrBitmap32(template.width, template.height).context2d {
            if (templateOnTop) {
                drawImage(pre, x, y)
                drawImage(template, 0, 0)
            } else {
                drawImage(template, 0, 0)
                drawImage(pre, x, y)
            }
        }.encode(PNG)
    }
}

fun getKorimCircle(size: Int, color: Paint = Colors.WHITE) = NativeImageOrBitmap32(size, size)
    .context2d {
        fillStyle = Colors.BLACK
        fillRect(0, 0, width, height)
        beginPath()
        arc(size / 2, size / 2, size / 2, Angle.fromDegrees(0), Angle.fromDegrees(360))
        fillStyle = color
        fill()
    }
fun Bitmap.circle() = ImmutableImage.fromAwt(toAwt())
    .composite(MultiplyComposite(1.0), ImmutableImage.fromAwt(getKorimCircle(width).toAwt()))
    .awt().toAwtNativeImage()

class AnimatedTemplate(templateFolder: String, configMap: Map<String, String>): Template {
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
        var pre = if (circle)
            target.circle()
        else
            target
        pre = if (psLocations == null)
            pre.resized(locations.maxOf { it[2] }, locations.maxOf { it[3] }, ScaleMode.EXACT, Anchor.CENTER)
        else
            Pseudo3D.computeImage(
                pre.resized(locations.maxOf { it[2] }, locations.maxOf { it[3] }, ScaleMode.EXACT, Anchor.CENTER)
                , psLocations[0], psLocations[3], psLocations[2], psLocations[1])
        val frames = templates.mapIndexed { i, frame ->
            val template = frame.readNativeImage()
            NativeImageOrBitmap32(template.width, template.height).context2d {
                val now = if (psLocations == null)
                    pre.resized(locations[i][2], locations[i][3], ScaleMode.EXACT, Anchor.CENTER)
                else
                    pre
                if (templateOnTop) {
                    drawImage(now, locations[i][0], locations[i][1])
                    drawImage(template, 0, 0)
                } else {
                    drawImage(template, 0, 0)
                    drawImage(now, locations[i][0], locations[i][1])
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

object ImageTemplateFactory {
    fun fromConfig(config: ImageTemplateConfig): Template? = when (config.type) {
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
            put("frameRate", "80")
        }))
        put("膜", ImageTemplateConfig("animated", "worship", buildMap {
            put("perspective", "0,-30\n135,17\n135,145\n0,140")
            put("locations", "0,0,150,150\n".repeat(10).trimEnd { it == '\n' })
            put("frameRate", "250")
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
    })
}

@Serializable
open class ImageTemplateConfig(
    val type: String,
    val template: String,
    val values: Map<String, String> = mapOf()
)
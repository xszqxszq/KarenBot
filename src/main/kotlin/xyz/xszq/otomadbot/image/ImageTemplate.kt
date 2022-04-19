package xyz.xszq.otomadbot.image

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.bitmap.sliceWithSize
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korim.format.readNativeImage
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.file.std.toVfs
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.subscribeMessages
import xyz.xszq.otomadbot.core.Quota
import kotlinx.serialization.Serializable
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.nextMessage
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.core.available


object ImageTemplate: EventHandler("图像模板", "image.template") {
    private val quota = Quota("image_template")
    override fun register() {
        GlobalEventChannel.subscribeMessages {
            startsWithSimple("/生成") { raw, _ ->
                requireNot(denied) {
                    if (!available(quota)) {
                        quoteReply(quotaExceededMessage)
                        return@requireNot
                    }
                    val args = raw.replace("@", " ").toArgsList()
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
                                return@requireNot
                            }
                            when (config.type) {
                                "skew" -> {
                                    val before = NetworkUtils.downloadTempFile(target)!!
                                    skew(OtomadBotCore
                                        .resolveConfigFile("image/template/${config.template}").toVfs()
                                        .readNativeImage().toBMP32(), before.toVfs().readNativeImage().toBMP32()
                                        .scaled(config.values["w"]!!.toInt(), config.values["h"]!!.toInt()),
                                        config.values["x"]!!, config.values["y"]!!, config.values["xr"]!!,
                                        config.values["yr"]!!, config.values["deg"]!!
                                    ).encode(PNG).toExternalResource().use {
                                        quoteReply(subject.uploadImage(it))
                                        quota.update(subject)
                                    }
                                    before.deleteOnExit()
                                }
                                else -> {
                                }
                            }
                        }
                    }
                }
            }
        }
        super.register()
    }
    fun skew(template: Bitmap32, target: Bitmap32, x: Double, y: Double, xr: Double, yr: Double, deg: Double): Bitmap32 {
        val temp = Bitmap32(template.width, template.height).context2d {
            (0..target.height/2).forEach { i ->
                setTransform(1.0, xr * i / target.height,
                    0.0, 1.0, 20.0, 20.0)
                drawImage(target,
                    0, target.height / 2 - i, target.width, 2,
                    0, target.height / 2 - i, target.width, 2)
                setTransform(1.0, yr * i / target.height,
                    0.0, 1.0, 20.0, 20.0)
                drawImage(target,
                    0, target.height / 2 + i, target.width, 2,
                    0, target.height / 2 + i, target.width, 2)
            }
        }
        return Bitmap32(template.width, template.height).context2d {
            rotateDeg(deg)
            drawImage(temp, x, y)
            rotateDeg(-deg)
            drawImage(template, 0, 0)
        }
    }

    private fun getTemplateNameList() = ImageTemplateConfigs.templates.map { it.key }
    private val unknownMessage: String
        get() = "使用方法：/生成 模板名 @一个人/qq号(需在群内)/自己/后面跟张图片\n当前支持的模板：" +
                getTemplateNameList().joinToString(", ")
    private const val quotaExceededMessage = "今日该功能限额已经用完了哦~"
}
fun Context2d.drawImage(image: Bitmap32, sx: Int, sy: Int, sWidth: Int, sHeight: Int, dx: Int, dy: Int,
                        dWidth: Int, dHeight: Int) {
    drawImage(image.sliceWithSize(sx, sy, sWidth, sHeight).extract(), dx, dy, dWidth, dHeight)
}

object ImageTemplateConfigs: AutoSavePluginConfig("image_template") {
    val templates by value(buildMap {
        put("舔", ImageTemplateConfig("skew", "prpr.png", buildMap {
            put("x", -42.0)
            put("y", 288.0)
            put("xr", 0.2)
            put("yr", -0.4)
            put("w", 340.0)
            put("h", 340.0)
            put("deg", -11.0)
        }))
    })
}

@Serializable
open class ImageTemplateConfig(
    val type: String,
    val template: String,
    val values: Map<String, Double> = mapOf()
)
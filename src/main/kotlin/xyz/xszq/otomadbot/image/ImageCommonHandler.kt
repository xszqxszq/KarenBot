@file:Suppress("WeakerAccess", "MemberVisibilityCanBePrivate", "unused")
package xyz.xszq.otomadbot.image

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korio.file.std.toVfs
import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.Face
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.MarketFace
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.sendAsImageTo
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.NetworkUtils.getFile
import xyz.xszq.otomadbot.api.PythonApi
import xyz.xszq.otomadbot.core.Cooldown
import xyz.xszq.otomadbot.core.ifReady
import xyz.xszq.otomadbot.core.update
import xyz.xszq.otomadbot.image.ImageMatcher.matchImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.pow
import kotlin.random.Random.Default.nextInt

class ReplyPicList {
    val list = HashMap<String, ArrayList<Path>>()
    val included = mutableListOf<String>()
    fun load(dir: String, target: String = dir) {
        if (!list.containsKey(target)) {
            list[target] = arrayListOf()
            included.add(target)
        }
        Files.walk(OtomadBotCore.configFolder.resolve("image/$dir").toPath(), 2)
            .filter { i -> Files.isRegularFile(i) }
            .forEach { path -> list[target]!!.add(path) }
    }
    fun getRandom(dir: String): File = list[dir]!![nextInt(list[dir]!!.size)].toFile()
    fun isDirIncluded(dir: String) = included.contains(dir)
    fun insert(dir: String, element: String) = list[dir]!!.add(File(element).toPath())
}
object ImageCommonHandler: EventHandler("图片通用功能", "image.common") {
    val parseCooldown = Cooldown("image_parse")
    const val r18Threshold = 0.75
    const val r15Threshold = 0.9
    val hsoList = listOf(PlainText("hso"), Face(Face.SE),
        PlainText("\uD83E\uDD75\uD83E\uDD75\uD83E\uDD75"), PlainText("烧"), PlainText("莫多莫多"))
    private val replyDenied by lazy {
        PermissionService.INSTANCE.register(
            PermissionId("otm", "deny.image.reply"), "禁用自动回复表情包")
    }
    private val ltDetect by lazy {
        PermissionService.INSTANCE.register(
            PermissionId("otm", "image.lt"), "启用龙图检测")
    }
    val replyPic = ReplyPicList()
    override fun register() {
        ltDetect
        GlobalEventChannel.subscribeAlways<GroupMessageEvent> {
            if (message.any { it is Image || it is MarketFace })
                handle(this)
        }
        super.register()
    }
    suspend fun isBlonde(file: File): Boolean {
        var result = false
        val dataset = listOf(
            doubleArrayOf(90.73948779100502,-4.477169141775095,42.932090784625785),
            doubleArrayOf(251.14211886, 246.48062016, 188.24859663).toLab(),
            doubleArrayOf(81.3990919790363,10.1113281133971,23.247659861636638),
            doubleArrayOf(95.85821436038806,-19.323659509977777,67.48143654894507)
        )
        val skin = doubleArrayOf(250.6755287, 233.2529708, 219.23726083).toLab()
        try {
            PythonApi.getHairColor(file.absolutePath)?.split(";")?.forEach out@{ rgb ->
                val lab = rgb.split(',').map { it.toDouble() }.toDoubleArray().toLab()
                println(lab.joinToString(","))
                if (lab.dis(skin) > 100) {
                    dataset.forEach { std ->
                        if (lab.dis(std) < 100) {
                            println("Yes hair matched")
                            result = true
                            return@out
                        }
                    }
                }
            }
        } catch (e: Exception) {
            pass
        }
        return result
    }
    private suspend fun handle(event: GroupMessageEvent) = event.run {
        message.filter { it is Image || it is MarketFace}.fastForEach { msg ->
            val file =
                when (msg) {
                    is Image -> msg.getFile()!!
                    is MarketFace -> msg.getFile()!!
                    else -> File("")
                }
            ifReady(parseCooldown) {
                requireNot(denied) {
                    requireNot(replyDenied) {
                        if ((!file.toVfs().isValidGIF() && matchImage("reply", file)) || isBlonde(file)) {
                            replyPic.getRandom("reply").sendAsImageTo(group)
                            update(parseCooldown)
                        }
                    }
                    try {
                        val result = file.decodeQR()
                        if (urlRegex.matches(result))
                            quoteReply(result)
                        update(parseCooldown)
                        pass
                    } catch (e: Exception) {
                        pass
                    }
                    pass
                }
            }
            require(ltDetect) {
                PythonApi.isLt(file.absolutePath) ?.let { result ->
                    if (result) {
                        if (group.botAsMember.isOperator() && !sender.isOperator()) {
                            message.recall()
                            group.sendMessage("请遵守群规哦")
                        } else {
                            quoteReply("我超，龙")
                        }
                        return@require
                    }
                }
            }
            if (!file.delete()) {
                println("[DEBUG] Delete was failed: " + file.absolutePath)
            }
        }
    }
}

// Reference: https://stackoverflow.com/a/45263428/12944612
fun DoubleArray.toLab(): DoubleArray {
    // --------- RGB to XYZ ---------//
    var r = this[0] / 255.0
    var g = this[1] / 255.0
    var b = this[2] / 255.0
    r = if (r > 0.04045) ((r + 0.055) / 1.055).pow(2.4) else r / 12.92
    g = if (g > 0.04045) ((g + 0.055) / 1.055).pow(2.4) else g / 12.92
    b = if (b > 0.04045) ((b + 0.055) / 1.055).pow(2.4) else b / 12.92
    r *= 100.0
    g *= 100.0
    b *= 100.0
    val x = 0.4124 * r + 0.3576 * g + 0.1805 * b
    val y = 0.2126 * r + 0.7152 * g + 0.0722 * b
    val z = 0.0193 * r + 0.1192 * g + 0.9505 * b
    // --------- XYZ to Lab --------- //
    var xr = x / 95.047
    var yr = y / 100.0
    var zr = z / 108.883
    xr = if (xr > 0.008856) xr.pow(1 / 3.0) else ((7.787 * xr) + 16 / 116.0)
    yr = if (yr > 0.008856) yr.pow(1 / 3.0) else ((7.787 * yr) + 16 / 116.0)
    zr = if (zr > 0.008856) zr.pow(1 / 3.0) else ((7.787 * zr) + 16 / 116.0)
    val lab = DoubleArray(3)
    lab[0] = 116 * yr - 16
    lab[1] = 500 * (xr - yr)
    lab[2] = 200 * (yr - zr)
    return lab
}
fun DoubleArray.dis(target: DoubleArray): Double {
    return (this[0] - target[0]).pow(2) + (this[1] - target[1]).pow(2) + (this[2]-target[2]).pow(2)
}
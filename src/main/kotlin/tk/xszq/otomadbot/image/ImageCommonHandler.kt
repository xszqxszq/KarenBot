@file:Suppress("WeakerAccess", "MemberVisibilityCanBePrivate", "unused")
package tk.xszq.otomadbot.image

import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.anyIsInstance
import net.mamoe.mirai.utils.ExternalResource.Companion.sendAsImageTo
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.NetworkUtils.getFile
import tk.xszq.otomadbot.api.PythonApi
import tk.xszq.otomadbot.core.Cooldown
import tk.xszq.otomadbot.core.OtomadBotCore
import tk.xszq.otomadbot.core.ifReady
import tk.xszq.otomadbot.core.update
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
    private val replyDenied by lazy {
        PermissionService.INSTANCE.register(
            PermissionId("otm", "deny.image.reply"), "禁用自动回复表情包")
    }
    val replyPic = ReplyPicList()
    override fun register() {
        GlobalEventChannel.subscribeAlways<GroupMessageEvent> {
            if (message.anyIsInstance<Image>())
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
        message.forEach { msg ->
            if (msg is Image && msg.imageId.split(".").last() != "gif") {
                ifReady(parseCooldown) {
                    requireNot(denied) {
                        val file = msg.getFile()
                        file?.let { img ->
                            requireNot(replyDenied) {
                                if (isTargetH2Image("reply", img) || isBlonde(img)) {
                                    val pic = replyPic.getRandom("reply")
                                    pic.sendAsImageTo(group)
                                }
                                update(parseCooldown)
                            }
                            try {
                                val result = img.decodeQR()
                                if (urlRegex.matches(result))
                                    quoteReply(result)
                                update(parseCooldown)
                                pass
                            } catch (e: Exception) {
                                pass
                            }
                        }
                        file?.delete()
                    }
                }
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
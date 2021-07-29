@file:Suppress("WeakerAccess", "MemberVisibilityCanBePrivate", "unused")
package tk.xszq.otomadbot.image

import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.anyIsInstance
import net.mamoe.mirai.utils.ExternalResource.Companion.sendAsImageTo
import tk.xszq.otomadbot.EventHandler
import tk.xszq.otomadbot.NetworkUtils.getFile
import tk.xszq.otomadbot.core.OtomadBotCore
import tk.xszq.otomadbot.pass
import tk.xszq.otomadbot.quoteReply
import tk.xszq.otomadbot.requireNot
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
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
    private suspend fun handle(event: GroupMessageEvent) = event.run {
        message.forEach { msg ->
            if (msg is Image && msg.imageId.split(".").last() != "gif") {
                requireNot(denied) {
                    val file = msg.getFile()
                    file ?.let { img ->
                        requireNot(replyDenied) {
                            if (isTargetH2Image("reply", img)) {
                                val pic = replyPic.getRandom("reply")
                                pic.sendAsImageTo(group)
                            }
                        }
                        try {
                            val result = img.decodeQR()
                            quoteReply(result)
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
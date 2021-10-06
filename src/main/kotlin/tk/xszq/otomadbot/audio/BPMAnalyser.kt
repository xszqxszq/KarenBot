package tk.xszq.otomadbot.audio

import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import tk.xszq.otomadbot.EventHandler
import tk.xszq.otomadbot.NetworkUtils
import tk.xszq.otomadbot.api.PythonApi
import tk.xszq.otomadbot.quoteReply
import tk.xszq.otomadbot.startsWithSimple
import kotlin.math.roundToInt


// TODO: Fix it
object BPMAnalyser: EventHandler("测BPM", "audio.bpm") {
    override fun register() {
        GlobalEventChannel.subscribeGroupMessages {
            startsWithSimple("测bpm") { _, file ->
                if (file.isBlank()) {
                    quoteReply("使用方法：测bpm 群文件名")
                    return@startsWithSimple
                }
                val targetFile = group.filesRoot.resolve(file)
                targetFile.getInfo() ?: run {
                    quoteReply("文件不存在，请检查拼写！")
                    return@startsWithSimple
                }
                if (targetFile.getInfo()?.length!! >= 20971520L) {
                    quoteReply("文件大小请勿超过20M :(")
                    return@startsWithSimple
                }
                val target = NetworkUtils.downloadTempFile(targetFile.getDownloadInfo()!!.url,
                    ext = targetFile.name.split(".").last())!!
                val before = AudioEncodeUtils.anyToWav(target)!!
                val bpm = PythonApi.getBPM(before.absolutePath)!!
                quoteReply(bpm.roundToInt().toString() + " (%.3f)".format(bpm))
                before.delete()
                target.delete()
            }
        }
        super.register()
    }
}
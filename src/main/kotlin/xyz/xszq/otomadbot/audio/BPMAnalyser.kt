package xyz.xszq.otomadbot.audio

import kotlinx.coroutines.flow.toList
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import xyz.xszq.otomadbot.EventHandler
import xyz.xszq.otomadbot.NetworkUtils
import xyz.xszq.otomadbot.api.PythonApi
import xyz.xszq.otomadbot.quoteReply
import xyz.xszq.otomadbot.startsWithSimple
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
                val targetFile = group.files.root.resolveFiles(file).toList().firstOrNull()
                targetFile ?: run {
                    quoteReply("文件不存在，请检查拼写！")
                    return@startsWithSimple
                }
                if (targetFile.size >= 20971520L) {
                    quoteReply("文件大小请勿超过20M :(")
                    return@startsWithSimple
                }
                val target = NetworkUtils.downloadTempFile(targetFile.getUrl()!!,
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
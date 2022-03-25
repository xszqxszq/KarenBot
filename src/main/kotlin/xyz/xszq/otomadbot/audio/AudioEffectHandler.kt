package xyz.xszq.otomadbot.audio

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.core.BinConfig
import java.io.File

object AudioEffectHandler: EventHandler("音频效果", "audio.effect") {
    override fun register() {
        GlobalEventChannel.subscribeGroupMessages {
            startsWithSimple("帮我修音", true) { _, path ->
                handlePitchShift(path, this)
            }
        }
        super.register()
    }
    private suspend fun handlePitchShift(path: String, event: GroupMessageEvent) = event.run {
        if (path.isBlank()) {
            quoteReply("请指定欲修音的文件名（仅支持WAV格式）！")
        } else {
            val file = group.files.root.resolveFiles(path).toList()
            if (file.isEmpty()) {
                quoteReply("文件不存在，请检查是否有拼写错误")
            } else if (file.first().size > 10485760L) {
                quoteReply("文件不得超过10M")
            } else {
                quoteReply("正在处理中，请稍等片刻……")
                val target = file.first()
                val url = target.getUrl()!!
                val raw = NetworkUtils.downloadTempFile(url, ext = File(target.name).extension)!!
                if (raw.getAudioDuration() > 10.0) {
                    quoteReply("文件不得超过10s")
                } else {
                    val before = AudioEncodeUtils.anyToWav(raw)!!
                    val command = "${BinConfig.python} ${BinConfig.pitchshift} " +
                            before.absolutePath
                    bot.logger.debug(command)
                    ProgramExecutor(command).start()
                    val result = File(before.absolutePath + ".result.wav")
                    result.toExternalResource().use {
                        try {
                            val uploaded = group.files.uploadNewFile("/${before.name}", it)
                            quoteReply("修音成功，该文件将在10min内被撤回。")
                            delay(600000)
                            uploaded.delete()
                        } catch (e: Exception) {
                            quoteReply("文件上传失败")
                            e.printStackTrace()
                        }
                    }
                    raw.delete()
                    result.delete()
                }
            }
        }
    }
}
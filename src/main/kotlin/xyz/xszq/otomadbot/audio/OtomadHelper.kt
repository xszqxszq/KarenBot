package xyz.xszq.otomadbot.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import xyz.xszq.events
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.api.PythonApi
import xyz.xszq.otomadbot.mirai.quoteReply
import xyz.xszq.otomadbot.mirai.startsWithSimple
import java.io.File
import kotlin.math.roundToInt

object OtomadHelper: CommandModule("音MAD功能", "otomad") {
    override suspend fun subscribe() {
        events.subscribeGroupMessages {
            startsWithSimple("帮我修音", true) { _, path ->
                pitchShift.checkAndRun(CommandEvent(listOf(path), this))
            }
            startsWithSimple("测bpm") { _, file ->
                bpm.checkAndRun(CommandEvent(listOf(file), this))
            }
        }
    }
    // TODO: Change to Java wrapper of world
    val pitchShift = GroupCommandWithArgs("帮我修音", "pitch_shift") {
        val path = args.first()
        if (path.isBlank()) {
            event.quoteReply("使用本命令时请指定欲修音的文件名（仅支持WAV格式）！")
        } else {
            val file = event.group.files.root.resolveFiles(path).toList()
            if (file.isEmpty()) {
                event.quoteReply("文件不存在，请检查是否有拼写错误")
            } else if (file.first().size > 10485760L) {
                event.quoteReply("文件不得超过10M")
            } else {
                event.quoteReply("正在处理中，请稍等片刻……")
                val target = file.first()
                withContext(Dispatchers.IO) {
                    val url = target.getUrl()!!
                    val raw = NetworkUtils.downloadTempFile(url, ext = File(target.name).extension)!!
                    if (raw.getAudioDuration() > 10.0) {
                        event.quoteReply("文件不得超过10s")
                    } else {
                        val before = AudioEncodeUtils.anyToWav(raw)!!
                        val command = "${BinConfig.data.values["python"]} ${BinConfig.data.values["pitch_shift"]} " +
                                before.absolutePath
                        event.bot.logger.debug(command)
                        ProgramExecutor(command).start()
                        val result = File(before.absolutePath + ".result.wav")
                        result.toExternalResource().use {
                            try {
                                val uploaded = event.group.files.uploadNewFile("/${before.name}", it)
                                event.quoteReply("修音成功，该文件将在10min内被撤回。")
                                delay(600000)
                                uploaded.delete()
                            } catch (e: Exception) {
                                event.quoteReply("文件上传失败")
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
    // TODO: Implement this in Kotlin
    val bpm = GroupCommandWithArgs("测BPM", "bpm") {
        val file = args.first()
        if (file.isBlank()) {
            event.quoteReply("使用方法：测bpm 群文件名")
            return@GroupCommandWithArgs
        }
        val targetFile = event.group.files.root.resolveFiles(file).toList().firstOrNull()
        targetFile ?: run {
            event.quoteReply("文件不存在，请检查拼写！")
            return@GroupCommandWithArgs
        }
        if (targetFile.size >= 20971520L) {
            event.quoteReply("文件大小请勿超过20M :(")
            return@GroupCommandWithArgs
        }
        withContext(Dispatchers.IO) {
            val target = NetworkUtils.downloadTempFile(targetFile.getUrl()!!,
                ext = targetFile.name.split(".").last())!!
            val before = AudioEncodeUtils.anyToWav(target)!!
            val bpm = PythonApi.getBPM(before.absolutePath)!!
            event.quoteReply(bpm.roundToInt().toString() + " (%.3f)".format(bpm))
            before.delete()
            target.delete()
        }
    }
}
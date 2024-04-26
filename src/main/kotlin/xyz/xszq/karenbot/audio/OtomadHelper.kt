package xyz.xszq.karenbot.audio

import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.std.toVfs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import xyz.xszq.events
import xyz.xszq.karenbot.*
import xyz.xszq.karenbot.api.PythonApi
import xyz.xszq.karenbot.mirai.quoteReply
import xyz.xszq.karenbot.mirai.startsWithSimple
import java.io.File
import kotlin.math.roundToInt

object OtomadHelper: CommandModule("音MAD功能", "otomad") {
    override suspend fun subscribe() {
        events.subscribeGroupMessages {
            startsWithSimple("帮我修音", true) { _, file ->
                pitchShift.checkAndRun(this, file)
            }
            startsWithSimple("测bpm") { _, file ->
                bpm.checkAndRun(this, file)
            }
        }
    }
    // TODO: Change to Java wrapper of world
    val pitchShift = GroupCommandWithArg("帮我修音", "pitch_shift") { path ->
        if (path!!.isBlank()) {
            quoteReply("使用本命令时请指定欲修音的文件名（仅支持WAV格式）！")
        } else {
            val file = group.files.root.resolveFiles(path).toList()
            if (file.isEmpty()) {
                quoteReply("文件不存在，请检查是否有拼写错误")
            } else if (file.first().size > 10485760L) {
                quoteReply("文件不得超过10M")
            } else {
                quoteReply("正在处理中，请稍等片刻……")
                val target = file.first()
                withContext(Dispatchers.IO) {
                    val url = target.getUrl()!!
                    val raw = NetworkUtils.downloadTempFile(url, ext = File(target.name).extension)!!.toVfs()
                    if (raw.getAudioDuration() > 10.0) {
                        quoteReply("文件不得超过10s")
                    } else {
                        val before = AudioEncodeUtils.anyToWav(raw)
                        val command = listOf(BinConfig.data.values["python"]!!, BinConfig.data.values["pitch_shift"]!!, before.absolutePath)
                        bot.logger.debug(command.joinToString(" "))
                        ProgramExecutor(command).start()
                        val result = File(before.absolutePath + ".result.wav")
                        result.toExternalResource().use {
                            try {
                                val uploaded = group.files.uploadNewFile("/${before.baseName}", it)
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
    // TODO: Implement this in Kotlin
    val bpm = GroupCommandWithArg("测BPM", "bpm") { file ->
        if (file!!.isBlank()) {
            quoteReply("使用方法：测bpm 群文件名")
            return@GroupCommandWithArg
        }
        val targetFile = group.files.root.resolveFiles(file).toList().firstOrNull()
        targetFile ?: run {
            quoteReply("文件不存在，请检查拼写！")
            return@GroupCommandWithArg
        }
        if (targetFile.size >= 20971520L) {
            quoteReply("文件大小请勿超过20M :(")
            return@GroupCommandWithArg
        }
        withContext(Dispatchers.IO) {
            val target = NetworkUtils.downloadTempFile(targetFile.getUrl()!!,
                ext = targetFile.name.split(".").last())!!.toVfs()
            val before = AudioEncodeUtils.anyToWav(target)
            val bpm = PythonApi.getBPM(before.absolutePath)!!
            quoteReply(bpm.roundToInt().toString() + " (%.3f)".format(bpm))
            before.delete()
            target.delete()
        }
    }
}
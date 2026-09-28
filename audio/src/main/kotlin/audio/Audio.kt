package xyz.xszq.bot.audio

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import io.ktor.client.plugins.*
import korlibs.io.async.launch
import korlibs.io.file.VfsFile
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.withContext
import xyz.xszq.bot.Plugin
import xyz.xszq.bot.audio.touhou.Touhou
import xyz.xszq.bot.audio.voice.TTSParser
import xyz.xszq.bot.audio.voice.VoicePresets
import xyz.xszq.bot.ffmpeg.FFMpegFileType
import xyz.xszq.bot.ffmpeg.FFMpegTask
import xyz.xszq.bot.message.Audio
import xyz.xszq.bot.message.RemoteVoice
import xyz.xszq.bot.reply
import xyz.xszq.bot.util.cpuDispatcher
import xyz.xszq.bot.util.use
import java.io.File

/**
 * 音频插件
 */
@Suppress("unused")
class Audio: Plugin() {
    lateinit var presets: VoicePresets
    lateinit var tts: TTSParser
    val touhou = Touhou(this)

    private companion object {
        val auditPrompt = buildString {
            appendLine("审核活字印刷的输入文本，只审核用户输入。")
            appendLine("仅政治敏感返回 false。")
            appendLine("政治敏感指现实政治人物、重大政治事件、政治组织、口号、机构、")
            appendLine("政策与意识形态争议，以及明显代称、谐音、缩写或影射。")
            appendLine("其余内容，包括脏话辱骂、普通玩梗和拿不准的情况都返回 true。")
            appendLine("只输出 true 或 false。")
        }.trim()

        val audioAuditPrompt = buildString {
            appendLine("审核音频内容是否合规。")
            appendLine("仅政治敏感返回 false。")
            appendLine("政治敏感指现实政治人物、重大政治事件、政治组织、口号、机构、")
            appendLine("政策与意识形态争议，以及明显代称、谐音、缩写或影射。")
            appendLine("其余内容，包括色情、脏话辱骂、玩梗、听不清和拿不准的情况都返回 true。")
            appendLine("只输出 true 或 false。")
        }.trim()
    }

    @OptIn(ExperimentalHoplite::class)
    override suspend fun load() {
        presets = ConfigLoaderBuilder.default()
            .addFileSource("./data/audio/otto/presets.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<VoicePresets>()

        tts = TTSParser(presets, localCurrentDirVfs["data/audio/otto"])
        tts.init()
        touhou.init()

        setRoute()
        touhou.setRoute()
        logger.info { "[音频] 插件加载完成。" }
    }

    /**
     * 注册路由
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun setRoute() = route {
        // 活字印刷
        startsWith("活字印刷") { text ->
            if (text.isBlank() || text.length >= 120) {
                reply(buildString {
                    appendLine("使用方法：/活字印刷 文本")
                    appendLine("例：/活字印刷 大家好啊，我是可怜Bot")
                    appendLine("注：文本字数需在120字以内。")
                }.trim())
                return@startsWith
            }
            if (!audit(text)) {
                reply("检测到疑似违规内容，请检查输入")
                return@startsWith
            }

            launch(cpuDispatcher) {
                runCatching {
                    tts.generate(text) ?.let { pcm ->
                        pcm.use { reply(Audio(pcm)) }
                    } ?: reply("输入的文本貌似未包含有效内容，请重试")
                }.onFailure { e ->
                    logger.error(e) { "TTS 生成失败" }
                    reply("语音生成失败，请稍后再试")
                }
            }
        }
        // 倒放语音
        startsWith(listOf("倒放", "逆再生")) {
            reference ?.filterIsInstance<RemoteVoice>() ?.firstOrNull() ?.use { voice ->
                val wav = FFMpegTask(FFMpegFileType.WAV) {
                    input(File(voice.absolutePath))
                    audioFilter("areverse")
                    yes()
                    forceFormat("wav")
                    audioCodec("pcm_s16le")
                    logLevel("warning")
                    audioRate("24k")
                    audioChannels(1)
                }.result()
                wav.use { reversed ->
                    if (!audit(reversed)) {
                        reply("检测到疑似违规内容，请检查输入")
                        return@use
                    }
                    val pcm = FFMpegTask(FFMpegFileType.PCM) {
                        input(File(reversed.absolutePath))
                        yes()
                        forceFormat("s16le")
                        audioCodec("pcm_s16le")
                        logLevel("warning")
                        audioRate("24k")
                        audioChannels(1)
                    }.result()
                    reply(withContext(cpuDispatcher) { Audio(pcm) })
                    pcm.delete()
                }
            }
        }
    }

    /**
     * 用 LLM 审核活字印刷的用户输入
     *
     * @param input 用户输入
     * @return 是否合规
     */
    suspend fun audit(input: String): Boolean {
        val client = pluginLoader.llmClient ?: return true
        return runCatching {
            val content = client.chat(scene = "audit") {
                system(auditPrompt)
                user(input)
            }
            content.trim() == "true"
        }.getOrElse { e ->
            e !is ClientRequestException
        }
    }

    /**
     * 用 LLM 审核倒放后的音频
     *
     * @param audio 要审核的倒放音频
     * @return 是否合规
     */
    suspend fun audit(audio: VfsFile): Boolean {
        val client = pluginLoader.llmClient ?: return true
        return runCatching {
            val data = audio.readBytes()
            val content = client.chat(scene = "audit") {
                system(audioAuditPrompt)
                user {
                    text("审核这段音频。")
                    audio(data)
                }
            }
            content.trim() == "true"
        }.getOrElse { e ->
            e !is ClientRequestException
        }
    }

    override suspend fun unload() {
        logger.info { "[活字印刷] 插件卸载完成" }
    }
}
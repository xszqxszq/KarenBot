package xyz.xszq.bot.audio

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import korlibs.io.async.launch
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import xyz.xszq.bot.Plugin
import xyz.xszq.bot.audio.touhou.Touhou
import xyz.xszq.bot.audio.voice.TTSParser
import xyz.xszq.bot.audio.voice.VoicePresets
import xyz.xszq.bot.ffmpeg.FFMpegFileType
import xyz.xszq.bot.ffmpeg.FFMpegTask
import xyz.xszq.bot.message.Audio
import xyz.xszq.bot.message.RemoteVoice
import xyz.xszq.bot.reply
import xyz.xszq.bot.util.use
import java.io.File

/**
 * 音频插件
 *
 * 活字印刷相关，以及东方原曲相关功能，音频位置位于 data/audio
 */
@Suppress("unused")
class Audio: Plugin() {
    lateinit var presets: VoicePresets
    lateinit var tts: TTSParser
    val touhou = Touhou(this)

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

            // TODO: 加入文本审查
            launch(Dispatchers.IO) {
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
                // TODO: 加入TTS/多模态审查
                val pcm = FFMpegTask(FFMpegFileType.PCM) {
                    input(File(voice.absolutePath))
                    audioFilter("areverse")
                    yes()
                    forceFormat("s16le")
                    audioCodec("pcm_s16le")
                    logLevel("warning")
                    audioRate("24k")
                    audioChannels(1)
                }.result()
                reply(Audio(pcm))
                pcm.delete()
            }
        }
    }

    override suspend fun unload() {
        logger.info { "[活字印刷] 插件卸载完成" }
    }
}
package xyz.xszq.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import korlibs.io.async.launch
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import xyz.xszq.bot.ffmpeg.FFMpegFileType
import xyz.xszq.bot.ffmpeg.FFMpegTask
import xyz.xszq.bot.message.Audio
import xyz.xszq.bot.message.RemoteVoice
import xyz.xszq.bot.voice.TTSParser
import xyz.xszq.bot.voice.VoicePresets
import java.io.File

@Suppress("unused")
class OttoVoice: Plugin() {
    lateinit var presets: VoicePresets
    lateinit var tts: TTSParser
    @OptIn(ExperimentalHoplite::class)
    override suspend fun load() {
        presets = ConfigLoaderBuilder.default()
            .addFileSource("./data/audio/otto/presets.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<VoicePresets>()

        tts = TTSParser(presets, localCurrentDirVfs["data/audio/otto"])
        tts.init()

        setRoute()
        logger.info { "[活字印刷] 插件加载完成。" }
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun setRoute() = route {
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
                tts.generate(text).use { pcm ->
                    reply(Audio(pcm))
                }
            }
        }
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
                println(pcm)
                reply(Audio(pcm))
                pcm.delete()
            }
        }
    }

    override suspend fun unload() {
        logger.info { "Module OttoVoice unloaded." }
    }
}
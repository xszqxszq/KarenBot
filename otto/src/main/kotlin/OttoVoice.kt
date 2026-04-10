package xyz.xszq.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import korlibs.io.async.launch
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import xyz.xszq.bot.message.Audio
import xyz.xszq.bot.voice.TTSParser
import xyz.xszq.bot.voice.VoicePresets

@Suppress("unused")
class OttoVoice: Plugin() {
    lateinit var presets: VoicePresets
    lateinit var tts: TTSParser
    @OptIn(ExperimentalHoplite::class)
    override suspend fun load() {
        presets = ConfigLoaderBuilder.Companion.default()
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

            launch(Dispatchers.IO) {
                tts.generate(text).use { pcm ->
                    reply(Audio(pcm))
                }
            }
        }
    }

    override suspend fun unload() {
        logger.info { "Module OttoVoice unloaded." }
    }
}
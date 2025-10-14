package xyz.xszq.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import xyz.xszq.bot.message.Audio
import xyz.xszq.bot.voice.OttoConfig
import xyz.xszq.bot.voice.TTSParser

@Suppress("unused")
class OttoVoice: Plugin() {
    lateinit var config: OttoConfig
    lateinit var tts: TTSParser
    @OptIn(ExperimentalHoplite::class)
    override fun load() {
        config = ConfigLoaderBuilder.Companion.default()
            .addFileSource("./config/otto.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<OttoConfig>()

        tts = TTSParser(config, localCurrentDirVfs["data/audio/otto"])

        setRoute()
        logger.info { "[活字印刷] 插件加载完成。" }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun setRoute() = route {
        startsWith("活字印刷") { text ->
            if (text.isBlank() || text.length >= 120) {
                reply(buildString {
                    appendLine("使用方法：/活字印刷 文本")
                    appendLine("例：/活字印刷 大家好啊，我是可怜Bot")
                    appendLine("注：文本字数需在120字以内。")
                }.trim())
                return@startsWith
            }

            GlobalScope.launch {
                tts.generate(text).use { pcm ->
                    reply(Audio(pcm))
                }
            }
        }
    }

    override fun unload() {
        logger.info { "Module OttoVoice unloaded." }
    }
}
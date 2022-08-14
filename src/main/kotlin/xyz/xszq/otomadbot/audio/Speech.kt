package xyz.xszq.otomadbot.audio

import com.github.pemistahl.lingua.api.Language
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder
import com.soywiz.korio.lang.UTF8
import com.soywiz.korio.net.QueryString
import com.soywiz.korio.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.AudioSupported
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import xyz.xszq.events
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.api.PythonApi
import xyz.xszq.otomadbot.api.availableUA
import xyz.xszq.otomadbot.mirai.startsWithSimple
import java.io.File

object Speech: CommandModule("语音功能", "speech") {
    val detector = LanguageDetectorBuilder.fromAllLanguages().build()
    val japaneseRegex = "([ぁ-んァ-ン])".toRegex()
    private val cooldown = Cooldown("speech")
    override suspend fun subscribe() {
        events.subscribeMessages {
            startsWithSimple("朗读") { _, text ->
                tts.checkAndRun(CommandEvent(listOf(detectLanguage(text).isoCode639_1.name, text), this))
            }
            listOf("汉语朗读", "中文朗读").forEach {
                startsWithSimple(it) { _, text ->
                    tts.checkAndRun(CommandEvent(listOf("ZH", text), this))
                }
            }
            startsWithSimple("日语朗读") { _, text ->
                tts.checkAndRun(CommandEvent(listOf("JA", text), this))
            }
        }
    }
    val tts = CommonCommandWithArgs("朗读", "tts") {
        event.ifReady(cooldown) {
            withContext(Dispatchers.IO) {
                val language = args.first()
                val text = args.last()
                val file = when (language) {
                    "JA" -> File(PythonApi.getTTS(text)!!)
                    else -> googleTranslate(text, language) ?: googleTranslate(text)
                }
                file ?.let { f ->
                    val final = AudioEncodeUtils.prepareAudio(f)!!
                    final.toExternalResource().use {
                        event.subject.sendMessage((event.subject as AudioSupported).uploadAudio(it))
                    }
                    final.delete()
                }
                file?.deleteOnExit()
            }
            cooldown.update(event.subject)
        }
    }
    suspend fun detectLanguage(text: String): Language = withContext(Dispatchers.IO) {
        if (japaneseRegex.find(text) != null)
            Language.JAPANESE
        detector.detectLanguageOf(text)
    }
    /**
     * Download TTS results from Google.
     * @param text Text to speak
     * @param lang Language of the text
     */
    suspend fun googleTranslate(text: String, lang: String = "zh-CN") = NetworkUtils.downloadTempFile(
        "https://translate.google.com/translate_tts?" + QueryString.encode(
            Pair("ie", "UTF-8"), Pair("total", "1"), Pair("idx", "0"), Pair("textlen",
                if (text.length < 10) "16" else text.length.toString()),
            Pair("client", "tw-ob"), Pair("q", text), Pair("tl", lang)
        ), listOf(
            Pair("Referer", "https://translate.google.com/?sl=auto&tl=zh-CN&text="
                    + URL.encodeComponent(text, UTF8, formUrlEncoded = true)
                    + "&op=translate"),
            Pair("User-Agent", availableUA)
        ), "mp3", true
    )
}
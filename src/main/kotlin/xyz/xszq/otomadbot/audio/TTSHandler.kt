package xyz.xszq.otomadbot.audio

import com.soywiz.korio.lang.UTF8
import com.soywiz.korio.net.QueryString
import com.soywiz.korio.net.URL
import io.ktor.client.request.*
import net.mamoe.mirai.contact.AudioSupported
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.jsoup.Jsoup
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.api.ApiClient
import xyz.xszq.otomadbot.api.PythonApi
import xyz.xszq.otomadbot.core.Cooldown
import xyz.xszq.otomadbot.core.ifReady
import xyz.xszq.otomadbot.core.remaining
import xyz.xszq.otomadbot.core.update
import java.io.File

object TTSHandler: EventHandler("TTS", "audio.tts") {
    private val cooldown = Cooldown("tts")
    override fun register() {
        GlobalEventChannel.subscribeGroupMessages {
            startsWithSimple("朗读") { text, _ ->
                requireNot(denied) {
                    ifReady(cooldown) {
                        update(cooldown)
                        if (text.isNotBlank()) {
                            val lang = PythonApi.getLanguage(text)
                            val raw = lang?.let {
                                if (lang == "ja") TTSDownloader.yukkuri(text)
                                else TTSDownloader.googleTranslate(text, it)
                            } ?: run { TTSDownloader.googleTranslate(text) }
                            val encoded = AudioEncodeUtils.convertAnyToSilk(raw!!)
                            encoded?.let { file ->
                                file.toExternalResource().use {
                                    subject.sendMessage((subject as AudioSupported).uploadAudio(it))
                                }
                            }
                            raw.delete()
                            encoded?.delete()
                        }
                        pass
                    } ?: run {
                        quoteReply("让我歇个 ${remaining(cooldown)} 秒再读，好吗QWQ")
                    }
                }
            }
        }
        super.register()
    }
}
object TTSDownloader: ApiClient() {
    /**
     * Download TTS results from yukkuritalk.com.
     * @param text Text to speak
     * @param mode Yukkuri speak mode
     */
    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    suspend fun yukkuri(text: String, mode: String = "ゆっくり言っていてね"): File? {
        val url = "http://yukkuritalk.com/?" + QueryString.encode(Pair("txt", text), Pair("submit", mode))
        val doc = Jsoup.parse(client.get<String>(url))!!
        val audio = doc.select(".translate_yukkuri>audio")
        return if (audio.isNotEmpty())
            NetworkUtils.downloadTempFile(
                "http://yukkuritalk.com/" + audio.first()!!.attr("src"),
                listOf(
                    Pair("Referer", "http://yukkuritalk.com/"),
                    Pair("User-Agent", availableUA)
                ), "wav")
        else null
    }
    /**
     * Download TTS results from Google.
     * @param text Text to speak
     * @param lang Language of the text
     */
    fun googleTranslate(text: String, lang: String = "zh-CN") = NetworkUtils.downloadTempFile(
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
package tk.xszq.otomadbot.audio

import com.soywiz.korio.lang.UTF8
import com.soywiz.korio.net.QueryString
import com.soywiz.korio.net.URL
import net.mamoe.mirai.contact.AudioSupported
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import ru.gildor.coroutines.okhttp.await
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.api.PythonApi
import java.io.File

object TTSHandler: EventHandler("TTS", "audio.tts") {
    override fun register() {
        GlobalEventChannel.subscribeMessages {
            startsWithSimple("朗读") { text, _ ->
                requireNot(denied) {
                    if (text.isNotBlank() && subject is AudioSupported) {
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
                }
            }
        }
        super.register()
    }
}
object TTSDownloader {
    /**
     * Download TTS result from yukkuritalk.com.
     * @param text Text to speak
     * @param mode Yukkuri speak mode
     */
    suspend fun yukkuri(text: String, mode: String = "ゆっくり言っていてね"): File? {
        val url = "http://yukkuritalk.com/?" + QueryString.encode(Pair("txt", text), Pair("submit", mode))
        val response = OkHttpClient().newCall(Request.Builder().url(url).build()).await()
        return if (response.isSuccessful) {
            val doc = Jsoup.parse(response.body!!.get())
            val audio = doc.select(".translate_yukkuri>audio")
            if (audio.isNotEmpty())
                NetworkUtils.downloadTempFile(
                    "http://yukkuritalk.com/" + audio.first().attr("src"),
                    listOf(
                        Pair("Referer", "http://yukkuritalk.com/"),
                        Pair("User-Agent", availableUA)
                    ), "wav")
            else null
        } else null
    }
    /**
     * Download TTS result from Google.
     * @param text Text to speak
     * @param lang Language of the text
     */
    fun googleTranslate(text: String, lang: String = "zh-CN") = NetworkUtils.downloadTempFile(
        "https://translate.google.cn/translate_tts?" + QueryString.encode(
            Pair("ie", "UTF-8"), Pair("total", "1"), Pair("idx", "0"), Pair("textlen",
                if (text.length < 10) "16" else text.length.toString()),
            Pair("client", "tw-ob"), Pair("q", text), Pair("tl", lang)
        ), listOf(
            Pair("Referer", "https://translate.google.cn/?sl=auto&tl=zh-CN&text="
                    + URL.encodeComponent(text, UTF8, formUrlEncoded = true)
                    + "&op=translate"),
            Pair("User-Agent", availableUA)
        ), "mp3", true
    )
}
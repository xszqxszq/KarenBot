@file:Suppress("EXPERIMENTAL_API_USAGE", "unused")

package tk.xszq.otomadbot.api

import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.SimpleServiceMessage
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import ru.gildor.coroutines.okhttp.await
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.text.TextSettings

object Midishow: EventHandler("MIDI搜索", "audio.midishow") {
    override fun register() {
        GlobalEventChannel.subscribeGroupMessages {
            finding(Regex(TextSettings.regex.midishow)) { finding ->
                requireNot(denied) {
                    finding.groupValues.lastOrNull { i -> i.isNotBlank() }?.let { keyword ->
                        handle(this, keyword)
                    }
                }
            }
        }
        super.register()
    }
    private suspend fun handle(event: MessageEvent, keyword: String) = event.run {
        val request = Request.Builder()
            .url("https://www.midishow.com/search/result?q=$keyword")
            .addHeader("Referer", "https://www.midishow.com/")
            .addHeader("User-Agent", availableUA)
            .build()
        val response = OkHttpClient().newCall(request).await()
        if (response.isSuccessful) {
            val results = Jsoup.parse(response.body!!.get()).select("#search-result>div")
            if (results.size == 0 || results[0].attr("data-key") == "") {
                quoteReply("没有找到相关MIDI……")
            } else {
                val title = results[0].select(".text-hover-primary").text()
                val summary = """上传用户：${results[0].select(".avatar-img").attr("alt")}
                        乐曲时长：${results[0].select("[title=\"乐曲时长\"]").text()}
                        音轨数量：${results[0].select("[title=\"音轨数量\"]").text()}""".trimLiteralTrident()
                subject.sendMessage(SimpleServiceMessage(60, QQXMLMessage {
                    brief("[MIDI]$title")
                    url("https://www.midishow.com/midi/${results[0].attr("data-key")}.html")
                    title("$title :: MidiShow")
                    summary(summary)
                    cover("https://www.midishow.com/favicon.ico")
                }.build()))
            }
        } else {
            quoteReply("网络错误，MIDI搜索失败")
        }
    }
}
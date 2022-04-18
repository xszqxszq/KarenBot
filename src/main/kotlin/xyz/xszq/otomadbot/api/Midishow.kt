@file:Suppress("EXPERIMENTAL_API_USAGE", "unused")

package xyz.xszq.otomadbot.api

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.SimpleServiceMessage
import org.jsoup.Jsoup
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.text.TextSettings

object Midishow: EventHandler("MIDI搜索", "audio.midishow") {
    val client = HttpClient()
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
        val response = client.get("https://www.midishow.com/search/result?q=$keyword") {
            headers {
                append("Referer", "https://www.midishow.com/")
                append("User-Agent", availableUA)
            }
        }
        if (response.status == HttpStatusCode.OK) {
            val results = Jsoup.parse(response.bodyAsText()).select("#search-result>div")
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
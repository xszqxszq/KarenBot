package xyz.xszq.otomadbot.audio

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.SimpleServiceMessage
import org.jsoup.Jsoup
import xyz.xszq.events
import xyz.xszq.otomadbot.CommandEvent
import xyz.xszq.otomadbot.CommandModule
import xyz.xszq.otomadbot.GroupCommand
import xyz.xszq.otomadbot.GroupCommandWithArgs
import xyz.xszq.otomadbot.api.availableUA
import xyz.xszq.otomadbot.kotlin.trimLiteralTrident
import xyz.xszq.otomadbot.mirai.quoteReply
import xyz.xszq.otomadbot.mirai.startsWithSimple
import xyz.xszq.otomadbot.text.TextSettings

object MidiShow: CommandModule("midi搜索", "midishow") {
    val client = HttpClient()
    override suspend fun subscribe() {
        events.subscribeGroupMessages {
            finding(Regex(TextSettings.data.values["regex_midishow"]!!)) { finding ->
                finding.groupValues.lastOrNull { i -> i.isNotBlank() }?.let { keyword ->
                    search.checkAndRun(CommandEvent(listOf(keyword), this))
                }
            }
        }
    }

    val search = GroupCommandWithArgs("midi搜索", "search") {
        val keyword = args.first()
        val response = client.get<HttpResponse>("https://www.midishow.com/search/result?q=$keyword") {
            headers {
                append("Referer", "https://www.midishow.com/")
                append("User-Agent", availableUA)
            }
        }
        if (response.status == HttpStatusCode.OK) {
            val results = Jsoup.parse(response.readText()).select("#search-result>div")
            if (results.size == 0 || results[0].attr("data-key") == "") {
                event.quoteReply("没有找到相关MIDI……")
            } else {
                val title = results[0].select(".text-hover-primary").text()
                val summary = """上传用户：${results[0].select(".avatar-img").attr("alt")}
                        乐曲时长：${results[0].select("[title=\"乐曲时长\"]").text()}
                        音轨数量：${results[0].select("[title=\"音轨数量\"]").text()}""".trimLiteralTrident()
                val text = "https://www.midishow.com/midi/${results[0].attr("data-key")}.html\n" +
                        "$title\n" + summary
                event.quoteReply(text)
            }
        } else {
            event.quoteReply("网络错误，MIDI搜索失败")
        }
    }
}
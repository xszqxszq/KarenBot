@file:Suppress("EXPERIMENTAL_API_USAGE")

package xyz.xszq.otomadbot.text

import com.soywiz.korio.net.URL
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.OtomadBotCore

@Serializable
data class MWSearchResultInfo(val totalhits: Int)
@Serializable
data class MWSearchResultQuery(val searchinfo: MWSearchResultInfo, val search: List<HashMap<String, String>>)
@Serializable
data class MWSearchResult(val query: MWSearchResultQuery)
object WikiQuery: EventHandler("维基搜索", "wiki", HandlerType.DEFAULT_DISABLED) {
    override fun register() {
        GlobalEventChannel.subscribeGroupMessages {
            startsWithSimple("维基搜索") { keyword, _ ->
                requireNot(denied) {
                    handle(keyword, this)
                }
            }
        }
        super.register()
    }
    val client = HttpClient()
    private suspend fun handle(keyword: String, event: GroupMessageEvent) = event.run {
        if (keyword.trim() == "") return@run
        val searchUrl = "https://otomad.wiki/api.php?action=query&list=search&srwhat=title" +
                "&srnamespace=0&format=json&srsearch=" + URL.encodeComponent(keyword)
        val parseUrl = "https://otomad.wiki/api.php?action=parse&format=json&page=" + URL.encodeComponent(keyword)
        val responseExist = client.get(parseUrl).bodyAsText()
        if ("\"code\":\"missingtitle\"" in responseExist || "\"totalhits\":0" in responseExist) {
            val responseSearch = client.get(searchUrl)
            if (responseSearch.status == HttpStatusCode.OK) {
                val result = OtomadBotCore.json.decodeFromString<MWSearchResult>(responseSearch.bodyAsText())
                if (result.query.searchinfo.totalhits > 10) {
                    quoteReply(
                        "https://otomad.wiki/index.php?title=Special:%E6%90%9C%E7%B4%A2" +
                                "&profile=advanced&fulltext=1&search=" + URL.encodeComponent(keyword)
                    )
                } else if (result.query.searchinfo.totalhits > 0) {
                    quoteReply(
                        "https://otomad.wiki/" +
                                URL.encodeComponent(
                                    result.query.search[0]["title"].toString()
                                        .replace(" ", "_")
                                )
                    )
                }
            }
            pass
        } else {
            quoteReply("https://otomad.wiki/" + URL.encodeComponent(keyword.replace(" ", "_")))
        }
    }
}
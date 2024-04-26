package xyz.xszq.otomadbot.text

import com.soywiz.korio.net.URL
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.mamoe.mirai.event.subscribeGroupMessages
import xyz.xszq.OtomadBotCore
import xyz.xszq.events
import xyz.xszq.otomadbot.CommandModule
import xyz.xszq.otomadbot.GroupCommandWithArg
import xyz.xszq.otomadbot.mirai.quoteReply
import xyz.xszq.otomadbot.mirai.startsWithSimple


@Serializable
data class MWSearchResultInfo(val totalhits: Int)
@Serializable
data class MWSearchResultQuery(val searchinfo: MWSearchResultInfo, val search: List<HashMap<String, String>>)
@Serializable
data class MWSearchResult(val query: MWSearchResultQuery)
object WikiQuery: CommandModule("维基搜索", "wiki") {
    override suspend fun subscribe() {
        events.subscribeGroupMessages {
            startsWithSimple("维基搜索") { keyword, _ ->
                otomadWiki.checkAndRun(this, keyword)
            }
        }
    }
    val client = HttpClient()
    // TODO: Refactor with ktor 2.0
    val otomadWiki = GroupCommandWithArg("音MAD维基搜索", "otomad_wiki", false) { keyword ->
        if (keyword!!.trim() == "") return@GroupCommandWithArg
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
                    quoteReply("https://otomad.wiki/" + URL.encodeComponent(
                        result.query.search[0]["title"].toString().replace(" ", "_")
                    ))
                }
            }
        } else {
            quoteReply("https://otomad.wiki/" + URL.encodeComponent(keyword.replace(" ", "_")))
        }
    }
}